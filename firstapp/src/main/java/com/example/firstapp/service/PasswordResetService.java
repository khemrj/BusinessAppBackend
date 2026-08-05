package com.example.firstapp.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.firstapp.entity.PasswordResetToken;
import com.example.firstapp.entity.User;
import com.example.firstapp.exception.InvalidTokenException;
import com.example.firstapp.repository.PasswordResetTokenRepository;
import com.example.firstapp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;   // BCrypt, reused
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Keep this in sync with the expiry text shown in the reset email.
    private static final Duration TOKEN_TTL = Duration.ofMinutes(20);

    /** Step 1: user requests a reset. ALWAYS returns quietly (no enumeration). */
    @Transactional
    public void requestReset(String email) {
        users.findByEmail(email).ifPresent(user -> {
            // Invalidate any prior pending tokens for this user
            tokens.deleteByUserId(user.getId());

            // 32 random bytes → URL-safe string. This is what we email.
            byte[] raw = new byte[32];
            SECURE_RANDOM.nextBytes(raw);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

            // Store ONLY the hash — never the raw token
            PasswordResetToken prt = PasswordResetToken.builder()
                    .tokenHash(passwordEncoder.encode(rawToken))
                    .userId(user.getId())
                    .expiresAt(Instant.now().plus(TOKEN_TTL))
                    .used(false)
                    .build();
            tokens.save(prt);

            // Email the RAW token (never stored). The email service builds the
            // reset URL from app.frontend.url, so no link is constructed here.
            emailService.sendPasswordResetEmail(
                    user.getEmail(), user.getUsername(), rawToken);
        });
        // Whether or not the user existed, the caller gets the same response.
    }

    /** Step 2: user submits new password + token from the email. */
    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        // We stored a hash, so we can't look up by raw token directly.
        // Fetch unused, unexpired candidates and verify the hash.
        List<PasswordResetToken> candidates =
                tokens.findByUsedFalseAndExpiresAtAfter(Instant.now());

        PasswordResetToken match = candidates.stream()
                .filter(t -> passwordEncoder.matches(rawToken, t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset link"));

        User user = users.findById(match.getUserId())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset link"));

        // Your User field is `password`, so setPassword (managed entity → flushes on commit)
        user.setPassword(passwordEncoder.encode(newPassword));

        // Burn every reset token for this user (includes the one just used)
        tokens.deleteByUserId(user.getId());

        // Recommended next step: invalidate existing JWTs so a reset actually
        // locks out anyone who had access (bump a per-user token version).
    }
}