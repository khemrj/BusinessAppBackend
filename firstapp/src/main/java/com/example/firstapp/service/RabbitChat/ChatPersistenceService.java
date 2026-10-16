package com.example.firstapp.service.RabbitChat;

import com.example.firstapp.dto.Chat.ChatDtos.*;
import com.example.firstapp.entity.Messaging.ChatMessage;
import com.example.firstapp.service.Advanced.UserServiceImpl;
import com.example.firstapp.service.RabbitChat.ChatEvent;
import com.example.firstapp.repository.Messaging.MessageRepository;
import com.example.firstapp.repository.Messaging.ParticipantRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class ChatPersistenceService {

    private final MessageRepository messages;
    private final ParticipantRepository participants;
    private final UserServiceImpl users;

    public ChatPersistenceService(MessageRepository messages,
                                  ParticipantRepository participants,
                                  UserServiceImpl users) {
        this.messages = messages;
        this.participants = participants;
        this.users = users;
    }

    // ---------- write path (called by the AMQP consumer) ----------

    /** Idempotent: redelivery or client retry with the same clientMsgId returns the existing row. */
    @Transactional
    public ChatMessage saveIfAbsent(ChatEvent e) {
        if (!participants.existsByConversationIdAndUserId(e.conversationId(), e.senderId())) {
            throw new IllegalArgumentException("sender is not a participant");
        }
        return messages.findBySenderIdAndClientMsgId(e.senderId(), e.clientMsgId())
                .orElseGet(() -> messages.save(
                        new ChatMessage(e.conversationId(), e.senderId(), e.clientMsgId(), e.content())));
    }

    @Transactional(readOnly = true)
    public List<Long> participantIds(Long conversationId) {
        return participants.findByConversationId(conversationId).stream()
                .map(p -> p.getUserId()).toList();
    }

    public MessageDto toDto(ChatMessage m) {
        return MessageDto.from(m, users.getUserName(m.getSenderId()), users.getAvatarUrl(m.getSenderId()));
    }

    // ---------- read path (REST: reconnect catch-up, scroll-back, read pointer) ----------

    /** Everything newer than afterId (client passes the highest message id it has). Ascending. */
    @Transactional(readOnly = true)
    public SyncResponse syncAfter(Long userId, Long conversationId, long afterId, int limit) {
        requireParticipant(userId, conversationId);
        List<ChatMessage> rows = messages.findByConversationIdAndIdGreaterThanOrderByIdAsc(
                conversationId, afterId, PageRequest.of(0, limit + 1));
        boolean hasMore = rows.size() > limit;
        List<ChatMessage> page = hasMore ? rows.subList(0, limit) : rows;
        return new SyncResponse(page.stream().map(this::toDto).toList(), hasMore);
    }

    /** Older messages for scroll-back. Returned ascending; oldestId is the next beforeId. */
    @Transactional(readOnly = true)
    public HistoryResponse history(Long userId, Long conversationId, Long beforeId, int limit) {
        requireParticipant(userId, conversationId);
        PageRequest page = PageRequest.of(0, limit + 1);
        List<ChatMessage> rows = beforeId == null
                ? messages.findByConversationIdOrderByIdDesc(conversationId, page)
                : messages.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId, beforeId, page);
        boolean hasMore = rows.size() > limit;
        List<ChatMessage> trimmed = hasMore ? rows.subList(0, limit) : rows;
        List<ChatMessage> asc = new java.util.ArrayList<>(trimmed);
        Collections.reverse(asc);
        Long oldest = asc.isEmpty() ? null : asc.get(0).getId();
        return new HistoryResponse(asc.stream().map(this::toDto).toList(), hasMore, oldest);
    }

    /** Atomic, forward-only (a stale/duplicate request can never move the pointer backwards). */
    @Transactional
    public MarkReadAckDto markRead(Long userId, Long conversationId, Long messageId) {
        requireParticipant(userId, conversationId);
        ChatMessage m = messages.findById(messageId)
                .filter(x -> x.getConversationId().equals(conversationId))
                .orElseThrow(() -> new AccessDeniedException("message not in conversation"));
        participants.advanceReadPointer(conversationId, userId, m.getId());
        return new MarkReadAckDto(conversationId, m.getId(), Instant.now().toString());
    }

    private void requireParticipant(Long userId, Long conversationId) {
        if (!participants.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new AccessDeniedException("not a participant");
        }
    }
}
