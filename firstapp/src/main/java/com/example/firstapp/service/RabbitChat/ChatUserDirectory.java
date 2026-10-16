package com.example.firstapp.service.RabbitChat;

/**
 * Implement with your existing User repository/service (and cache it: this is called per delivered message).
 * Example: @Service class ChatUserDirectoryImpl implements ChatUserDirectory { ... @Cacheable("chatUsers") ... }
 */
public interface ChatUserDirectory {
    String displayName(Long userId);
    String avatarUrl(Long userId);
}
