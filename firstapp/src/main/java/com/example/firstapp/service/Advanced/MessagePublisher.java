package com.example.firstapp.service.Advanced;

import com.example.firstapp.dto.Chat.ChatDtos.MessageDto;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

@Component
@RequiredArgsConstructor 
public class MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);

    private final SimpMessagingTemplate template;
    private final SimpUserRegistry userRegistry;
    private final PushNotifier pushNotifier;

   

    public void publish(MessageDto dto, Collection<Long> recipientIds) {
        Objects.requireNonNull(dto, "Message must not be null.");
        Objects.requireNonNull(recipientIds, "Recipient IDs must not be null.");

        for (Long recipientId : recipientIds) {
            Objects.requireNonNull(recipientId, "Recipient IDs must not contain null values.");
            String username = recipientId.toString();

            try {
                template.convertAndSendToUser(username, "/queue/messages", dto);
            } catch (RuntimeException ex) {
                log.error("WebSocket delivery failed for message {} to recipient {}.",
                        dto.id(), recipientId, ex);
            }

            if (recipientId.equals(dto.senderId())) {
                continue;
            }

            final boolean recipientOffline;
            try {
                recipientOffline = userRegistry.getUser(username) == null;
            } catch (RuntimeException ex) {
                log.error("Could not determine online status for recipient {} while publishing message {}.",
                        recipientId, dto.id(), ex);
                continue;
            }

            if (recipientOffline) {
                try {
                    pushNotifier.notifyOffline(recipientId, dto);
                } catch (RuntimeException ex) {
                    log.error("Offline push notification failed for message {} to recipient {}.",
                            dto.id(), recipientId, ex);
                }
            }
        }     
    }
}