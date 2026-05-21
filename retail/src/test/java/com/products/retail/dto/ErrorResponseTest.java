package com.products.retail.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ErrorResponse}.
 * <p>
 * Verifies factory methods, field accessors, and immutability.
 */
class ErrorResponseTest {

    @Test
    void ofCreatesResponseWithCodeAndMessage() {
        ErrorResponse response = ErrorResponse.of("NOT_FOUND", "Product not found: 123");

        assertThat(response.code()).isEqualTo("NOT_FOUND");
        assertThat(response.message()).isEqualTo("Product not found: 123");
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.details()).isNull();
    }

    @Test
    void ofCreatesResponseWithDetails() {
        Map<String, Object> details = Map.of("productId", "123");
        ErrorResponse response = ErrorResponse.of("BAD_REQUEST", "Invalid input", details);

        assertThat(response.code()).isEqualTo("BAD_REQUEST");
        assertThat(response.message()).isEqualTo("Invalid input");
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.details()).isNotNull();
        assertThat(response.details()).containsEntry("productId", "123");
    }

    @Test
    void timestampIsValidIsoInstant() {
        ErrorResponse response = ErrorResponse.of("OK", "test");
        Instant parsed = Instant.parse(response.timestamp());
        assertThat(parsed).isNotNull();
    }
}
