package com.example.firstapp.config;

import io.github.bucket4j.Bucket;

import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limit Configuration — all limits defined in ONE place.
 *
 * TOKEN BUCKET ALGORITHM:
 * → Bucket starts with N tokens (capacity)
 * → Each request consumes 1 token
 * → Tokens refill at configured rate
 * → If bucket empty → 429 Too Many Requests
 *
 * GREEDY vs INTERVALLY refill:
 * Greedy: tokens drip back smoothly (1 per interval)
 *   Better: attacker gets 1 token every 12 seconds (for 5/min)
 * Intervally: all tokens return at once after interval
 *   Worse: attacker can burst again every minute
 *
 * We use GREEDY for all auth endpoints (more secure).
 *
 * DIFFERENT LIMITS PER ENDPOINT (industry standard):
 * Login:          5/min  → brute force protection
 * Signup:         3/hour → bot account prevention
 * Password reset: 3/hour → account takeover protection
 * Write ops:      30/min → data mutation protection
 * Read ops:       300/min → liberal for reads
 * General:        100/min → default protection
 */
@Configuration
public class RateLimitConfig {

    // ── Capacity Constants ─────────────────────────────────────

    // Login: 5 per minute per IP
    public static final int LOGIN_CAPACITY = 5;
    public static final Duration LOGIN_REFILL_DURATION =
            Duration.ofMinutes(3);

    // Signup: 3 per hour per IP
    public static final int SIGNUP_CAPACITY = 3;
    public static final Duration SIGNUP_REFILL_DURATION =
            Duration.ofHours(1);

    // General API: 100 per minute per user/IP
    public static final int GENERAL_CAPACITY = 100;
    public static final Duration GENERAL_REFILL_DURATION =
            Duration.ofMinutes(1);

    // Write operations: 30 per minute per user
    public static final int WRITE_CAPACITY = 30;
    public static final Duration WRITE_REFILL_DURATION =
            Duration.ofMinutes(1);

    // ── Bucket Factory Methods ─────────────────────────────────

    /**
     * Login bucket — strictest protection.
     * 5 per minute + max 20 per hour.
     *
     * TWO LIMITS simultaneously:
     * Limit 1: 5 per minute (burst protection)
     * Limit 2: 20 per hour (sustained protection)
     * BOTH must have tokens — prevents minute-by-minute attack.
     */
    public Bucket createLoginBucket() {
        return Bucket.builder()
                // Limit 1: 5 per minute
                .addLimit(limit -> limit  // depriated so i changed from looking at gemini
                    .capacity(LOGIN_CAPACITY)
                    .refillGreedy(LOGIN_CAPACITY, LOGIN_REFILL_DURATION)
                )
                // Limit 2: 20 per hour sustained
                .addLimit(limit -> limit
                    .capacity(20)
                    .refillGreedy(20, Duration.ofHours(1))
                )
                .build();
    }

    /**
     * Signup bucket.
     * 3 per hour per IP.
     * Prevents automated bot account creation.
     */
    public Bucket createSignupBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                    .capacity(SIGNUP_CAPACITY)
                    .refillGreedy(SIGNUP_CAPACITY, SIGNUP_REFILL_DURATION)
                )
                .build();
    }

    /**
     * General API bucket.
                        Refill.greedy(
                                SIGNUP_CAPACITY,
                                SIGNUP_REFILL_DURATION
                        )
                ))
                .build();
    }

    /**
     * General API bucket.
     * 100 per minute + 1000 per hour.
     * Used for most authenticated endpoints.
     */
    public Bucket createGeneralBucket() {
        return Bucket.builder()
                // Burst: 100 per minute
                .addLimit(limit -> limit
                    .capacity(GENERAL_CAPACITY)
                    .refillGreedy(GENERAL_CAPACITY, GENERAL_REFILL_DURATION)
                )
                // Sustained: 1000 per hour
                .addLimit(limit -> limit
                    .capacity(1000)
                    .refillGreedy(1000, Duration.ofHours(1))
                        )
    
                .build();
    }

    /**
     * Write operation bucket.
     * 30 per minute per user.
     * Stricter than general — protects data mutations.
     */
    public Bucket createWriteBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                    .capacity(WRITE_CAPACITY)
                    .refillGreedy(WRITE_CAPACITY, WRITE_REFILL_DURATION)
                )
                
                .build();
    }
}
