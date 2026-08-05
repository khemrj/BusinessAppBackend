package com.example.firstapp.dto;

import com.example.firstapp.enums.EmailTemplate;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Email Request DTO.
 *
 * Single Responsibility:
 * Carries all data needed to send one email.
 * No sending logic. No template logic. Just data.
 *
 * IMMUTABLE by design:
 * @Getter without @Setter — cannot be modified after build.
 * @Builder — fluent construction pattern.
 *
 * Usage:
 * EmailRequest.builder()
 *     .to("kj@gmail.com")
 *     .toName("KJ")
 *     .template(EmailTemplate.WELCOME)
 *     .variable("username", "KJ")
 *     .build();
 */
@Getter
@Builder
public class EmailRequest {

    // Recipient email address
    private final String to;

    // Recipient's display name (used for personalization)
    private final String toName;

    // Override default subject from EmailTemplate enum
    // If null → EmailTemplate.defaultSubject is used
    private final String subject;

    // Which HTML template to render
    private final EmailTemplate template;

    // Variables merged into Thymeleaf template
    // Key: variable name in template (th:text="${username}")
    // Value: actual value to display
    private final Map<String, Object> variables;

    // Plain text version for email clients that block HTML
    // Accessibility best practice — always provide this
    private final String plainTextFallback;
}
