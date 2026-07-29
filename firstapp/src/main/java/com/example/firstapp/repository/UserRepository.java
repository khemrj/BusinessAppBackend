package com.example.firstapp.repository;


import com.example.firstapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * User Repository — database operations for User entity.
 *
 * Spring Data JPA auto-generates SQL implementations:
 * findByEmail → SELECT * FROM users WHERE email = ?
 * existsByEmail → SELECT COUNT(*) > 0 FROM users WHERE email = ?
 *
 * @Modifying = used for UPDATE and DELETE queries
 * Must be used with @Transactional in service layer
 * i removed @Repository annotation because JpaRepository already has it, so it's redundant
 */
public interface UserRepository
        extends JpaRepository<User, Long> {

    // ── Lookup Methods ─────────────────────────────────────────

    // Primary lookup — used by UserDetailsService during auth
    // SQL: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Duplicate check during signup
    // SQL: SELECT COUNT(*) > 0 FROM users WHERE email = ?
    boolean existsByEmail(String email);

    // Duplicate check during signup
    boolean existsByUsername(String username);

    // ── Account Management Methods ─────────────────────────────

    /**
     * Increment failed login attempts by 1.
     * Called after every failed login attempt.
     * @Modifying = this is an UPDATE query
     */
    @Modifying
    @Query("UPDATE User u SET " +
           "u.failedAttempts = u.failedAttempts + 1 " +
           "WHERE u.email = :email")
    void incrementFailedAttempts(
            @Param("email") String email
    );

    /**
     * Reset failed attempts on successful login.
     * Also unlocks account if it was locked.
     * Called after successful authentication.
     */
    @Modifying
    @Query("UPDATE User u SET " +
           "u.failedAttempts = 0, " +
           "u.isLocked = false, " +
           "u.lockTime = null " +
           "WHERE u.email = :email")
    void resetFailedAttempts(
            @Param("email") String email
    );

    /**
     * Lock account after too many failed attempts.
     * Sets isLocked = true and records lockTime.
     * Account auto-unlocks after 30 minutes (in entity).
     */
    @Modifying
    @Query("UPDATE User u SET " +
           "u.isLocked = true, " +
           "u.lockTime = :lockTime " +
           "WHERE u.email = :email")
    void lockAccount(
            @Param("email") String email,
            @Param("lockTime") LocalDateTime lockTime
    );

    /**
     * Update last login timestamp on successful login.
     * Useful for security monitoring and audit.
     */
    @Modifying
    @Query("UPDATE User u SET " +
           "u.lastLoginAt = :loginTime " +
           "WHERE u.email = :email")
    void updateLastLoginAt(
            @Param("email") String email,
            @Param("loginTime") LocalDateTime loginTime
    );
}
