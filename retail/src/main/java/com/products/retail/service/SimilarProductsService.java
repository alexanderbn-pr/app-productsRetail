package com.products.retail.service;

import com.products.retail.client.ProductApiClient;
import com.products.retail.exception.ProductNotFoundException;
import com.products.retail.model.ProductDetail;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieves similar products. Pure orchestration layer — delegates
 * HTTP to {@link ProductApiClient}, resilience to annotations, and
 * errors to {@link com.products.retail.exception.GlobalExceptionHandler}.
 */
@Service
public class SimilarProductsService {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    private final ProductApiClient productApiClient;

    /**
     * Constructs the service with the required HTTP client.
     *
     * @param productApiClient the HTTP client for the external product API
     */
    public SimilarProductsService(ProductApiClient productApiClient) {
        this.productApiClient = productApiClient;
    }

    /**
     * @param productId the base product ID
     * @return similar products list (never {@code null})
     */
    @Cacheable("similarIds")
    @CircuitBreaker(name = "similarProducts", fallbackMethod = "fallbackSimilarProducts")
    @Bulkhead(name = "similarProductsBulkhead")
    @Retry(name = "similarProductsRetry", fallbackMethod = "fallbackSimilarProducts")
    public List<ProductDetail> getSimilarProducts(String productId) {
        log.info("get_similar_products productId={}", productId);
        return productApiClient.getSimilarProducts(productId);
    }

    /**
     * Resilience4j fallback — returns empty list for graceful degradation.
     */
    public List<ProductDetail> fallbackSimilarProducts(String productId, Exception ex) {
        log.warn("fallback_similar_products productId={} error={}", productId, ex.getMessage());
        return List.of();
    }
}
