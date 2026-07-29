package com.example.firstapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Standard response wrapper for ALL API endpoints.
 *
 * Flutter ALWAYS receives this structure — no surprises.
 *
 * Success: {"success":true, "message":"...", "data":{...}}
 * Error:   {"success":false, "message":"...", "data":null}
 *
 * @JsonInclude(NON_NULL) = omit null fields from JSON
 * Keeps response clean when data is not needed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Factory Methods ────────────────────────────────────────

    /** Success response with data (login, signup, profile) */
    public static <T> ApiResponse<T> success(
            String message, T data
    ) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /** Success response without data (logout, delete) */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /** Error response */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
