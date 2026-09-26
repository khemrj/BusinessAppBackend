package com.example.firstapp.dto;

import java.time.LocalDateTime;

import com.example.firstapp.entity.Connection;
import com.example.firstapp.enums.ConnectionStatus;

public record ConnectionResponse(
        Long id,
        Long requesterId, String requesterEmail,
        Long receiverId, String receiverEmail,
        ConnectionStatus status,
        LocalDateTime createdAt, LocalDateTime respondedAt
) {
    public static ConnectionResponse from(Connection c) {
        return new ConnectionResponse(
                c.getId(),
                c.getRequester().getId(), c.getRequester().getEmail(),
                c.getReceiver().getId(), c.getReceiver().getEmail(),
                c.getStatus(), c.getCreatedAt(), c.getRespondedAt());
    }
}
