package com.example.firstapp.service.RabbitChat;

import com.example.firstapp.repository.Messaging.ParticipantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatAccessService {

    public static class RateLimitedException extends RuntimeException {
        public RateLimitedException() { super("rate limited"); }
    }

    private static final int MAX_MESSAGES = 20;
    private static final long WINDOW_MS = 10_000;

    private final ParticipantRepository participants;
    // userId -> [windowStartMillis, count]. PER INSTANCE: with N instances the effective limit is N x higher.
    // Move to Redis (bucket4j) when you run more than one backend instance.
    private final ConcurrentHashMap<Long, long[]> windows = new ConcurrentHashMap<>();

    public ChatAccessService(ParticipantRepository participants) {
        this.participants = participants;
    }

    public void assertParticipant(Long userId, Long conversationId) {
        if (!participants.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new AccessDeniedException("not a participant");
        }
    }

    public void enforceRateLimit(Long userId) {
        long now = System.currentTimeMillis();
        long[] w = windows.compute(userId, (k, v) -> {
            if (v == null || now - v[0] >= WINDOW_MS) return new long[]{now, 1};
            v[1]++;
            return v;
        });
        if (w[1] > MAX_MESSAGES) throw new RateLimitedException();
        if (windows.size() > 100_000) windows.entrySet().removeIf(e -> now - e.getValue()[0] >= WINDOW_MS);
    }
}
