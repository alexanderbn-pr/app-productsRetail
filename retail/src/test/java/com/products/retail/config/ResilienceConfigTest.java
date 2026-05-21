package com.products.retail.config;

import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ResilienceConfig}.
 * <p>
 * Verifies that the CircuitBreaker and Bulkhead customizers are
 * properly registered as Spring beans.
 */
@SpringBootTest
class ResilienceConfigTest {

    @Autowired(required = false)
    private CircuitBreakerConfigCustomizer circuitBreakerCustomizer;

    @Autowired(required = false)
    private BulkheadConfigCustomizer bulkheadCustomizer;

    @Test
    void circuitBreakerConfigCustomizerBeanExists() {
        assertThat(circuitBreakerCustomizer).isNotNull();
    }

    @Test
    void bulkheadConfigCustomizerBeanExists() {
        assertThat(bulkheadCustomizer).isNotNull();
    }
}
