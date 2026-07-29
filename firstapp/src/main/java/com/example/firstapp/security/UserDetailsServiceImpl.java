package com.example.firstapp.security;

import com.example.firstapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Bridge between Spring Security and our User database.
 *
 * Spring Security calls loadUserByUsername() in TWO situations:
 * 1. During login — AuthenticationManager uses this to load
 *    user then calls BCrypt.matches() for password verification
 * 2. During JwtAuthFilter — to load user for token validation
 *    and to build the Authentication object
 *
 * Our User entity implements UserDetails directly,
 * so we return it without any conversion needed.
 *
 * CONSTRUCTOR INJECTION via @RequiredArgsConstructor:
 * Lombok generates constructor with all final fields.
 * This is the recommended approach — no @Autowired needed.
 * Makes dependencies explicit and enables easy testing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl
        implements UserDetailsService {

    // Constructor injection — final ensures immutability
    private final UserRepository userRepository;

    /**
     * Loads user from database by email (our "username").
     *
     * Spring Security calls this and then:
     * → getPassword() — for BCrypt comparison
     * → isEnabled() — check account active
     * → isAccountNonLocked() — check not locked
     * → getAuthorities() — get roles for authorization
     *
     * SECURITY: UsernameNotFoundException is converted
     * to BadCredentialsException by Spring Security.
     * Never reveals if the EMAIL specifically was wrong.
     * Attacker cannot distinguish "wrong email" vs "wrong password".
     *
     * @param email The email address used as username
     * @return User entity (implements UserDetails)
     * @throws UsernameNotFoundException if email not in DB
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        log.debug("Loading user for authentication: {}",
                email);

        // SQL: SELECT * FROM users WHERE email = ?
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new UsernameNotFoundException(
                        "User not found: " + email
                    );
                });
        // Returns User entity — Spring Security reads
        // getPassword(), isEnabled(), getAuthorities() etc.
    }
}