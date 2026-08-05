package com.example.firstapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = {
        // token_hash is looked up / must be unique
        @Index(name = "idx_prt_token_hash", columnList = "token_hash", unique = true),
        // we query and delete by user_id — index it
        @Index(name = "idx_prt_user_id", columnList = "user_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor 
public class PasswordResetToken {
    //getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The HASH of the token — never the raw token
    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    // FK to users.id — Long, matching your User entity
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}