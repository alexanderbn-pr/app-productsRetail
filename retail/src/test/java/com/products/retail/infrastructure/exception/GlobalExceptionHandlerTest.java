package com.products.retail.infrastructure.exception;

import com.products.retail.application.constant.ApplicationConstants;
import com.products.retail.domain.exception.ProductNotFoundException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 * <p>
 * Uses MockMvc standalone setup with a test controller that throws
 * each handled exception type to verify HTTP status mapping and
 * {@link org.springframework.http.ProblemDetail ProblemDetail} body format.
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
                .andExpect(jsonPath("$.title").value(ApplicationConstants.ERROR_PRODUCT_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value("Product not found: missing-id"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void illegalArgumentExceptionReturns400() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(ApplicationConstants.ERROR_BAD_REQUEST))
                .andExpect(jsonPath("$.detail").value("Invalid product ID"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void httpClientErrorNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/test/http-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(ApplicationConstants.ERROR_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value("404 Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void callNotPermittedExceptionReturns503() throws Exception {
        mockMvc.perform(get("/test/circuit-breaker"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value(ApplicationConstants.ERROR_SERVICE_UNAVAILABLE))
                .andExpect(jsonPath("$.detail").value("Service temporarily unavailable"))
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void bulkheadFullExceptionReturns429() throws Exception {
        mockMvc.perform(get("/test/bulkhead-full"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value(ApplicationConstants.ERROR_TOO_MANY_REQUESTS))
                .andExpect(jsonPath("$.detail").value("Too many concurrent requests"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void requestNotPermittedExceptionReturns429() throws Exception {
        mockMvc.perform(get("/test/rate-limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value(ApplicationConstants.ERROR_RATE_LIMITED))
                .andExpect(jsonPath("$.detail").value("Rate limit exceeded. Please try again later."))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void genericExceptionReturns500() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value(ApplicationConstants.ERROR_INTERNAL))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.status").value(500));
    }

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

        @GetMapping("/test/rate-limited")
        void throwRateLimited() {
            throw RequestNotPermitted.createRequestNotPermitted(
                    RateLimiter.ofDefaults("test"));
        }

        @GetMapping("/test/generic-error")
        void throwGeneric() {
            throw new RuntimeException("Unexpected internal error");
        }
    }
}
