package com.example.firstapp.service.RabbitChat;



/**
 * Sent by the server back to the sender after a chat message is persisted.
 * The client matches it to the optimistic bubble using clientMsgId.
 */
public record SendAckDto(
        String clientMsgId,
        Long messageId,
        Long conversationId,
        String createdAt
) {
}


