package com.products.retail.dto;

import java.time.Instant;
import java.util.Map;

/**
 * @param code    machine-readable error code (e.g. {@code "NOT_FOUND"})
 * @param message human-readable description
 * @param details optional contextual details (may be {@code null})
 */
public record ErrorResponse(String code, String message, String timestamp, Map<String, Object> details) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now().toString(), null);
    }

    public static ErrorResponse of(String code, String message, Map<String, Object> details) {
        return new ErrorResponse(code, message, Instant.now().toString(), details);
    }
}
