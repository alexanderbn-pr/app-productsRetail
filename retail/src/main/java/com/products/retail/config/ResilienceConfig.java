package com.products.retail.config;

import com.products.retail.exception.ProductNotFoundException;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    @Bean
    public CircuitBreakerConfigCustomizer similarProductsCircuitBreakerCustomizer() {
        log.info("circuit_breaker_configured instance=similarProducts window=10 threshold=50% wait=10s halfOpen=3");
        return new CircuitBreakerConfigCustomizer() {
            @Override
            public void customize(CircuitBreakerConfig.Builder builder) {
                builder.slidingWindowSize(10)
                        .failureRateThreshold(50f)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .permittedNumberOfCallsInHalfOpenState(3);
            }

            @Override
            public String name() {
                return "similarProducts";
            }
        };
    }

    @Bean
    public BulkheadConfigCustomizer similarProductsBulkheadCustomizer() {
        log.info("bulkhead_configured instance=similarProductsBulkhead maxConcurrent=10 maxWait=500ms");
        return new BulkheadConfigCustomizer() {
            @Override
            public void customize(BulkheadConfig.Builder builder) {
                builder.maxConcurrentCalls(10)
                        .maxWaitDuration(Duration.ofMillis(500));
            }

            @Override
            public String name() {
                return "similarProductsBulkhead";
            }
        };
    }

    @Bean
    public RetryConfigCustomizer similarProductsRetryCustomizer() {
        return new RetryConfigCustomizer() {
            @Override
            public void customize(RetryConfig.Builder builder) {
                builder.ignoreExceptions(ProductNotFoundException.class);
            }

            @Override
            public String name() {
                return "similarProductsRetry";
            }
        };
    }
}
