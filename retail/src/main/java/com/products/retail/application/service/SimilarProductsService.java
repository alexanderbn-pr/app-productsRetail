package com.products.retail.application.service;

import com.products.retail.application.dto.ProductDetailResponse;
import com.products.retail.application.mapper.ProductDetailMapper;
import com.products.retail.application.port.inbound.GetSimilarProductsUseCase;
import com.products.retail.application.port.outbound.ProductRepository;
import com.products.retail.domain.exception.ProductNotFoundException;
import com.products.retail.infrastructure.constant.InfrastructureConstants;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pure orchestration layer — delegates HTTP to {@link ProductRepository},
 * resilience to annotations, and errors to
 * {@link com.products.retail.infrastructure.exception.GlobalExceptionHandler}.
 */
@Service
public class SimilarProductsService implements GetSimilarProductsUseCase {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    private final ProductRepository productRepository;
    private final ProductDetailMapper productDetailMapper;

    public SimilarProductsService(ProductRepository productRepository, ProductDetailMapper productDetailMapper) {
        this.productRepository = productRepository;
        this.productDetailMapper = productDetailMapper;
    }

    @Cacheable(InfrastructureConstants.CACHE_SIMILAR_IDS)
    @CircuitBreaker(name = "similarProducts", fallbackMethod = "fallbackSimilarProducts")
    @Bulkhead(name = "similarProductsBulkhead")
    @Retry(name = "similarProductsRetry")
    @Override
    public List<ProductDetailResponse> getSimilarProducts(String productId) {
        log.info("get_similar_products productId={}", productId);
        var details = productRepository.getSimilarProducts(productId);
        return details.stream()
                .map(productDetailMapper::toProductDetailResponse)
                .toList();
    }

    /**
     * Resilience4j fallback — returns empty list for transient errors.
     * <p>
     * {@link ProductNotFoundException} is rethrown so it propagates to
     * {@link com.products.retail.infrastructure.exception.GlobalExceptionHandler GlobalExceptionHandler}
     * and produces a 404 response instead of a misleading empty 200.
     */
    public List<ProductDetailResponse> fallbackSimilarProducts(String productId, Exception ex) {
        if (ex instanceof ProductNotFoundException) {
            throw (ProductNotFoundException) ex;
        }
        log.warn("fallback_similar_products productId={} error={}", productId, ex.getMessage());
        return List.of();
    }
}
