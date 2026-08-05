package com.example.firstapp.service;

import com.example.firstapp.dto.EmailRequest;
import com.example.firstapp.config.EmailRateLimiter;
import com.example.firstapp.enums.EmailTemplate;
import com.example.firstapp.email.EmailAddressValidator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Email Service Implementation.
 *
 * Responsible only for:
 *   1. Validating the email address
 *   2. Checking rate limits
 *   3. Rendering the HTML template
 *   4. Sending via Brevo SMTP (JavaMailSender)
 *   5. Logging success or failure
 *
 * Design rules:
 *   - @Async on the PUBLIC sender methods, so calls from other beans run on a
 *     background thread (and NOT inside the caller's transaction).
 *   - Email failure != business failure — never propagate to the caller.
 *   - Always log failures with enough detail to debug.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailAddressValidator emailValidator;
    private final EmailRateLimiter emailRateLimiter;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.from-name}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // Keep in sync with PasswordResetService.TOKEN_TTL
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 20;

    // ── CORE SEND METHOD ───────────────────────────────────────

    /**
     * Sends an email via Brevo SMTP. @Async so that direct external calls to
     * sendEmail(...) also run off the request thread. When invoked internally
     * from the specific sender methods below, it simply runs on the async
     * thread those methods already established.
     *
     * All exceptions are caught and logged — never propagated to the caller.
     */
    @Override
    @Async("emailTaskExecutor")
    public void sendEmail(EmailRequest request) {
        // ── Step 1: Validate email address ────────────────────
        if (!emailValidator.isValid(request.getTo())) {
            log.error("Invalid email address — skipping send: {}",
                    maskEmail(request.getTo()));
            return;
        }

        // ── Step 2: Check rate limit ───────────────────────────
        if (request.getTemplate() != EmailTemplate.PASSWORD_RESET
                && !emailRateLimiter.canSendGeneralEmail(request.getTo())) {
            log.warn("Email rate limited — skipping: {} template={}",
                    maskEmail(request.getTo()), request.getTemplate());
            return;
        }

        // ── Step 3: Render HTML template ───────────────────────
        String htmlContent;
        try {
            htmlContent = renderTemplate(
                    request.getTemplate().getTemplatePath(),
                    request.getVariables() != null
                            ? request.getVariables()
                            : new HashMap<>()
            );
        } catch (Exception e) {
            log.error("Template rendering failed: template={} error={}",
                    request.getTemplate(), e.getMessage());
            return;
        }

        // ── Step 4 + 5: Build and send email ──────────────────
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setTo(request.getTo());

            String subject = (request.getSubject() != null
                    && !request.getSubject().isBlank())
                    ? request.getSubject()
                    : request.getTemplate().getDefaultSubject();
            helper.setSubject(subject);

            if (request.getPlainTextFallback() != null) {
                // plain text + HTML = multipart/alternative
                helper.setText(request.getPlainTextFallback(), htmlContent);
            } else {
                helper.setText(htmlContent, true);
            }

            mailSender.send(message);

            log.info("Email sent: to={} template={}",
                    maskEmail(request.getTo()), request.getTemplate());

        } catch (MailException e) {
            // SMTP error: auth failed, connection refused, Brevo limit, DNS, etc.
            log.error("SMTP send failed: to={} template={} error={}",
                    maskEmail(request.getTo()), request.getTemplate(), e);
            // DO NOT rethrow — email failure ≠ business failure

        } catch (MessagingException e) {
            log.error("Email format error: to={} error={}",
                    maskEmail(request.getTo()), e.getMessage());

        } catch (UnsupportedEncodingException e) {
            log.error("Email encoding error: {}", e.getMessage());
        }
    }

    // ── SPECIFIC EMAIL METHODS ─────────────────────────────────
    // Each is @Async so it runs on the email thread pool when called from
    // another bean (e.g. AuthService, PasswordResetService), keeping SMTP work
    // off the request thread and out of the caller's transaction.

    @Override
    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(String toEmail, String username) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("username", username);
        vars.put("loginUrl", frontendUrl + "/login");
        vars.put("supportEmail", "support@businessapp.com");

        sendEmail(
                EmailRequest.builder()
                        .to(toEmail)
                        .toName(username)
                        .template(EmailTemplate.WELCOME)
                        .variables(vars)
                        .plainTextFallback(
                                "Welcome to Business App, " + username + "! "
                                + "Visit " + frontendUrl + "/login to get started.")
                        .build()
        );
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendAccountLockedEmail(String toEmail, String username) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("username", username);
        vars.put("supportEmail", "support@businessapp.com");
        vars.put("unlockAfterMinutes", 30);

        sendEmail(
                EmailRequest.builder()
                        .to(toEmail)
                        .toName(username)
                        .template(EmailTemplate.ACCOUNT_LOCKED)
                        .variables(vars)
                        .plainTextFallback(
                                "Your account has been locked due to multiple "
                                + "failed login attempts. It will unlock "
                                + "automatically in 30 minutes. Contact "
                                + "support@businessapp.com if needed.")
                        .build()
        );
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        // Stricter rate limit for password resets
        if (!emailRateLimiter.canSendPasswordReset(toEmail)) {
            log.warn("Password reset rate limited: {}", maskEmail(toEmail));
            // Return silently — caller shows a generic message either way
            return;
        }

        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

        Map<String, Object> vars = new HashMap<>();
        vars.put("username", username);
        vars.put("resetUrl", resetUrl);
        // Matches PasswordResetService.TOKEN_TTL (20 minutes)
        vars.put("expiryMinutes", RESET_TOKEN_EXPIRY_MINUTES);

        sendEmail(
                EmailRequest.builder()
                        .to(toEmail)
                        .toName(username)
                        .template(EmailTemplate.PASSWORD_RESET)
                        .variables(vars)
                        .plainTextFallback(
                                "Reset your password at: " + resetUrl
                                + " (link expires in " + RESET_TOKEN_EXPIRY_MINUTES
                                + " minutes)")
                        .build()
        );
    }

    // ── TEMPLATE RENDERING ─────────────────────────────────────

    private String renderTemplate(String templatePath, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        // Global variables available in EVERY template
        context.setVariable("appName", "Business App");
        context.setVariable("frontendUrl", frontendUrl);
        context.setVariable("currentYear", java.time.Year.now().getValue());
        context.setVariable("supportEmail", "support@businessapp.com");

        // e.g. resources/templates/email/password-reset.html
        return templateEngine.process(templatePath, context);
    }

    /**
     * Masks an email for safe logging: "kj@gmail.com" → "k**@gmail.com".
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 1) {
            return "*@" + parts[1];
        }
        return name.charAt(0) + "**@" + parts[1];
    }
}