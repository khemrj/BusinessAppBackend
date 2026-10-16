package com.example.firstapp.service.Advanced;


import com.example.firstapp.dto.Chat.ChatDtos.*;
import com.example.firstapp.entity.Messaging.ChatMessage;
import com.example.firstapp.entity.Messaging.Conversation;
import com.example.firstapp.entity.Messaging.ConversationParticipant;
import com.example.firstapp.repository.Messaging.ConversationRepository;
import com.example.firstapp.repository.Messaging.MessageRepository;
import com.example.firstapp.repository.Messaging.ParticipantRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversations;
    private final ParticipantRepository participants;
    private final MessageRepository messages;
    private final MessagePublisher publisher;
    private final TransactionTemplate tx;
    private final UserService userService;  // ← Add this to fetch user details

    

    // ========== CONVERSATIONS ==========

    /**
     * Get or create a direct (1:1) conversation.
     * Returns the conversation wrapped in StartDirectResponse.
     */
    public StartDirectResponse getOrCreateDirect(Long me, Long other) {
        if (me.equals(other)) {
            throw new IllegalArgumentException("Cannot start a chat with yourself");
        }
        
        String key = directKey(me, other);
        Conversation c = conversations.findByDirectKey(key).orElseGet(() -> {
            try {
                return tx.execute(status -> {
                    Conversation created = conversations.save(
                            new Conversation(Conversation.Type.DIRECT, key));
                    participants.save(new ConversationParticipant(created.getId(), me));
                    participants.save(new ConversationParticipant(created.getId(), other));
                    return created;
                });
            } catch (DataIntegrityViolationException race) {
                return conversations.findByDirectKey(key)
                        .orElseThrow(() -> race);
            }
        });
        
        ConversationDto dto = toConversationDto(c, me);
        return new StartDirectResponse(dto);
    }

    /**
     * List all conversations for a user, sorted by recency.
     */
    @Transactional(readOnly = true)
    public ConversationListResponse listConversations(Long userId) {
        List<Long> convIds = participants.findByUserId(userId).stream()
                .map(ConversationParticipant::getConversationId)
                .toList();
        
        List<ConversationDto> dtos = conversations.findAllById(convIds).stream()
                .map(c -> toConversationDto(c, userId))
                .sorted(Comparator.comparing(
                        (ConversationDto d) -> d.lastActivityAt() == null ? "" : d.lastActivityAt())
                        .reversed())
                .toList();
        
        return new ConversationListResponse(dtos, dtos.size());
    }

    /**
     * Get full conversation details including participants.
     */
    @Transactional(readOnly = true)
    public ConversationDetailDto getConversationDetail(Long userId, Long conversationId) {
        assertMember(userId, conversationId);
        
        Conversation c = conversations.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        
        List<ParticipantDto> participantDtos = participants
                .findByConversationId(conversationId).stream()
                .map(this::toParticipantDto)
                .toList();
        
        MessageDto lastMsg = messages.findFirstByConversationIdOrderByIdDesc(conversationId)
                .map(this::toMessageDto)
                .orElse(null);
        
        return new ConversationDetailDto(
                c.getId(),
                c.getType().name(),
                participantDtos,
                lastMsg,
                c.getCreatedAt().toString());
    }

    // ========== MESSAGES ==========

    /**
     * Send a message (idempotent via clientMsgId).
     * Persist first, then push to all participants.
     */
    public MessageDto send(Long senderId, SendMessageRequest req) {
        ChatMessage saved = messages.findBySenderIdAndClientMsgId(senderId, req.clientMsgId())
                .orElse(null);
        
        if (saved == null) {
            try {
                saved = tx.execute(status -> persist(senderId, req));
            } catch (DataIntegrityViolationException dup) {
                saved = messages.findBySenderIdAndClientMsgId(senderId, req.clientMsgId())
                        .orElseThrow(() -> dup);
            }
        }
        
        MessageDto dto = toMessageDto(saved);
        List<Long> recipientIds = participantIds(saved.getConversationId());
        publisher.publish(dto, recipientIds);
        
        return dto;
    }

    private ChatMessage persist(Long senderId, SendMessageRequest req) {
        ConversationParticipant me = participants
                .findByConversationIdAndUserId(req.conversationId(), senderId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Not a participant of this conversation"));
        
        ChatMessage m = messages.save(new ChatMessage(
                req.conversationId(),
                senderId,
                req.clientMsgId(),
                req.content().strip()));
        
        me.markRead(m.getId());  // sender has read their own message
        return m;
    }

    /**
     * Get message history (newest first).
     * Wrapped in HistoryResponse for pagination.
     */
    @Transactional(readOnly = true)
    public HistoryResponse history(Long userId, Long conversationId, Long beforeId, int limit) {
        assertMember(userId, conversationId);
        
        Pageable page = PageRequest.of(0, clamp(limit, 100));
        List<ChatMessage> rows = beforeId == null
                ? messages.findByConversationIdOrderByIdDesc(conversationId, page)
                : messages.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId, beforeId, page);
        
        List<MessageDto> dtos = rows.stream()
                .map(this::toMessageDto)
                .toList();
        
        Long oldestId = dtos.isEmpty() ? null : dtos.get(dtos.size() - 1).id();
        boolean hasMore = dtos.size() == limit;
        
        return new HistoryResponse(dtos, hasMore, oldestId);
    }

    /**
     * Sync messages after reconnect (oldest first).
     * Pass the last message ID you saw as `afterId`.
     */
    @Transactional(readOnly = true)
    public SyncResponse sync(Long userId, Long conversationId, Long afterId, int limit) {
        assertMember(userId, conversationId);
        
        List<ChatMessage> rows = messages.findByConversationIdAndIdGreaterThanOrderByIdAsc(
                conversationId, afterId, PageRequest.of(0, clamp(limit, 200)));
        
        List<MessageDto> dtos = rows.stream()
                .map(this::toMessageDto)
                .toList();
        
        boolean hasMore = dtos.size() == limit;
        
        return new SyncResponse(dtos, hasMore);
    }

    /**
     * Mark a single message as read.
     */
    public MarkReadAckDto markRead(Long userId, Long conversationId, Long messageId) {
        ConversationParticipant p = participants
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Not a participant of this conversation"));
        
        messages.findById(messageId)
                .filter(m -> m.getConversationId().equals(conversationId))
                .orElseThrow(() -> new IllegalArgumentException("Unknown message"));
        
        p.markRead(messageId);
        
        return new MarkReadAckDto(conversationId, messageId, Instant.now().toString());
    }

    /**
     * Mark all messages up to messageId as read (bulk operation).
     */
    public MarkReadAckDto markReadBulk(Long userId, Long conversationId, Long messageId) {
        ConversationParticipant p = participants
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Not a participant of this conversation"));
        
        // Verify message belongs to this conversation
        messages.findById(messageId)
                .filter(m -> m.getConversationId().equals(conversationId))
                .orElseThrow(() -> new IllegalArgumentException("Unknown message"));
        
        // Mark all messages <= messageId as read
        p.markRead(messageId);
        
        return new MarkReadAckDto(conversationId, messageId, Instant.now().toString());
    }

    /**
     * Get unread counts for all conversations of a user.
     */
    @Transactional(readOnly = true)
    public UnreadCountsResponse getUnreadCounts(Long userId) {
        List<UnreadCountDto> counts = participants.findByUserId(userId).stream()
                .map(p -> {
                    long unread = messages.countByConversationIdAndIdGreaterThanAndSenderIdNot(
                            p.getConversationId(),
                            p.getLastReadMessageId() == null ? 0L : p.getLastReadMessageId(),
                            userId);
                    return new UnreadCountDto(p.getConversationId(), unread);
                })
                .toList();
        
        return new UnreadCountsResponse(counts);
    }

    // ========== HELPERS ==========

    private ConversationDto toConversationDto(Conversation c, Long me) {
        List<Long> participantIds = participantIds(c.getId());
        MessageDto lastMsg = messages.findFirstByConversationIdOrderByIdDesc(c.getId())
                .map(this::toMessageDto)
                .orElse(null);
        
        long lastRead = participants.findByConversationIdAndUserId(c.getId(), me)
                .map(ConversationParticipant::getLastReadMessageId)
                .orElse(0L);
        
        long unreadCount = messages.countByConversationIdAndIdGreaterThanAndSenderIdNot(
                c.getId(), lastRead, me);
        
        String lastActivityAt = lastMsg == null ? null : lastMsg.createdAt();
        
        return new ConversationDto(
                c.getId(),
                c.getType().name(),
                participantIds,
                lastMsg,
                unreadCount,
                lastActivityAt);
    }

    private MessageDto toMessageDto(ChatMessage m) {
        String senderName = userService.getUserName(m.getSenderId());
        String avatarUrl = userService.getAvatarUrl(m.getSenderId());
        
        return new MessageDto(
                m.getId(),
                m.getConversationId(),
                m.getSenderId(),
                m.getClientMsgId(),
                m.getContent(),
                m.getCreatedAt().toString(),
                senderName,
                avatarUrl);
    }

    private ParticipantDto toParticipantDto(ConversationParticipant p) {
        String status = userService.getUserStatus(p.getUserId());  // "online", "offline", "away"
        
        return new ParticipantDto(
                p.getUserId(),
                userService.getUserName(p.getUserId()),
                userService.getAvatarUrl(p.getUserId()),
                status,
                p.getLastReadMessageId(),
                p.getJoinedAt().toString());
    }

    private List<Long> participantIds(Long conversationId) {
        return participants.findByConversationId(conversationId).stream()
                .map(ConversationParticipant::getUserId)
                .toList();
    }

    private void assertMember(Long userId, Long conversationId) {
        if (!participants.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new AccessDeniedException(
                    "Not a participant of this conversation");
        }
    }

    private static String directKey(Long a, Long b) {
        return Math.min(a, b) + ":" + Math.max(a, b);
    }

    private static int clamp(int value, int max) {
        return Math.min(Math.max(value, 1), max);
    }
}
