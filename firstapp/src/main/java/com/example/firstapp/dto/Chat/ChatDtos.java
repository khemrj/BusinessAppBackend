package com.example.firstapp.dto.Chat;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

import com.example.firstapp.entity.Messaging.ChatMessage;

public final class ChatDtos {
    private ChatDtos() {}

    // ========== REQUEST DTOs (client → server) ==========

    public record SendMessageRequest(
            @NotNull Long conversationId,
            @NotBlank @Size(max = 36) String clientMsgId,
            @NotBlank @Size(max = 4000) String content) {}

    public record StartDirectRequest(
            @NotNull Long userId) {}

    public record MarkReadRequest(
            @NotNull Long messageId) {}

    public record BulkMarkReadRequest(
            @NotNull Long conversationId,
            @NotNull Long messageId) {}

    public record TypingIndicatorRequest(
            @NotNull Long conversationId) {}

    // ========== RESPONSE DTOs (server → client) ==========

    public record MessageDto(
            Long id,
            Long conversationId,
            Long senderId,
            String clientMsgId,
            String content,
            String createdAt,
            String senderName,
            String senderAvatarUrl) {

        public static MessageDto from(ChatMessage m, String senderName, String avatarUrl) {
            return new MessageDto(m.getId(), m.getConversationId(), m.getSenderId(),
                    m.getClientMsgId(), m.getContent(), m.getCreatedAt().toString(),
                    senderName, avatarUrl);
        }

        public static MessageDto from(ChatMessage m) {
            return from(m, "Unknown", null);
        }
    }

    public record HistoryResponse(
            List<MessageDto> messages,
            boolean hasMore,
            Long oldestId) {}

    public record SyncResponse(
            List<MessageDto> messages,
            boolean hasMore) {}

   // ✅ Better
public record ConversationDto(Long id, String type, List<Long> participantIds,
                              MessageDto lastMessage, long unreadCount,
                              String lastActivityAt) {}  // ISO-8601 of the newest message

// In listConversations():
//.sorted(Comparator.comparing(ConversationDto::lastActivityAt).reversed())

    public record ParticipantDto(
            Long userId,
            String name,
            String avatarUrl,
            String status,
            Long lastReadMessageId,
            String joinedAt) {}

    public record ConversationDetailDto(
            Long id,
            String type,
            List<ParticipantDto> participants,
            MessageDto lastMessage,
            String createdAt) {}

    public record StartDirectResponse(
            ConversationDto conversation) {}

    public record MarkReadAckDto(
            Long conversationId,
            Long messageId,
            String timestamp) {}

    public record ErrorDto(
            String code,
            String message,
            String requestId,
            String timestamp) {
        public ErrorDto(String code, String message) {
            this(code, message, null, java.time.Instant.now().toString());
        }
    }

    public record TypingIndicatorDto(
            Long conversationId,
            Long userId,
            String userName) {}

    public record OnlineStatusDto(
            Long userId,
            String status,
            String timestamp) {}

    public record UnreadCountDto(
            Long conversationId,
            long unreadCount) {}

    public record UnreadCountsResponse(
            List<UnreadCountDto> counts) {}

    // ========== BROADCAST DTOs ==========

    public record PublishBroadcastRequest(
            @NotBlank String title,
            @NotBlank @Size(max = 2000) String body,
            @NotNull String audience) {}

    public record BroadcastDto(
            Long id,
            String title,
            String body,
            String audience,
            String createdAt,
            String createdBy) {}

    // ========== AUTH & SETUP ==========

    public record LoginResponse(
            String accessToken,
            long expiresIn,
            String tokenType) {}

    public record ConversationListResponse(
            List<ConversationDto> conversations,
            int total) {}

    // ========== UTILITY ==========

    public record PagedResponse<T>(
            List<T> items,
            long total,
            int page,
            int pageSize,
            boolean hasNext) {}

    public record SearchResultDto(
            Long messageId,
            Long conversationId,
            Long senderId,
            String senderName,
            String content,
            String createdAt,
            String snippet) {}

    public record FieldErrorDto(
            String field,
            String message,
            Object rejectedValue) {}

    public record ValidationErrorResponse(
            String code,
            String message,
            List<FieldErrorDto> fieldErrors) {}
}