package com.example.firstapp.repository.Messaging;



import org.springframework.data.jpa.repository.JpaRepository;

import com.example.firstapp.entity.Messaging.Conversation;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByDirectKey(String directKey);
}
