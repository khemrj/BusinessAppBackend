package com.example.firstapp.handler;

import com.example.firstapp.dto.ApiResponse;
import com.example.firstapp.exception.BadRequestException;
import com.example.firstapp.exception.EmailAlreadyExistsException;
import com.example.firstapp.exception.ResourceNotFoundException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access
        .AccessDeniedException;
import org.springframework.security.authentication
        .BadCredentialsException;
import org.springframework.security.core
        .AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind
        .MethodArgumentNotValidException;
import org.springframework.web.bind.annotation
        .ExceptionHandler;
import org.springframework.web.bind.annotation
        .RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler — catches ALL exceptions.
 *
 * WITHOUT this: Flutter gets Spring's raw error pages
 * WITH this: Flutter ALWAYS gets structured ApiResponse JSON
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * Applies to ALL controllers automatically.
 *
 * SECURITY PRINCIPLE:
 * → Log full details SERVER-SIDE (for developers)
 * → Send safe, minimal messages to CLIENT (for users)
 * → Never expose stack traces, SQL errors, class names
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * @Valid Bean Validation failures.
     * Returns field-specific errors so Flutter can
     * highlight exactly which field failed.
     * HTTP 400
     *
     * Flutter receives:
     * {
     *   "success": false,
     *   "message": "Validation failed",
     *   "fieldErrors": {
     *     "email": "Enter a valid email address",
     *     "password": "Must contain uppercase..."
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
            handleValidationErrors(
                MethodArgumentNotValidException ex
            ) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors()
                .forEach(error -> {
                    String field =
                        ((FieldError) error).getField();
                    String message =
                        error.getDefaultMessage();
                    fieldErrors.put(field, message);
                });

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("fieldErrors", fieldErrors);

        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity
                .badRequest() // 400
                .body(response);
    }

    /**
     * Duplicate email during signup.
     * HTTP 409 Conflict
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleEmailExists(
                EmailAlreadyExistsException ex
            ) {
        log.warn("Email conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Entity not found in database.
     * HTTP 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleNotFound(
                ResourceNotFoundException ex
            ) {
        log.warn("Not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // 404
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * General bad request (invalid input, business rule).
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleBadRequest(
                BadRequestException ex
            ) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .badRequest() // 400
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * JWT token has expired.
     * Flutter should redirect to login on receiving this.
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleExpiredJwt(
                ExpiredJwtException ex
            ) {
        log.warn("JWT expired: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(ApiResponse.error(
                    "Session expired. Please login again."
                ));
    }

    /**
     * JWT invalid (malformed, wrong signature, tampered).
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleJwtException(
                JwtException ex
            ) {
        log.warn("JWT invalid: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(ApiResponse.error(
                    "Invalid authentication token."
                ));
    }

    /**
     * Wrong credentials during login.
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleBadCredentials(
                BadCredentialsException ex
            ) {
        // Don't log actual message — may contain credential info
        log.warn("Bad credentials attempt");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(ApiResponse.error(
                    "Invalid email or password."
                ));
    }

    /**
     * Not authenticated (missing or invalid JWT).
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleAuthException(
                AuthenticationException ex
            ) {
        log.warn("Authentication failed: {}",
                ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(ApiResponse.error(
                    "Authentication required. " +
                    "Please login."
                ));
    }

    /**
     * @PreAuthorize failed — authenticated but wrong role.
     * HTTP 403 Forbidden
     *
     * Difference from 401:
     * 401 = you didn't prove who you are
     * 403 = we know who you are but you can't do this
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleAccessDenied(
                AccessDeniedException ex
            ) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403
                .body(ApiResponse.error(
                    "You don't have permission to " +
                    "perform this action."
                ));
    }

    /**
     * Catch-all — any unhandled exception.
     * NEVER expose internal details to client.
     * Log full stack trace server-side ONLY.
     * HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>>
            handleAllExceptions(
                Exception ex,
                HttpServletRequest request
            ) {
        // Full details in server logs (for developers)
        log.error("Unexpected error at {}: {}",
                request.getRequestURI(),
                ex.getMessage(), ex);

        // Safe, generic message to client (for users)
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    "Something went wrong. " +
                    "Please try again later."
                ));
    }
}