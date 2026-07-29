package com.example.firstapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for POST /api/v1/auth/login
 *
 * Flutter sends:
 * {
 *   "email": "kj@gmail.com",
 *   "password": "MyPass@123"
 * }
 *
 * @Valid on controller triggers these validations.
 * If any fail → MethodArgumentNotValidException
 * → GlobalExceptionHandler → 400 with field errors
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 100, message = "Invalid password")
    // No complex validation on login password
    // Don't leak password policy to attackers
    private String password;
}
