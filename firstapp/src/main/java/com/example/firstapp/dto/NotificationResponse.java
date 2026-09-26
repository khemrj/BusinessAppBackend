package com.example.firstapp.dto;

import java.time.LocalDateTime;

import com.example.firstapp.enums.NotificationType;

public record NotificationResponse(
        Long id, NotificationType type, String payload, boolean read, LocalDateTime createdAt
) {}
//authorName, authorAvatarUrl, and likedbycurrentuser are't columns they rea populated in the service layer mapper + a batched "which of these posts did the current user like " query showi in 5 so the feed never does an N+ 