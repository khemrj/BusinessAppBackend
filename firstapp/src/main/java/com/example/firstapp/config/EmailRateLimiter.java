package com.example.firstapp.config;


import io.github.bucket4j.Bucket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Email Rate Limiter.
 *
 * Single Responsibility:
 * ONLY decides whether an email is allowed to be sent
 * to a specific address right now.
 *
 * WHY this exists:
 * Without it, an attacker can hit /forgot-password
 * 10,000 times with the same email address.
 * → 10,000 emails sent to one person = email bomb
 * → Brevo flags your account for spam abuse
 * → Your account gets suspended
 * → All your real users stop getting emails
 *
 * With it:
 * Same email gets max 3 general emails per hour
 * Same email gets max 5 OTPs per hour
 * Attack is neutralized silently
 *
 * SECURITY NOTE:
 * Never tell caller WHY email was not sent.
 * Attacker should not know if email exists in system.
 * Always return generic "if account exists, email sent"
 */
@Component
@Slf4j
public class EmailRateLimiter {

    // Separate maps for different email types
    // Key format: "general:email@domain.com"
    //             "otp:email@domain.com"
    // Namespacing prevents collision between types

    // General emails: welcome, notifications, confirmations
    private final Map<String, Bucket> generalBuckets =
            new ConcurrentHashMap<>();

    // Password reset: stricter — account takeover risk
    private final Map<String, Bucket> passwordResetBuckets =
            new ConcurrentHashMap<>();

    /**
     * Checks if a general email can be sent to this address.
     * Limit: 5 emails per hour per address.
     * Covers: welcome, notifications, confirmations.
     *
     * @param email Recipient email address
     * @return true if allowed, false if rate limited
     */
    public boolean canSendGeneralEmail(String email) {
        Bucket bucket = generalBuckets.computeIfAbsent(
            "general:" + email,
            k -> buildGeneralBucket()
        );
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn(
                "General email rate limit exceeded: {}",
                maskEmail(email)
            );
        }

        return allowed;
    }

    /**
     * Checks if a password reset email can be sent.
     * Limit: 3 per hour per address.
     * Stricter because password reset is a security operation.
     * Attacker trying to exhaust reset tokens is slowed down.
     *
     * @param email Recipient email address
     * @return true if allowed, false if rate limited
     */
    public boolean canSendPasswordReset(String email) {
        Bucket bucket = passwordResetBuckets.computeIfAbsent(
            "reset:" + email,
            k -> buildPasswordResetBucket()
        );
        boolean allowed = bucket.tryConsume(1);

        if (!allowed) {
            log.warn(
                "Password reset rate limit exceeded: {}",
                maskEmail(email)
            );
        }

        return allowed;
    }

    // ── Private Bucket Factories ───────────────────────────────

    /**
     * General email bucket: 5 per hour.
     * Greedy refill: 1 token every 12 minutes.
     * Smoother than interval refill.
     */
    private Bucket buildGeneralBucket() {
        return Bucket.builder()
            .addLimit(limit -> limit
                .capacity(5)
                .refillGreedy(5, Duration.ofHours(1))
            )
            .build();
    }

    /**
     * Password reset bucket: 3 per hour.
     * Stricter — security operation.
     */
    private Bucket buildPasswordResetBucket() {
        return Bucket.builder()
            .addLimit(limit -> limit
                .capacity(3)
                .refillGreedy(3, Duration.ofHours(1))
            )
            .build();
    }

    /**
     * Masks email for logs.
     * Never log full email addresses — privacy.
     * "kj@gmail.com" → "k***@gmail.com"
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 1) return "*@" + parts[1];
        return name.charAt(0) +
               "*".repeat(name.length() - 1) +
               "@" + parts[1];
    }
}
