package com.example.firstapp.service.Advanced;

import com.example.firstapp.dto.Chat.ChatDtos.MessageDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class MessagePublisher {

    private final SimpMessagingTemplate template;
    private final SimpUserRegistry userRegistry;
    private final PushNotifier pushNotifier;

    public MessagePublisher(SimpMessagingTemplate template, SimpUserRegistry userRegistry,
                            PushNotifier pushNotifier) {
        this.template = template;
        this.userRegistry = userRegistry;
        this.pushNotifier = pushNotifier;
    }

    public void publish(MessageDto dto, Collection<Long> recipientIds) {
        for (Long uid : recipientIds) {
            String name = uid.toString();
            template.convertAndSendToUser(name, "/queue/messages", dto);
            if (!uid.equals(dto.senderId()) && userRegistry.getUser(name) == null) {
                pushNotifier.notifyOffline(uid, dto);
            }
        }
    }
}