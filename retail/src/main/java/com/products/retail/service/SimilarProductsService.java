package com.products.retail.service;

import com.products.retail.client.ProductApiClient;
import com.products.retail.constant.ApiConstants;
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
 * Pure orchestration layer — delegates HTTP to {@link ProductApiClient},
 * resilience to annotations, and errors to
 * {@link com.products.retail.exception.GlobalExceptionHandler}.
 */
@Service
public class SimilarProductsService {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    private final ProductApiClient productApiClient;

    public SimilarProductsService(ProductApiClient productApiClient) {
        this.productApiClient = productApiClient;
    }

    @Cacheable(ApiConstants.CACHE_SIMILAR_IDS)
    @CircuitBreaker(name = "similarProducts", fallbackMethod = "fallbackSimilarProducts")
    @Bulkhead(name = "similarProductsBulkhead")
    @Retry(name = "similarProductsRetry")
    public List<ProductDetail> getSimilarProducts(String productId) {
        log.info("get_similar_products productId={}", productId);
        return productApiClient.getSimilarProducts(productId);
    }

    /**
     * Resilience4j fallback — returns empty list for transient errors.
     * <p>
     * {@link ProductNotFoundException} is rethrown so it propagates to
     * {@link com.products.retail.exception.GlobalExceptionHandler GlobalExceptionHandler}
     * and produces a 404 response instead of a misleading empty 200.
     */
    public List<ProductDetail> fallbackSimilarProducts(String productId, Exception ex) {
        if (ex instanceof ProductNotFoundException) {
            throw (ProductNotFoundException) ex;
        }
        log.warn("fallback_similar_products productId={} error={}", productId, ex.getMessage());
        return List.of();
    }
}
