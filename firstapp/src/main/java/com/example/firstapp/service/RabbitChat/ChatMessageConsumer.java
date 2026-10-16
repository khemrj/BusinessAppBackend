package com.example.firstapp.service.RabbitChat;

import com.example.firstapp.dto.Chat.ChatDtos.MessageDto;

import com.example.firstapp.entity.Messaging.ChatMessage;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageConsumer {

    private final ChatPersistenceService persistence;
    private final SimpMessagingTemplate ws;
    private final PushService push;

    public ChatMessageConsumer(ChatPersistenceService persistence, SimpMessagingTemplate ws, PushService push) {
        this.persistence = persistence;
        this.ws = ws;
        this.push = push;
    }

    /**
     * ONE consumer per shard queue (direct container, concurrency=1 => consumers-per-queue=1):
     * messages of a conversation are persisted strictly in order. Parallelism = number of shards.
     * Flow: commit to MySQL first, THEN deliver. A crash in between just redelivers (idempotent).
     */
    @RabbitListener(queues = "#{T(com.example.firstapp.service.RabbitChat.ChatRabbitConfig).persistQueueNames()}",
                    concurrency = "1")
    public void persistAndDeliver(ChatEvent e) {
        ChatMessage saved;
        try {
            saved = persistence.saveIfAbsent(e);               // @Transactional, commits on return
        } catch (IllegalArgumentException bad) {
            throw new AmqpRejectAndDontRequeueException("invalid event " + e.eventId(), bad); // straight to DLQ
        }
        // Any other exception (DB down, deadlock, duplicate-key race) -> requeue -> retried up to delivery-limit -> DLQ.

        MessageDto dto = persistence.toDto(saved);
        for (Long userId : persistence.participantIds(e.conversationId())) {
            if (!userId.equals(e.senderId())) {
                ws.convertAndSendToUser(userId.toString(), "/queue/messages", dto);
            }
        }
        ws.convertAndSendToUser(e.senderId().toString(), "/queue/acks",
                new SendAckDto(e.clientMsgId(), saved.getId(), saved.getConversationId(),
                        saved.getCreatedAt().toString()));
    }

    @RabbitListener(queues = ChatRabbitConfig.PUSH_Q, concurrency = "4")
    public void notifyOffline(ChatEvent e) {
        push.notifyOffline(e);
    }
}
