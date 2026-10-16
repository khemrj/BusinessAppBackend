package com.example.firstapp.service.RabbitChat;

import com.example.firstapp.dto.Chat.ChatDtos.ErrorDto;
import com.example.firstapp.dto.Chat.ChatDtos.SendMessageRequest;
import jakarta.validation.Valid;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * Client SEND /app/chat.send  {conversationId, clientMsgId, content}
 * Validates, then publishes to RabbitMQ. No DB work here: the consumer persists, then delivers.
 */
@Controller
public class ChatStompController {

    private final RabbitTemplate rabbit;
    private final SimpMessagingTemplate ws;
    private final ChatAccessService access;

    public ChatStompController(RabbitTemplate rabbit, SimpMessagingTemplate ws, ChatAccessService access) {
        this.rabbit = rabbit;
        this.ws = ws;
        this.access = access;
    }

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageRequest req, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());

        access.assertParticipant(senderId, req.conversationId());
        access.enforceRateLimit(senderId);

        ChatEvent event = new ChatEvent(UUID.randomUUID().toString(), req.conversationId(), senderId,
                req.clientMsgId(), req.content(), System.currentTimeMillis());

        CorrelationData cd = new CorrelationData(event.eventId());
        rabbit.convertAndSend(ChatRabbitConfig.EXCHANGE, ChatRabbitConfig.routingKey(req.conversationId()),
                event, m -> {
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    m.getMessageProperties().setMessageId(event.eventId());
                    return m;
                }, cd);

        // Broker confirm = durably accepted. If not, the client flips "sending..." to "failed" and may retry
        // with the SAME clientMsgId (safe: the DB unique key makes it idempotent).
        cd.getFuture().whenComplete((confirm, ex) -> {
            if (ex != null || confirm == null || !confirm.isAck()) {
                ws.convertAndSendToUser(principal.getName(), "/queue/errors",
                        new ErrorDto("SEND_FAILED", "Message could not be queued",
                                req.clientMsgId(), Instant.now().toString()));
            }
        });
    }

    @MessageExceptionHandler(AccessDeniedException.class)
    @SendToUser("/queue/errors")
    public ErrorDto onForbidden(AccessDeniedException e) { return new ErrorDto("FORBIDDEN", "Not allowed"); }

    @MessageExceptionHandler(ChatAccessService.RateLimitedException.class)
    @SendToUser("/queue/errors")
    public ErrorDto onRateLimited(ChatAccessService.RateLimitedException e) {
        return new ErrorDto("RATE_LIMITED", "Too many messages, slow down");
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ErrorDto onInvalid(MethodArgumentNotValidException e) {
        return new ErrorDto("VALIDATION_FAILED", "Invalid message");
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorDto onUnexpected(Exception e) { return new ErrorDto("INTERNAL", "Something went wrong"); }
}
