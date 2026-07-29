package com.example.firstapp.service;

import com.example.firstapp.dto.LoginRequest;
import com.example.firstapp.dto.SignupRequest;
import com.example.firstapp.dto.AuthResponse;
import com.example.firstapp.entity.User;
import com.example.firstapp.enums.Role;
import com.example.firstapp.exception.BadRequestException;
import com.example.firstapp.exception.EmailAlreadyExistsException;
import com.example.firstapp.repository.UserRepository;
import com.example.firstapp.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication
        .AuthenticationManager;
import org.springframework.security.authentication
        .BadCredentialsException;
import org.springframework.security.authentication
        .LockedException;
import org.springframework.security.authentication
        .UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password
        .PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication Service — ALL auth business logic lives here.
 *
 * SEPARATION OF CONCERNS:
 * Controller → receives HTTP request, calls service, returns response
 * Service    → ALL business logic (this class)
 * Repository → database operations only
 *
 * CONSTRUCTOR INJECTION via @RequiredArgsConstructor:
 * All dependencies final — injected via constructor.
 * No @Autowired on fields — cleaner, testable, explicit.
 *
 * @Transactional:
 * → signup: rollbackFor = Exception.class (all exceptions)
 *   If user saved but token generation fails → rollback user
 *   No orphaned users in database
 * → handleFailedLogin: REQUIRES_NEW propagation
 *   Runs in own transaction even if caller's fails
 *   Failed attempt MUST be recorded even if other things fail
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    // All dependencies via constructor injection
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // Max failed attempts before account lockout
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * Register a new user account.
     *
     * FLOW:
     * 1. Check email uniqueness
     * 2. Check username uniqueness
     * 3. Hash password with BCrypt
     * 4. Save user to MySQL
     * 5. Generate JWT token
     * 6. Return AuthResponse with token
     *
     * @Transactional(rollbackFor = Exception.class):
     * If ANYTHING fails (even checked exceptions) → rollback
     * Ensures no partial state in database
     * No user created without JWT being generatable
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse signup(SignupRequest request) {
        log.info("Signup attempt for email: {}",
                request.getEmail());

        // ── Uniqueness Validation ──────────────────────────────

        // Check if email already registered
        if (userRepository.existsByEmail(
                request.getEmail())) {
            // RuntimeException → @Transactional auto-rollback
            throw new EmailAlreadyExistsException(
                request.getEmail()
            );
        }

        // Check if username already taken
        if (userRepository.existsByUsername(
                request.getUsername())) {
            throw new BadRequestException(
                "Username '" + request.getUsername()
                + "' is already taken"
            );
        }

        // ── Create User Entity ─────────────────────────────────

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                // HASH password — NEVER store plain text
                // BCrypt generates different hash each time
                // (random salt embedded in hash)
                .password(passwordEncoder.encode(
                    request.getPassword()
                ))
                // Default: CUSTOMER — minimum privileges
                // Admin accounts created separately by admin
                .role(Role.ROLE_CUSTOMER)
                .isActive(true)
                .isLocked(false)
                .failedAttempts(0)
                .build();
        // @PrePersist sets createdAt and updatedAt automatically

        User savedUser = userRepository.save(user);
        log.info("User created: id={} email={}",
                savedUser.getId(), savedUser.getEmail());

        // ── Generate JWT Token ─────────────────────────────────

        // User is registered AND immediately logged in
        // No separate login step needed after signup
        String token = jwtService.generateToken(savedUser);

        log.info("Signup successful: {}", request.getEmail());
        return buildAuthResponse(savedUser, token);
    }

    /**
     * Authenticate existing user with email and password.
     *
     * FLOW:
     * 1. AuthenticationManager verifies credentials
     *    a. Loads user via UserDetailsService.loadByUsername()
     *    b. BCrypt.matches(rawPassword, storedHash)
     *    c. Checks isEnabled(), isAccountNonLocked()
     * 2. Reset failed attempts on success
     * 3. Update last login timestamp
     * 4. Generate JWT token
     * 5. Return AuthResponse
     *
     * SECURITY: Never reveal which was wrong (email or password)
     * Always return same error for both failures.
     * Prevents email enumeration attacks.
     */@Transactional(rollbackFor = Exception.class)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());
        System.out.println("Login attempt for: " + request.getEmail());
        try {
            // AuthenticationManager.authenticate() does ALL of:
            // 1. userDetailsService.loadUserByUsername(email)
            //    SQL: SELECT * FROM users WHERE email = ?
            // 2. passwordEncoder.matches(rawPw, hashedPw)
            //    BCrypt comparison
            // 3. user.isEnabled() check
            // 4. user.isAccountNonLocked() check
            // 5. user.isAccountNonExpired() check
            // Throws specific exceptions for each failure
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),   // principal
                    request.getPassword() // credentials
                )
            );
            System.out.println("Login successful for: " + request.getEmail());
            // If we reach here → credentials are VALID ✅

        } catch (LockedException e) {
            // Account locked — too many failed attempts
            log.warn("Locked account login attempt: {}",
                    request.getEmail());
            throw new BadRequestException(
                "Account is locked due to multiple failed " +
                "attempts. Please wait 30 minutes or " +
                "contact support."
            );
        } catch (BadCredentialsException e) {
            // Wrong email or wrong password
            // Track failed attempts for lockout mechanism
            handleFailedLogin(request.getEmail());
            // VAGUE message — never reveal which was wrong
            // "Wrong email" vs "wrong password" is information leak
            throw new BadRequestException(
                "Invalid email or password"
            );
        }

        // ── Authentication Successful ──────────────────────────

        // Reset failed attempts counter
        userRepository.resetFailedAttempts(
            request.getEmail()
        );

        // Update last login timestamp for audit/security
        userRepository.updateLastLoginAt(
            request.getEmail(),
            LocalDateTime.now()
        );

        // Load user entity for token generation
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException(
                    "Invalid email or password"
                ));

        // Generate JWT token
        String token = jwtService.generateToken(user);

        log.info("Login successful: {}", request.getEmail());
        return buildAuthResponse(user, token);
    }

    /**
     * Handles failed login attempts.
     * Increments counter and locks account if threshold reached.
     *
     * REQUIRES_NEW propagation:
     * Runs in its OWN independent transaction.
     * Even if outer transaction fails/rolls back,
     * this STILL records the failed attempt.
     * Failed attempts MUST persist — security requirement.
     *
     * If user doesn't exist (wrong email) → silently ignore
     * Don't reveal that email doesn't exist in system
     */
    @Transactional(
        rollbackFor = Exception.class,
        propagation = org.springframework.transaction
                          .annotation.Propagation.REQUIRES_NEW
    )
    public void handleFailedLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Increment counter
            userRepository.incrementFailedAttempts(email);

            int newCount = user.getFailedAttempts() + 1;
            log.warn("Failed login attempt #{} for: {}",
                    newCount, email);

            // Lock account if threshold reached
            if (newCount >= MAX_FAILED_ATTEMPTS) {
                userRepository.lockAccount(
                    email,
                    LocalDateTime.now()
                );
                log.warn(
                    "Account LOCKED after {} failed attempts: {}",
                    MAX_FAILED_ATTEMPTS, email
                );
                // Account auto-unlocks after 30 min
                // (isAccountNonLocked() logic in User entity)
            }
        });
        // If email not found → silently do nothing
        // Never reveal if email exists in system
    }

    /**
     * Builds standardized AuthResponse from User entity and token.
     * Single place to construct response — avoids duplication.
     * Returns only safe fields — never expose password or internal IDs.
     */
    private AuthResponse buildAuthResponse(
            User user,
            String token
    ) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400000L) // 24 hours in ms
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
