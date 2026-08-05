package com.example.firstapp.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Email Template Enum.
 *
 * Single Responsibility:
 * Maps each email type to its Thymeleaf template file
 * and provides a default subject line.
 *
 * HOW TO ADD NEW EMAIL TYPE:
 * 1. Add constant here with template path and subject
 * 2. Create HTML file at resources/templates/email/name.html
 * 3. Call emailService.sendEmail() with this template
 * Zero changes to any other class needed.
 *
 * Template path convention:
 * "email/welcome" → resources/templates/email/welcome.html
 */
@Getter
@RequiredArgsConstructor
public enum EmailTemplate {

    WELCOME(
        "email/welcome",
        "Welcome to Business App! 🎉"
    ),

    ACCOUNT_LOCKED(
        "email/account-locked",
        "⚠️ Security Alert — Account Locked"
    ),

    PASSWORD_RESET(
        "email/password-reset",
        "Reset Your Password"
    ),

    ORDER_CONFIRMATION(
        "email/order-confirmation",
        "Order Confirmed ✅"
    ),

    TRADE_CONFIRMATION(
        "email/trade-confirmation",
        "Trade Executed Successfully"
    );

    // Path to Thymeleaf template (relative to resources/templates/)
    private final String templatePath;

    // Default subject line if caller does not override
    private final String defaultSubject;
}
