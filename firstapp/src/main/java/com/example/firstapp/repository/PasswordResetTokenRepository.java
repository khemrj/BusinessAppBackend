package com.example.firstapp.repository;

import com.example.firstapp.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.Instant;
import java.util.List;


public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Fetch unused, still-valid tokens so we can verify a raw token against
     * each stored hash. Used in confirmReset().
     */
    List<PasswordResetToken> findByUsedFalseAndExpiresAtAfter(Instant now);

    /**
     * Invalidate every reset token for a user — called when a new reset is
     * requested (kill old links) and after a successful reset (burn the rest).
     */
    void deleteByUserId(Long userId);

    /**
     * Cleanup job: purge expired tokens so the table doesn't grow forever.
     */
    void deleteByExpiresAtBefore(Instant cutoff);
}