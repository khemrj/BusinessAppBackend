package com.example.firstapp.dto;

import java.time.LocalDateTime;

import com.example.firstapp.enums.PostVisibility;

public record PostResponse(
        Long id,
        Long authorId, String authorName, String authorAvatarUrl,
        String content, String mediaUrl,
        int likeCount, int commentCount,
        boolean likedByCurrentUser,
        PostVisibility visibility,
        LocalDateTime createdAt
) {}
