package com.example.firstapp.email;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Email Address Validator.
 *
 * Single Responsibility:
 * ONLY validates email address format.
 * Nothing else lives here.
 *
 * WHY validate before sending?
 * JavaMailSender throws MessagingException for
 * invalid addresses — catching and handling that
 * is messier than validating upfront.
 * Early validation = cleaner error handling.
 */
@Component
public class EmailAddressValidator {

    // RFC 5322 compliant email regex
    // Covers the vast majority of real email addresses
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+" +
            "(?:\\.[a-zA-Z0-9_+&*-]+)*" +
            "@(?:[a-zA-Z0-9-]+\\.)" +
            "+[a-zA-Z]{2,7}$"
        );

    /**
     * Validates email address format.
     *
     * @param email Email address to validate
     * @return true if valid format, false otherwise
     */
    public boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        // Length check — RFC 5321 max 254 characters
        if (email.length() > 254) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates and throws if invalid.
     * Use when invalid email should stop processing.
     *
     * @param email Email address to validate
     * @throws IllegalArgumentException if invalid
     */
    public void validateOrThrow(String email) {
        if (!isValid(email)) {
            throw new IllegalArgumentException(
                "Invalid email address format: " + email
            );
        }
    }
}
