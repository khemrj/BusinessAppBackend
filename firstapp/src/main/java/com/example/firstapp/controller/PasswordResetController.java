package com.example.firstapp.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.firstapp.dto.ForgotPasswordRequest;
import com.example.firstapp.dto.ResetPasswordRequest;
import com.example.firstapp.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService resetService;

    /** Step 1: user asks for a reset link. Always the same response (no enumeration). */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {

        resetService.requestReset(request.getEmail());

        // Identical response whether or not the email exists in the DB
        return ResponseEntity.ok(Map.of(
                "message", "If an account exists for this email, a reset link has been sent."));
    }

    /** Step 2: user submits the token from the email + a new password. */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {

        resetService.confirmReset(request.getToken(), request.getNewPassword());

        return ResponseEntity.ok(Map.of(
                "message", "Password updated successfully. Please log in."));
    }
}