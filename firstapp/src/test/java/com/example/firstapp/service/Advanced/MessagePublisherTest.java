package com.example.firstapp.service.Advanced;

import com.example.firstapp.dto.Chat.ChatDtos.MessageDto;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagePublisherTest {

    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    private final SimpUserRegistry userRegistry = mock(SimpUserRegistry.class);
    private final PushNotifier pushNotifier = mock(PushNotifier.class);
    private final MessagePublisher publisher =
            new MessagePublisher(template, userRegistry, pushNotifier);
    private final MessageDto message = new MessageDto(
            10L, 20L, 99L, "client-1", "Hello", "2026-10-01T12:00:00Z", "Sender", null);

    @Test
    void publishContinuesDeliveringWhenWebSocketOrPushDeliveryFails() {
        when(userRegistry.getUser("1")).thenReturn(null);
        when(userRegistry.getUser("2")).thenReturn(null);
        when(userRegistry.getUser("3")).thenReturn(null);
        doThrow(new IllegalStateException("websocket unavailable"))
                .when(template).convertAndSendToUser("1", "/queue/messages", message);
        doThrow(new IllegalStateException("push unavailable"))
                .when(pushNotifier).notifyOffline(2L, message);

        assertDoesNotThrow(() -> publisher.publish(message, List.of(1L, 2L, 3L)));

        verify(template).convertAndSendToUser("3", "/queue/messages", message);
        verify(pushNotifier).notifyOffline(1L, message);
        verify(pushNotifier).notifyOffline(3L, message);
    }

    @Test
    void publishContinuesWhenOnlineStatusCannotBeRead() {
        when(userRegistry.getUser("1")).thenThrow(new IllegalStateException("registry unavailable"));
        when(userRegistry.getUser("2")).thenReturn(null);

        assertDoesNotThrow(() -> publisher.publish(message, List.of(1L, 2L)));

        verify(template).convertAndSendToUser("2", "/queue/messages", message);
        verify(pushNotifier).notifyOffline(2L, message);
    }

    @Test
    void publishRejectsNullRecipientCollectionWithClearMessage() {
        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> publisher.publish(message, null));

        assertEquals("Recipient IDs must not be null.", exception.getMessage());
    }
}