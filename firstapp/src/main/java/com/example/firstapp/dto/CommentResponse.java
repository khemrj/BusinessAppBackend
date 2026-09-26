package com.example.firstapp.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id, Long postId,
        Long userId, String userName,
        Long parentCommentId, String content,
        LocalDateTime createdAt
) {}
