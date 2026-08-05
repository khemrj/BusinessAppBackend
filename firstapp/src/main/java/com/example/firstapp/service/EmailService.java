package com.example.firstapp.service;

import com.example.firstapp.dto.EmailRequest;

/**
 * Email Service Interface.
 *
 * Single Responsibility:
 * Defines the CONTRACT for sending emails.
 * Zero implementation logic.
 *
 * WHY an interface?
 * → Callers (AuthService, TradeService) depend on
 *   this abstraction — not on concrete SMTP code
 * → Swap Brevo with SendGrid/AWS SES by changing
 *   ONE implementation class — callers unchanged
 * → Unit tests mock this interface easily
 * → Multiple implementations possible:
 *   EmailServiceImpl (real Brevo SMTP)
 *   MockEmailService (tests — no real sends)
 *
 * MVC / Layered Architecture:
 * AuthService (business layer) → EmailService (interface)
 * ↓
 * EmailServiceImpl (infrastructure layer) → Brevo SMTP
 */
public interface EmailService {

    /**
     * Core send method.
     * All specific methods below delegate to this.
     * Handles template rendering, SMTP, rate limiting.
     *
     * @param request Fully built EmailRequest
     */
    void sendEmail(EmailRequest request);

    /**
     * Sends welcome email after successful signup.
     *
     * @param toEmail  Recipient email
     * @param username Display name for personalization
     */
    void sendWelcomeEmail(String toEmail, String username);

    /**
     * Sends account locked notification.
     * Triggered after MAX_FAILED_ATTEMPTS login failures.
     *
     * @param toEmail  Recipient email
     * @param username Display name
     */
    void sendAccountLockedEmail(
        String toEmail, String username
    );

    /**
     * Sends password reset link.
     *
     * @param toEmail    Recipient email
     * @param username   Display name
     * @param resetToken Secure URL-safe token
     */
    void sendPasswordResetEmail(
        String toEmail,
        String username,
        String resetToken
    );
}
