package com.example.firstapp.repository.Messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.firstapp.entity.Messaging.ConversationParticipant;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    /**
     * Get all participants in a conversation.
     */

    // added advancereadPointer
    @Modifying(clearAutomatically = true)
    @Query("update ConversationParticipant p set p.lastReadMessageId = :msgId " +
            "where p.conversationId = :convId and p.userId = :userId " +
            "and (p.lastReadMessageId is null or p.lastReadMessageId < :msgId)")
    int advanceReadPointer(@Param("convId") Long convId, @Param("userId") Long userId, @Param("msgId") Long msgId);

    List<ConversationParticipant> findByConversationId(Long conversationId);

    /**
     * Get all conversations a user is part of.
     */
    List<ConversationParticipant> findByUserId(Long userId);

    /**
     * Get a specific participant record.
     */
    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * Check if a user is part of a conversation.
     */
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    /**
     * Delete all participants from a conversation (e.g., when deleting a group).
     */
    void deleteByConversationId(Long conversationId);
}
