package com.products.retail.exception;

import com.products.retail.dto.ErrorResponse;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * <p>
 * Uses MockMvc standalone setup with a test controller that throws
 * each handled exception type to verify HTTP status mapping and
 * {@link ErrorResponse} body format.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void productNotFoundExceptionReturns404() throws Exception {
        mockMvc.perform(get("/test/product-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found: missing-id"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void illegalArgumentExceptionReturns400() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid product ID"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void httpClientErrorNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/test/http-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void callNotPermittedExceptionReturns503() throws Exception {
        mockMvc.perform(get("/test/circuit-breaker"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void bulkheadFullExceptionReturns429() throws Exception {
        mockMvc.perform(get("/test/bulkhead-full"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void genericExceptionReturns500() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * Test controller that throws each handled exception type on specific endpoints.
     */
    @RestController
    static class ExceptionThrowingController {

        @GetMapping("/test/product-not-found")
        void throwProductNotFound() {
            throw new ProductNotFoundException("missing-id");
        }

        @GetMapping("/test/illegal-argument")
        void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid product ID");
        }

        @GetMapping("/test/http-not-found")
        void throwHttpNotFound() {
            throw HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", null, null, null);
        }

        @GetMapping("/test/circuit-breaker")
        void throwCircuitBreakerOpen() {
            throw CallNotPermittedException.createCallNotPermittedException(
                    CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults()));
        }

        @GetMapping("/test/bulkhead-full")
        void throwBulkheadFull() {
            throw BulkheadFullException.createBulkheadFullException(
                    Bulkhead.of("test", BulkheadConfig.ofDefaults()));
        }

        @GetMapping("/test/generic-error")
        void throwGeneric() {
            throw new RuntimeException("Unexpected internal error");
        }
    }
}
