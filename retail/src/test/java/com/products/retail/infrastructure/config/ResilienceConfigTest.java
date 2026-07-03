package com.products.retail.infrastructure.config;

import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class ResilienceConfigTest {

    @Autowired(required = false)
    private CircuitBreakerConfigCustomizer circuitBreakerCustomizer;

    @Autowired(required = false)
    private BulkheadConfigCustomizer bulkheadCustomizer;

    @Autowired(required = false)
    private RateLimiterConfigCustomizer rateLimiterCustomizer;

    @Test
    void circuitBreakerConfigCustomizerBeanExists() {
        assertThat(circuitBreakerCustomizer).isNotNull();
    }

    @Test
    void bulkheadConfigCustomizerBeanExists() {
        assertThat(bulkheadCustomizer).isNotNull();
    }

    @Test
    void rateLimiterConfigCustomizerBeanExists() {
        assertThat(rateLimiterCustomizer).isNotNull();
    }
}
