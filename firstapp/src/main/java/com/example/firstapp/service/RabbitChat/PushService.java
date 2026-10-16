package com.example.firstapp.service.RabbitChat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final SimpUserRegistry userRegistry; // cluster-wide because of setUserRegistryBroadcast(...)
    private final ChatPersistenceService persistence;

    public PushService(SimpUserRegistry userRegistry, ChatPersistenceService persistence) {
        this.userRegistry = userRegistry;
        this.persistence = persistence;
    }

    public void notifyOffline(ChatEvent e) {
        for (Long userId : persistence.participantIds(e.conversationId())) {
            if (userId.equals(e.senderId())) continue;
            if (userRegistry.getUser(userId.toString()) != null) continue; // has a live socket
            // TODO FCM: load the user's device tokens and send a data/notification push.
            // Privacy tip: send conversationId only, let the app fetch content via REST /sync.
            log.debug("push needed for user {} conversation {}", userId, e.conversationId());
        }
    }
}
