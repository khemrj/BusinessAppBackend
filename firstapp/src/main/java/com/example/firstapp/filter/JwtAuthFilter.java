package com.example.firstapp.filter;

import com.example.firstapp.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication
        .UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context
        .SecurityContextHolder;
import org.springframework.security.core.userdetails
        .UserDetails;
import org.springframework.security.core.userdetails
        .UserDetailsService;
import org.springframework.security.web.authentication
        .WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter.
 *
 * EXTENDS OncePerRequestFilter:
 * Guarantees filter executes EXACTLY ONCE per request.
 * Even in redirect/forward chains within app.
 * Parent class handles "once" logic — we implement logic.
 *
 * POSITION IN FILTER CHAIN:
 * 1. RateLimitFilter (blocks if rate exceeded)
 * 2. JwtAuthFilter (THIS — authenticates JWT)
 * 3. Spring Security Authorization (checks roles)
 * 4. Controller (business logic)
 *
 * WHAT IT DOES:
 * Extracts JWT → Validates → Loads User →
 * Sets Authentication in SecurityContext
 *
 * WHAT IT DOES NOT DO:
 * → Does NOT block requests (always calls filterChain.doFilter)
 * → Does NOT return error responses
 * → Does NOT make authorization decisions
 * Those are Spring Security's job downstream.
 *
 * CONSTRUCTOR INJECTION via @RequiredArgsConstructor:
 * All dependencies injected through constructor.
 * No @Autowired needed — cleaner and testable.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    // All final — injected via constructor (Lombok)
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Core filter logic — called ONCE per request.
     *
     * COMPLETE FLOW:
     * 1. Extract Authorization header from request
     * 2. Guard: exit early if no valid Bearer token
     * 3. Extract JWT string from header
     * 4. Extract username (email) from JWT
     * 5. Guard: exit if username null or already authenticated
     * 6. Load user from database
     * 7. Validate token (username match + not expired)
     * 8. Create Authentication object
     * 9. Set request details on auth object
     * 10. Store in SecurityContextHolder (KEY MOMENT)
     * 11. Continue filter chain (ALWAYS)
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        log.debug("JWT Filter: {} {}",
                request.getMethod(),
                request.getRequestURI());

        // ── STEP 1: Extract Authorization header ──────────────
        // Flutter sets: Authorization: Bearer eyJhbGci...
        // getHeader() returns null if header absent
        final String authHeader =
                request.getHeader("Authorization");

        // ── STEP 2: Guard — exit if no Bearer token ───────────
        // Missing or wrong format → pass through without auth
        // Public endpoints (login/signup) hit this guard
        // and pass through to AuthController correctly
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token — passing through: {}",
                    request.getRequestURI());
            // MUST call this to continue request processing
            filterChain.doFilter(request, response);
            return; // EXIT — nothing more to do
        }

        // ── STEP 3: Extract JWT string ─────────────────────────
        // "Bearer eyJhbGci..." → "eyJhbGci..."
        // substring(7) removes "Bearer " (7 chars including space)
        final String jwt = authHeader.substring(7);

        // ── STEP 4: Extract username (email) from token ────────
        final String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
            // Decodes JWT and reads "sub" claim
            // Throws JwtException if malformed/tampered
        } catch (Exception e) {
            log.warn("Cannot extract username from token: {}",
                    e.getMessage());
            // Invalid token — pass through without setting auth
            // Spring Security returns 401 for protected endpoints
            filterChain.doFilter(request, response);
            return; // EXIT
        }

        // ── STEP 5: Process if username found and not yet authed
        // Condition 1: userEmail != null
        //   Successfully extracted email from token
        // Condition 2: getAuthentication() == null
        //   Not already authenticated (prevent re-auth)
        //   SecurityContext is empty at start of each request
        if (userEmail != null
                && SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

            // ── STEP 6: Load user from database ───────────────
            // Load even though email is in token because:
            // → Verify user still exists (not deleted)
            // → Get CURRENT role (may have changed)
            // → Verify account still active/unlocked
            // SQL: SELECT * FROM users WHERE email = ?
            UserDetails userDetails =
                    userDetailsService
                            .loadUserByUsername(userEmail);

            // ── STEP 7: Validate token ─────────────────────────
            // isTokenValid checks:
            // 1. email in token == loaded user's email
            //    Prevents one user's token for another account
            // 2. Token expiry has not passed
            //    Forces re-login after session timeout
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // ── STEP 8: Create Authentication object ──────
                // 3-argument constructor = FULLY AUTHENTICATED
                // (setAuthenticated(true) called internally)
                // 2-argument = NOT authenticated (different!)
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,                  // principal: who
                        null,                         // credentials: null (JWT validated — no password needed)
                        userDetails.getAuthorities()  // authorities: [ROLE_ADMIN]
                    );

                // ── STEP 9: Attach request details ────────────
                // Stores IP address, session ID in auth token
                // Available for audit logging downstream
                authToken.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
                );

                // ── STEP 10: SET IN SECURITY CONTEXT ──────────
                // THE MOST IMPORTANT LINE IN THIS FILTER
                // All downstream Spring Security checks read from here:
                // → @PreAuthorize("hasRole('ADMIN')") reads authorities
                // → @AuthenticationPrincipal injects the user object
                // → authentication.getName() returns email
                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);

                log.debug("Authenticated: {} → {}",
                        userEmail,
                        request.getRequestURI());

            } else {
                // Token invalid (expired or signature mismatch)
                // SecurityContext remains empty
                // Spring Security returns 401 for protected endpoints
                log.warn("Token validation failed: {}",
                        userEmail);
            }
        }

        // ── STEP 11: ALWAYS continue filter chain ─────────────
        // Even if auth failed — pass to next filter
        // Spring Security authorization filter decides:
        // → Public endpoint? → allow
        // → Protected + authenticated? → allow
        // → Protected + no auth? → 401 Unauthorized
        // → Protected + wrong role? → 403 Forbidden
        filterChain.doFilter(request, response);
    }
}
