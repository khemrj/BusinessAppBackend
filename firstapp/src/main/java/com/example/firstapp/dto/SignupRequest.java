package com.example.firstapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for POST /api/v1/auth/signup
 *
 * Flutter sends:
 * {
 *   "username": "KJ",
 *   "email": "kj@gmail.com",
 *   "password": "MyPass@123"
 * }
 *
 * All validations run BEFORE reaching service layer.
 * Prevents invalid data from even touching business logic.
 */
@Data
public class SignupRequest {

    @NotBlank(message = "Username is required")
    @Size(
        min = 3,
        max = 20,
        message = "Username must be between 3 and 20 characters"
    )
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Username can only contain letters, " +
                  "numbers, and underscores"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(
        max = 100,
        message = "Email cannot exceed 100 characters"
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Size(
        min = 8,
        max = 72,
        message = "Password must be between 8 and 72 characters"
        // 72 = BCrypt's effective maximum length
    )
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])" +
                 "(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
        message = "Password must contain: uppercase letter, " +
                  "lowercase letter, number, special character"
    )
    private String password;
}
