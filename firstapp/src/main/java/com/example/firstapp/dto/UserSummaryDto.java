package com.example.firstapp.dto;

import com.example.firstapp.entity.User;
import com.example.firstapp.enums.Role;
import java.time.LocalDateTime;

// A record: immutable, concise. Password is deliberately absent — it can't leak.
public record UserSummaryDto(
        Long id,
        String username,
        String email,
        Role role,
        boolean active,
        boolean locked,
        int failedAttempts,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static UserSummaryDto from(User u) {
        return new UserSummaryDto(
                u.getId(),
                u.getUsername(),     // your entity returns email here — see note below
                u.getEmail(),
                u.getRole(),
                u.isActive(),
                u.isLocked(),
                u.getFailedAttempts(),
                u.getLastLoginAt(),
                u.getCreatedAt()
        );
    }
}
