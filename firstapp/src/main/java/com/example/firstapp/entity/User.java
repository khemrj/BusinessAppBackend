package com.example.firstapp.entity;

import com.example.firstapp.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(
            name = "idx_user_email",
            columnList = "email",
            unique = true
        ),
        @Index(
            name = "idx_user_username",
            columnList = "username",
            unique = true
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public/application username.
     *
     * Example:
     * khemraj
     */
    @Column(
        name = "username",
        nullable = false,
        unique = true,
        length = 50
    )
    private String username;

    /**
     * Authentication email.
     *
     * Spring Security uses this as the UserDetails username.
     */
    @Column(
        name = "email",
        nullable = false,
        unique = true,
        length = 254
    )
    private String email;

    /**
     * BCrypt hashes are currently 60 characters,
     * but 255 gives us room if password hashing strategy
     * changes in the future.
     */
    @Column(
        name = "password",
        nullable = false,
        length = 255
    )
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "role",
        nullable = false,
        length = 30
    )
    private Role role;

    // =========================================================
    // ACCOUNT STATUS
    // =========================================================

    @Builder.Default
    @Column(
        name = "is_active",
        nullable = false
    )
    private boolean active = true;

    @Builder.Default
    @Column(
        name = "is_locked",
        nullable = false
    )
    private boolean locked = false;

    @Builder.Default
    @Column(
        name = "failed_attempts",
        nullable = false
    )
    private int failedAttempts = 0;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    // =========================================================
    // AUDIT
    // =========================================================

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // =========================================================
    // JPA CALLBACKS
    // =========================================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =========================================================
    // SPRING SECURITY
    // =========================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(
            new SimpleGrantedAuthority(role.name())
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Authentication identity = email.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Keep this method side-effect free.
     *
     * Automatic unlocking should be handled by the authentication
     * service rather than silently modifying state here.
     */
    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}