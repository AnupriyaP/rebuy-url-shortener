package com.rebuy.urlshortener.exception;

import com.rebuy.urlshortener.dto.UrlDtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Hash not found → 404
    @ExceptionHandler(HashNotFoundException.class)
    public ResponseEntity<UrlDtos.ErrorResponse> handleHashNotFound(
            HashNotFoundException ex) {
        log.warn("Hash not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new UrlDtos.ErrorResponse(
                        "NOT_FOUND", ex.getMessage(), 404));
    }

    // Business logic error → 400
    @ExceptionHandler(UrlShortenerException.class)
    public ResponseEntity<UrlDtos.ErrorResponse> handleUrlShortener(
            UrlShortenerException ex) {
        log.error("URL shortener business error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new UrlDtos.ErrorResponse(
                        "BAD_REQUEST", ex.getMessage(), 400));
    }

    // Rate limit exceeded → 429
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<UrlDtos.ErrorResponse> handleRateLimit(
            RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new UrlDtos.ErrorResponse(
                        "TOO_MANY_REQUESTS", ex.getMessage(), 429));
    }

    // Validation errors → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UrlDtos.ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.debug("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new UrlDtos.ErrorResponse(
                        "VALIDATION_ERROR", message, 400));
    }

    // Anything else → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<UrlDtos.ErrorResponse> handleGeneric(
            Exception ex) {
        // CRITICAL: log full stack trace — this is your only trace
        // of unanticipated production bugs. Without this, a real
        // NullPointerException or DB failure becomes invisible.
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UrlDtos.ErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred", 500));
    }
}