package com.example.firstapp.service.RabbitChat;

/** The AMQP message body. Recipients are NOT in here: the consumer resolves them from conversation participants. */
public record ChatEvent(
        String eventId,
        Long conversationId,
        Long senderId,
        String clientMsgId,
        String content,
        long enqueuedAtMillis) {}
