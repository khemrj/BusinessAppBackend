package com.example.firstapp.filter;

import com.example.firstapp.config.RateLimitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context
        .SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter using Bucket4j.
 *
 * RUNS BEFORE JwtAuthFilter in the filter chain.
 * Rejects rate-exceeded requests before any processing.
 *
 * DIFFERENT LIMITS PER ENDPOINT TYPE:
 * /auth/login  → 5/min per IP (brute force protection)
 * /auth/signup → 3/hour per IP (bot protection)
 * POST/PUT/DELETE → 30/min per user (write protection)
 * GET and others → 100/min per user (general protection)
 *
 * PER-USER vs PER-IP:
 * Authenticated requests → limit per userId (fair, precise)
 * Unauthenticated requests → limit per IP (only option)
 * Multiple users behind same NAT share IP — userId is fairer
 *
 * RESPONSE HEADERS (industry standard):
 * X-RateLimit-Remaining → tokens left this window
 * X-RateLimit-Reset → seconds until refill
 * Retry-After → seconds to wait (on 429 only)
 *
 * IN-MEMORY vs REDIS:
 * This uses ConcurrentHashMap (single instance only)
 * For multiple instances → switch to Redis-backed Bucket4j
 * Redis needed when horizontal scaling is required
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    // Injected via constructor (Lombok)
    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    // ── Bucket Stores — one bucket per IP/user ─────────────────
    // ConcurrentHashMap = thread-safe for concurrent requests
    // Key format: "login:192.168.1.1" or "general:user@email.com"
    // Namespaced keys prevent collision between different limit types

    private final Map<String, Bucket> loginBuckets =
            new ConcurrentHashMap<>();

    private final Map<String, Bucket> signupBuckets =
            new ConcurrentHashMap<>();

    private final Map<String, Bucket> generalBuckets =
            new ConcurrentHashMap<>();

    private final Map<String, Bucket> writeBuckets =
            new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = extractClientIp(request);

        // ── Determine which rate limit applies ─────────────────
        ConsumptionProbe probe;
        String limitType;

        if (uri.contains("/auth/login")) {
            // Strictest: 5 per minute per IP
            // Brute force protection
            Bucket bucket = loginBuckets.computeIfAbsent(
                "login:" + clientIp,
                k -> rateLimitConfig.createLoginBucket()
            );
            probe = bucket.tryConsumeAndReturnRemaining(1);
            limitType = "LOGIN";

        } else if (uri.contains("/auth/signup")) {
            // Strict: 3 per hour per IP
            // Bot account prevention
            Bucket bucket = signupBuckets.computeIfAbsent(
                "signup:" + clientIp,
                k -> rateLimitConfig.createSignupBucket()
            );
            probe = bucket.tryConsumeAndReturnRemaining(1);
            limitType = "SIGNUP";

        } else {
            // For all other endpoints
            // Use userId for authenticated, IP for anonymous
            // userId is more fair (multiple users can share IP)
            String identifier = resolveIdentifier(clientIp);

            boolean isWriteOperation =
                    method.equals(HttpMethod.POST.name()) ||
                    method.equals(HttpMethod.PUT.name()) ||
                    method.equals(HttpMethod.DELETE.name()) ||
                    method.equals(HttpMethod.PATCH.name());

            if (isWriteOperation) {
                // Stricter for data-mutating operations
                Bucket bucket = writeBuckets.computeIfAbsent(
                    "write:" + identifier,
                    k -> rateLimitConfig.createWriteBucket()
                );
                probe = bucket
                        .tryConsumeAndReturnRemaining(1);
                limitType = "WRITE";
            } else {
                // Standard limit for reads and other operations
                Bucket bucket = generalBuckets.computeIfAbsent(
                    "general:" + identifier,
                    k -> rateLimitConfig.createGeneralBucket()
                );
                probe = bucket
                        .tryConsumeAndReturnRemaining(1);
                limitType = "GENERAL";
            }
        }

        // ── Add standard rate limit headers to response ────────
        // These are set on EVERY response (not just 429)
        // Clients use these for proactive throttling
        addRateLimitHeaders(response, probe);

        // ── Allow or block request ─────────────────────────────
        if (probe.isConsumed()) {
            // Token consumed → request is ALLOWED
            log.debug("Rate limit OK: {} {} remaining={}",
                    limitType, uri,
                    probe.getRemainingTokens());
            // Continue to next filter (JwtAuthFilter)
            filterChain.doFilter(request, response);

        } else {
            // No tokens left → REJECT with 429
            long waitSeconds =
                probe.getNanosToWaitForRefill() // what does thil line do? 
                / 1_000_000_000L;

            log.warn(
                "Rate limit EXCEEDED: {} for IP: {} " +
                "wait: {}s",
                limitType, clientIp, waitSeconds
            );

            sendRateLimitExceededResponse(
                response, waitSeconds, limitType
            );
            // DO NOT call filterChain.doFilter()
            // Request is BLOCKED here
        }
    }

    /**
     * Resolves identifier for rate limiting.
     * Authenticated users → use their email (userId)
     * Anonymous users → use IP address
     *
     * WHY userId over IP?
     * Multiple users behind corporate NAT share same IP.
     * Using IP would unfairly affect ALL users behind NAT.
     * Using email/userId is precise — one limit per user.
     *
     * NOTE: JwtAuthFilter runs AFTER RateLimitFilter.
     * For non-auth endpoints, SecurityContext may already
     * be populated if this isn't the first request.
     * For auth endpoints (login/signup), we always use IP.
     */
    private String resolveIdentifier(String clientIp) {
        Authentication auth = SecurityContextHolder
                .getContext().getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(
                        auth.getPrincipal())) {
            // Use email for authenticated users
            return "user:" + auth.getName();
        }

        // Fall back to IP for unauthenticated
        return "ip:" + clientIp;
    }

    /**
     * Adds standard rate limit headers to every response.
     * Industry standard — GitHub, Stripe, Twitter all do this.
     * Clients use these for proactive throttling.
     */
    private void addRateLimitHeaders(
            HttpServletResponse response,
            ConsumptionProbe probe
    ) {
        // Remaining tokens in current window
        response.setHeader(
            "X-RateLimit-Remaining",
            String.valueOf(probe.getRemainingTokens())
        );

        // Nanoseconds until next token refill → seconds
        long resetSeconds =
            probe.getNanosToWaitForRefill()
            / 1_000_000_000L;

        response.setHeader(
            "X-RateLimit-Reset",
            String.valueOf(resetSeconds)
        );

        // If rate limited — tell client how long to wait
        if (!probe.isConsumed()) {
            response.setHeader(
                "Retry-After",
                String.valueOf(resetSeconds)
            );
        }
    }

    /**
     * Sends 429 Too Many Requests response.
     * Structured JSON that Flutter can parse and display.
     *
     * Response body:
     * {
     *   "success": false,
     *   "error": "RATE_LIMIT_EXCEEDED",
     *   "message": "Too many requests...",
     *   "retryAfterSeconds": 47,
     *   "limitType": "LOGIN"
     * }
     */
    private void sendRateLimitExceededResponse(
            HttpServletResponse response,
            long waitSeconds,
            String limitType
    ) throws IOException {

        // 429 = Too Many Requests (correct status code)
        // NOT 403 Forbidden or 401 Unauthorized
        response.setStatus(
            HttpStatus.TOO_MANY_REQUESTS.value()
        );
        response.setContentType(
            MediaType.APPLICATION_JSON_VALUE
        );

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "RATE_LIMIT_EXCEEDED");
        body.put("message",
            "Too many requests. " +
            "Please wait before trying again."
        );
        body.put("retryAfterSeconds", waitSeconds);
        body.put("limitType", limitType);

        response.getWriter()
                .write(objectMapper
                        .writeValueAsString(body));
    }

    /**
     * Extracts real client IP from request.
     * Handles load balancer and reverse proxy scenarios.
     *
     * X-Forwarded-For: set by Nginx/AWS ELB/CloudFlare
     * Format: "clientIP, proxy1IP, proxy2IP"
     * We want FIRST IP (original client).
     *
     * SECURITY NOTE: X-Forwarded-For can be spoofed.
     * Only trust it from known proxy infrastructure.
     * In production: configure Nginx to set this header
     * and trust only requests from your proxy.
     */
    private String extractClientIp(
            HttpServletRequest request
    ) {
        // Check headers in priority order
        String[] proxyHeaders = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : proxyHeaders) {
            String value = request.getHeader(header);
            if (value != null
                    && !value.isEmpty()
                    && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For may have: "client, p1, p2"
                // Take FIRST = original client IP
                return value.split(",")[0].trim();
            }
        }

        // Direct connection — actual socket IP
        return request.getRemoteAddr();
    }
}
