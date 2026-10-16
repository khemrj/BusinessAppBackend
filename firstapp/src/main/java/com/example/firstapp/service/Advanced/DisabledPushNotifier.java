package com.example.firstapp.service.Advanced;

import com.example.firstapp.dto.Chat.ChatDtos.MessageDto;
import org.springframework.stereotype.Component;

@Component
public class DisabledPushNotifier implements PushNotifier {

    @Override
    public void notifyOffline(Long userId, MessageDto message) {
    }
}
