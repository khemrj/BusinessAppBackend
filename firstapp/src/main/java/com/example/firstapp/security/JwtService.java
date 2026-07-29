package com.example.firstapp.security;

import com.example.firstapp.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtService — All JWT operations in ONE place.
 *
 * RESPONSIBILITIES:
 * 1. Generate signed JWT tokens after authentication
 * 2. Extract claims (data) from incoming tokens
 * 3. Validate tokens (signature + expiry + username match)
 *
 * JWT STRUCTURE: HEADER.PAYLOAD.SIGNATURE
 * Header:    {"alg":"HS256","typ":"JWT"}
 * Payload:   {"sub":"email","role":"ROLE_ADMIN","exp":...}
 * Signature: HMAC256(header+payload, secretKey)
 *
 * SECURITY:
 * → Secret key from properties (never hardcoded)
 * → All exceptions caught (never leak JWT errors)
 * → Payload is signed NOT encrypted
 *   (never put passwords or secrets in payload)
 */
@Component
@Slf4j
public class JwtService {

    // Loaded from application.properties
    // Production: use environment variable ${JWT_SECRET}
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ── TOKEN GENERATION ────────────────────────────────────────

    /**
     * Generates signed JWT token for authenticated user.
     * Called after successful login or signup.
     *
     * Payload contains:
     * sub    = email (primary identifier)
     * userId = database ID
     * role   = user role for @PreAuthorize
     * username = display name
     * iat    = issued at timestamp
     * exp    = expiration timestamp
     */
    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("username", user.getUsername());
        // NEVER add password or sensitive data here
        // JWT payload is base64 encoded — readable by anyone

        return Jwts.builder()
                .setClaims(extraClaims)
                // "sub" = who this token belongs to
                .setSubject(user.getEmail())
                // "iat" = issued at
                .setIssuedAt(
                    new Date(System.currentTimeMillis())
                )
                // "exp" = expires at
                .setExpiration(new Date(
                    System.currentTimeMillis() + jwtExpiration
                ))
                // Sign with HMAC-SHA256
                // Any modification to payload = invalid signature
                .signWith(
                    getSigningKey(),
                    SignatureAlgorithm.HS256
                )
                // Serialize to "xxxxx.yyyyy.zzzzz" format
                .compact();
    }

    // ── CLAIM EXTRACTION ────────────────────────────────────────

    /**
     * Extracts email from token "sub" claim.
     * Called by JwtAuthFilter to identify requesting user.
     */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.getSubject());
    }

    /**
     * Extracts expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, claims -> claims.getExpiration());
    }

    /**
     * Extracts userId from token claims.
     * Use instead of DB call when only ID needed.
     */
    public Long extractUserId(String token) {
        return extractClaim(token,
            claims -> claims.get("userId", Long.class)
        );
    }

    /**
     * Extracts role from token claims.
     */
    public String extractRole(String token) {
        return extractClaim(token,
            claims -> claims.get("role", String.class)
        );
    }

    /**
     * Generic claim extractor using Function resolver.
     * Higher-order function — caller decides what to extract.
     *
     * Usage:
     * extractClaim(token, Claims::getSubject) → email
     * extractClaim(token, Claims::getExpiration) → date
     * extractClaim(token, c -> c.get("role", String.class)) → role
     */
    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses token and extracts ALL claims.
     * ALSO verifies signature as part of parsing.
     *
     * Throws:
     * ExpiredJwtException    — token past expiry
     * SignatureException     — token was tampered
     * MalformedJwtException  — not valid JWT format
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                // Signing key used to VERIFY signature
                .setSigningKey(getSigningKey())
                .build()
                // Parses token AND verifies signature
                .parseClaimsJws(token)
                // Gets the payload (claims/data)
                .getBody();
    }

    // ── TOKEN VALIDATION ────────────────────────────────────────

    /**
     * Validates token — THE critical security check.
     *
     * CHECK 1: Username match
     * Email in token must match loaded user's email.
     * Prevents stolen token being used for another account.
     *
     * CHECK 2: Not expired
     * Current time must be before token expiry date.
     * Forces re-authentication after session timeout.
     *
     * All JWT exceptions caught here — never propagate raw.
     *
     * @return true if token valid, false if invalid/expired
     */
    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {
        try {
            final String tokenUsername =
                extractUsername(token);

            // Both conditions must be true simultaneously
            return tokenUsername.equals(
                        userDetails.getUsername()
                   )
                   && !isTokenExpired(token);

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            // Payload was tampered with
            log.warn("JWT signature invalid — " +
                     "possible tampering: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT empty/null: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if token "exp" claim is before current time.
     * true  = token HAS expired (past expiry date)
     * false = token still valid (not yet expired)
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token)
                .before(new Date());
    }

    /**
     * Converts Base64-encoded secret string to Key object.
     * HMAC-SHA256 requires Key object — not raw string.
     * Called every time a token is signed or verified.
     */
    private Key getSigningKey() {
        byte[] keyBytes =
            Decoders.BASE64.decode(secretKey);
        // Keys.hmacShaKeyFor validates key meets HMAC requirements
        // Throws WeakKeyException if key < 256 bits
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
