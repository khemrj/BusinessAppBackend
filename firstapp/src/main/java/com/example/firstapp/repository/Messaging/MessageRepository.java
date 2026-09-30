package com.example.firstapp.repository.Messaging;



import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.firstapp.entity.Messaging.ChatMessage;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<ChatMessage, Long> {
 
    // ========== IDEMPOTENCY ==========
 
    /**
     * Find a message by sender and client-generated ID.
     * Used for idempotent sends: if (senderId, clientMsgId) exists, don't create duplicate.
     */
    Optional<ChatMessage> findBySenderIdAndClientMsgId(Long senderId, String clientMsgId);
 
    // ========== LATEST MESSAGE ==========
 
    /**
     * Get the most recent message in a conversation.
     * Used to populate "last message" in conversation list.
     */
    Optional<ChatMessage> findFirstByConversationIdOrderByIdDesc(Long conversationId);
 
    // ========== HISTORY (newest first) ==========
 
    /**
     * Get messages in a conversation, sorted newest first (no limit).
     * Use with Pageable for pagination.
     */
    List<ChatMessage> findByConversationIdOrderByIdDesc(Long conversationId, Pageable page);
 
    /**
     * Get messages before a specific ID, newest first.
     * Used to paginate history backwards.
     * E.g., "Give me messages with ID < 500, newest first"
     */
    List<ChatMessage> findByConversationIdAndIdLessThanOrderByIdDesc(
            Long conversationId, Long beforeId, Pageable page);
 
    // ========== SYNC (oldest first) ==========
 
    /**
     * Get messages after a specific ID, oldest first.
     * Used to backfill missed messages on reconnect.
     * E.g., "Give me messages with ID > 200, oldest first"
     */
    List<ChatMessage> findByConversationIdAndIdGreaterThanOrderByIdAsc(
            Long conversationId, Long afterId, Pageable page);
 
    // ========== UNREAD COUNT ==========
 
    /**
     * Count unread messages in a conversation for a specific user.
     * Returns count of messages where:
     * - message.id > user's last_read_message_id (unread)
     * - message.senderId != userId (not sent by self)
     */
    long countByConversationIdAndIdGreaterThanAndSenderIdNot(
            Long conversationId, Long lastReadId, Long userId);
 
    // ========== BULK OPERATIONS ==========
 
    /**
     * Delete all messages in a conversation (e.g., when deleting a conversation).
     */
    void deleteByConversationId(Long conversationId);
 
    /**
     * Count total messages in a conversation.
     * Useful for analytics or pagination info.
     */
    long countByConversationId(Long conversationId);
 
    // ========== SEARCH (optional, for future use) ==========
 
    /**
     * Find messages by content search.
     * Use if you add full-text search later.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.content LIKE %:keyword%")
    List<ChatMessage> searchByContent(
            @Param("conversationId") Long conversationId,
            @Param("keyword") String keyword,
            Pageable page);
}