package com.example.firstapp.repository.Messaging;



import org.springframework.data.jpa.repository.JpaRepository;

import com.example.firstapp.entity.Messaging.ConversationParticipant;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    /**
     * Get all participants in a conversation.
     */
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
