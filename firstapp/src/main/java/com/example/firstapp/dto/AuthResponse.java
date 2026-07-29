package com.example.firstapp.dto;

import lombok.*;

/**
 * Returned to Flutter after successful login or signup.
 * Contains JWT token and user information.
 *
 * Flutter:
 * 1. Stores token in FlutterSecureStorage
 * 2. Sends token in every subsequent request:
 *    Authorization: Bearer <token>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    // JWT token — Flutter stores this securely
    private String token;

    // Always "Bearer" — Flutter prepends to token in header
    @Builder.Default
    private String tokenType = "Bearer";

    // Token validity period in milliseconds
    private long expiresIn;

    // User info — avoids separate profile API call after login
    private Long userId;
    private String username;
    private String email;
    private String role;
}