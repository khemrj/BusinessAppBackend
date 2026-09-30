package com.example.firstapp.service.Advanced;
import com.example.firstapp.dto.Chat.ChatDtos.MessageDto;

public interface PushNotifier {
    void notifyOffline(Long userId, MessageDto message);
}
