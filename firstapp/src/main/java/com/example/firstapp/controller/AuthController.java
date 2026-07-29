package com.example.firstapp.controller;

import com.example.firstapp.dto.LoginRequest;
import com.example.firstapp.dto.SignupRequest;
import com.example.firstapp.dto.ApiResponse;
import com.example.firstapp.dto.AuthResponse;
import com.example.firstapp.entity.User;
import com.example.firstapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost
        .PreAuthorize;
import org.springframework.security.core.annotation
        .AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller — thin layer, only HTTP concerns.
 *
 * RESPONSIBILITY: Only handle HTTP request/response.
 * Business logic BELONGS IN AuthService — not here.
 *
 * All /api/v1/auth/** endpoints are PUBLIC.
 * Configured in SecurityConfig requestMatchers.
 *
 * CONSTRUCTOR INJECTION via @RequiredArgsConstructor:
 * Single final field AuthService — injected via constructor.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    // Constructor injection — final, immutable, testable
    private final AuthService authService;

    /**
     * POST /api/v1/auth/signup
     * Register new user account.
     *
     * @Valid triggers Bean Validation on SignupRequest.
     * If validation fails → MethodArgumentNotValidException
     * → GlobalExceptionHandler → 400 with field errors map.
     *
     * Flutter sends:
     * POST /api/v1/auth/signup
     * Content-Type: application/json
     * {
     *   "username": "KJ",
     *   "email": "kj@gmail.com",
     *   "password": "MyPass@123"
     * }
     *
     * Returns: 201 Created with JWT token
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(
            @RequestBody @Valid SignupRequest request
    ) {
        log.info("Signup request received: {}",
                request.getEmail());

        AuthResponse authResponse =
                authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED) // 201
                .body(ApiResponse.success(
                    "Account created successfully",
                    authResponse
                ));
    }

    /**
     * POST /api/v1/auth/login
     * Authenticate existing user.
     *
     * Flutter sends:
     * POST /api/v1/auth/login
     * Content-Type: application/json
     * {
     *   "email": "kj@gmail.com",
     *   "password": "MyPass@123"
     * }
     *
     * Returns: 200 OK with JWT token
     * Flutter stores token in FlutterSecureStorage
     * Sends token in every subsequent request:
     * Authorization: Bearer <token>
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody @Valid LoginRequest request
    ) {
        log.info("Login request received: {}",
                request.getEmail());

        AuthResponse authResponse =
                authService.login(request);

        return ResponseEntity.ok(
            ApiResponse.success(
                "Login successful",
                authResponse
            )
        );
    }

    /**
     * GET /api/v1/auth/me
     * Get current authenticated user's profile.
     * Requires valid JWT token in Authorization header.
     *
     * @AuthenticationPrincipal — Spring injects the current
     * user directly from SecurityContextHolder.
     * Set by JwtAuthFilter earlier in the filter chain.
     * No extra DB call needed.
     *
     * Flutter sends:
     * GET /api/v1/auth/me
     * Authorization: Bearer eyJhbGci...
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> getProfile(
            @AuthenticationPrincipal User currentUser
    ) {
        // Return safe user info — no password, no sensitive data
        return ResponseEntity.ok(
            ApiResponse.success(
                "Profile fetched successfully",
                buildProfileResponse(currentUser)
            )
        );
    }

    /**
     * POST /api/v1/auth/logout
     *
     * JWT is stateless — server has no session to invalidate.
     * Client (Flutter) must DELETE the stored token.
     *
     * This endpoint exists for:
     * → REST convention consistency
     * → Future: add token to blacklist (Redis)
     * → Future: revoke refresh tokens
     *
     * Flutter: on receiving this response,
     * call FlutterSecureStorage.delete('jwt_token')
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(
            ApiResponse.success(
                "Logged out successfully. " +
                "Please delete your token."
            )
        );
    }

    /**
     * Admin-only example endpoint.
     * Shows @PreAuthorize usage on controller method.
     * AccessDeniedException if non-admin tries to access.
     * → GlobalExceptionHandler → 403 Forbidden
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminOnly() {
        return ResponseEntity.ok(
            ApiResponse.success(
                "Admin endpoint accessed successfully"
            )
        );
    }

    /**
     * Builds safe profile response.
     * Never expose: password, failedAttempts, lockTime
     * Only expose: id, username, email, role, dates
     */
    private Object buildProfileResponse(User user) {
        return new Object() {
            public final Long id = user.getId();
            public final String username =
                    user.getUsername();
            public final String email = user.getEmail();
            public final String role =
                    user.getRole().name();
            public final boolean active = user.isEnabled();
            public final java.time.LocalDateTime createdAt =
                    user.getCreatedAt();
            public final java.time.LocalDateTime lastLogin =
                    user.getLastLoginAt();
        };
    }
}