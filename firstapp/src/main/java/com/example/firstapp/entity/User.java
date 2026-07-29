package com.example.firstapp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import  com.example.firstapp.enums.Role;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(
    name = "users",
    indexes = {
        // Index on email for fast login lookups
        // Without: full table scan on every login
        // With: O(log n) — critical at scale
        @Index(name = "idx_user_email",
               columnList = "email", unique = true),
        @Index(name = "idx_user_username",
               columnList = "username", unique = true)
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "username",
        unique = true,
        nullable = false,
        length = 50
    )
    private String username;

    // Used as the "username" in Spring Security
    // Stored in JWT "sub" claim
    @Column(
        name = "email",
        unique = true,
        nullable = false,
        length = 100
    )
    private String email;

    // ALWAYS BCrypt hashed — NEVER plain text
    // BCrypt hash length is always 60 characters
    @Column(
        name = "password",
        nullable = false,
        length = 60
    )
    private String password;

    // Stored as VARCHAR: "ROLE_ADMIN", "ROLE_STAFF" etc.
    @Enumerated(EnumType.STRING)
    @Column(
        name = "role",
        nullable = false,
        length = 20
    )
    private Role role;

    // ── Account Status Fields ──────────────────────────────────

    // false = soft deleted / banned account
    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

    // true = too many failed login attempts
    @Builder.Default
    @Column(name = "is_locked")
    private boolean isLocked = false;

    // Tracks consecutive failed logins
    // Lock account when this reaches MAX_FAILED_ATTEMPTS
    @Builder.Default
    @Column(name = "failed_attempts")
    private int failedAttempts = 0;

    // When account was locked — for auto-unlock after 30 min
    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    // ── Audit Fields ───────────────────────────────────────────

    // Set once on creation — never updated
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Updated on every save
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Track last successful login
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // ── JPA Lifecycle Callbacks ────────────────────────────────

    @PrePersist
    public void prePersist() {
        // Called automatically before first INSERT
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        // Called automatically before every UPDATE
        updatedAt = LocalDateTime.now();
    }

    // ── UserDetails Interface Implementation ───────────────────
    // Spring Security calls these methods automatically

    /**
     * Returns user's roles as Spring Security authorities.
     * @PreAuthorize("hasRole('ADMIN')") reads from here.
     * Returns: [SimpleGrantedAuthority[ROLE_ADMIN]]
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
            new SimpleGrantedAuthority(role.name())
        );
    }

    /**
     * Spring Security uses this to verify password.
     * BCryptPasswordEncoder.matches() compares
     * raw password against this BCrypt hash.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * We use EMAIL as the unique identifier (username).
     * This is what goes into JWT "sub" claim.
     * Called by UserDetailsService and during auth.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Account expiry check.
     * true = account has NOT expired (is still valid).
     * Implement account expiry logic here if needed.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Lock check with auto-unlock logic.
     * Returns true = account is NOT locked (can login).
     * Auto-unlocks after 30 minutes from lockTime.
     */
    @Override
    public boolean isAccountNonLocked() {
        // If locked, check if 30 minutes have passed
        if (isLocked && lockTime != null) {
            LocalDateTime unlockTime =
                lockTime.plusMinutes(30);
            if (LocalDateTime.now().isAfter(unlockTime)) {
                // 30 minutes passed — auto unlock
                // (actual DB update happens in AuthService)
                return true;
            }
        }
        return !isLocked;
    }

    /**
     * Credentials (password) expiry check.
     * true = credentials have NOT expired.
     * Implement password rotation policy here if needed.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Account enabled check.
     * false = Spring Security REJECTS authentication.
     * Used for soft-delete and banned accounts.
     */
    @Override
    public boolean isEnabled() {
        return isActive;
    }
}