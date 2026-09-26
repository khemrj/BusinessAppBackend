package com.example.firstapp.config;

import com.example.firstapp.filter.JwtAuthFilter;
import com.example.firstapp.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration — heart of security setup.
 *
 * @EnableWebSecurity — activates Spring Security web support
 * @EnableMethodSecurity — enables method-level security:
 *   @PreAuthorize("hasRole('ADMIN')")
 *   @PostAuthorize(...)
 *   @Secured(...)
 *
 * FILTER ORDER (first to last):
 * 1. RateLimitFilter — reject if too many requests
 * 2. JwtAuthFilter — validate JWT, set SecurityContext
 * 3. UsernamePasswordAuthenticationFilter — Spring default
 * 4. ExceptionTranslationFilter — convert auth exceptions
 * 5. FilterSecurityInterceptor — check rules and @PreAuthorize
 *
 * CONSTRUCTOR INJECTION via @RequiredArgsConstructor:
 * Lombok generates constructor with all final fields.
 * Preferred over @Autowired — more explicit, testable.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // All dependencies via constructor injection
    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Main Security Filter Chain.
     * Defines ALL security rules for the application.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            // ── Disable CSRF ───────────────────────────────────
            // CSRF attacks exploit browser session cookies.
            // We use stateless JWT — no sessions — no CSRF needed.
            // CORRECT to disable for REST APIs with JWT.
            .csrf(AbstractHttpConfigurer -> AbstractHttpConfigurer.disable())

            // ── Stateless Sessions ─────────────────────────────
            // NEVER create server-side sessions.
            // Every request must carry its own JWT.
            // Makes API horizontally scalable —
            // any server instance can handle any request.
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // ── URL Authorization Rules ────────────────────────
            // Rules checked IN ORDER — first match wins.
            // More specific rules BEFORE general rules.
            .authorizeHttpRequests(auth -> auth

                // PUBLIC — no JWT required
                .requestMatchers(
                    "/api/v1/auth/**",   // login, signup
                    "/api/v1/public/**", // public data
                    "/actuator/health",  // health monitoring
                    "/v3/api-docs/**",   // Swagger docs
                    "/swagger-ui/**", 
                    "/swagger-ui.html",
                    "/api/v1/opportunities" // public listing of opportunities
                ).permitAll()

                // ADMIN ONLY
                // hasRole("ADMIN") checks for "ROLE_ADMIN"
                .requestMatchers(
                    "/api/v1/admin/**"
                ).hasRole("ADMIN")

                // MANAGER AND ABOVE
                .requestMatchers(
                    "/api/v1/reports/**",
                    "/api/v1/analytics/**"
                ).hasAnyRole("ADMIN", "MANAGER")

                // STAFF AND ABOVE
                .requestMatchers(
                    "/api/v1/inventory/**"
                ).hasAnyRole("ADMIN", "MANAGER", "STAFF")

                // ALL REMAINING — just need authentication
                // Fine-grained control via @PreAuthorize
                // on individual controller methods
                .anyRequest().authenticated()
            )

            // ── Authentication Provider ────────────────────────
            .authenticationProvider(
                authenticationProvider()
            )

            // ── Add Rate Limit Filter FIRST ────────────────────
            // Runs before JWT filter
            // Rejects rate-exceeded requests early
            // Before any processing or DB calls
           
///
/// 
/// 
/// 
/// 
/// 
/// 
            // ── Add JWT Filter Before Spring's Default ─────────
            // JwtAuthFilter runs before Spring's
            // UsernamePasswordAuthenticationFilter
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
    //rate limiter runs outside of spring security filter chain, so it will run before jwt filter - if it's IP /endpoint based
    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitFilter rateLimitFilter
    ) {
       var reg = new FilterRegistrationBean<>(rateLimitFilter);
       reg.setOrder(Ordered.HIGHEST_PRECEDENCE); // run before Spring Security filters
       reg.addUrlPatterns("/*");
        return reg;
    }
    //prevent jwt filter from being auto-registered as a servlet filter, (would run twice)
    @Bean
    FilterRegistrationBean<JwtAuthFilter> disableJwtAuthFilterRegistration(JwtAuthFilter jwtAuthFilter) {
        var reg = new FilterRegistrationBean<>(jwtAuthFilter);
        reg.setEnabled(false); // Disable auto-registration
        return reg;
    }

    /**
     * Authentication Provider.
     *
     * Wires together HOW to:
     * → Load users: UserDetailsService (from MySQL)
     * → Verify passwords: BCryptPasswordEncoder
     *
     * DaoAuthenticationProvider flow:
     * 1. loadUserByUsername(email) → User from DB
     * 2. passwordEncoder.matches(rawPw, hashedPw)
     * 3. Checks isEnabled(), isAccountNonLocked() etc.
     * 4. If all pass → Authentication object returned
     * 5. If any fail → exception thrown
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider();
        // HOW to load user from database
        provider.setUserDetailsService(userDetailsService);
        // HOW to verify password against BCrypt hash
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt Password Encoder with strength 12.
     *
     * Strength 12 = 2^12 = 4096 hashing rounds.
     * Each hash takes ~400ms — acceptable for login.
     * For attackers: millions of guesses = billions of ms.
     *
     * NEVER use MD5, SHA-1, SHA-256 — they're too fast.
     * BCrypt is deliberately slow — that's the security.
     *
     * Used for:
     * → Hashing passwords during signup (AuthService)
     * → Verifying passwords during login (provider above)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication Manager.
     * Spring Security's central authentication coordinator.
     * Called by AuthService.login() to authenticate user.
     * Delegates to our authenticationProvider above.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
