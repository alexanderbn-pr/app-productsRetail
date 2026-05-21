package com.products.retail.service;

import com.products.retail.client.ProductApiClient;
import com.products.retail.model.ProductDetail;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for retrieving similar products.
 * <p>
 * Delegates HTTP calls to {@link ProductApiClient} for asynchronous
 * parallel fetching. Uses Spring's {@link Cacheable @Cacheable} for
 * caching similar-IDs results and Resilience4j {@link Retry @Retry}
 * for automatic retries on transient failures.
 * <p>
 * Returns {@link ResponseEntity} for backward compatibility; DTO
 * mapping will be introduced in a future PR.
 */
@Service
public class SimilarProductsService {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    private static final long FUTURE_TIMEOUT_SECONDS = 10;

    private final ProductApiClient productApiClient;

    /**
     * Constructs the service with the required HTTP client.
     *
     * @param productApiClient the async HTTP client for the external API
     */
    public SimilarProductsService(ProductApiClient productApiClient) {
        this.productApiClient = productApiClient;
    }

    /**
     * Retrieves the details of products similar to a given product.
     * <p>
     * The result is cached under the {@code "similarIds"} cache key.
     * On cache miss, delegates to {@link ProductApiClient#getSimilarProducts(String)}
     * and waits for the parallel batch to complete. Individual detail
     * failures within the batch are silently skipped.
     *
     * @param productId the base product ID
     * @return a {@link ResponseEntity} containing the list of similar products,
     *         or an error response on failure
     */
    @Cacheable("similarIds")
    @Retry(name = "similarProductsRetry", fallbackMethod = "fallbackSimilarProducts")
    public ResponseEntity<List<ProductDetail>> getSimilarProducts(String productId) {
        log.info("get_similar_products productId={}", productId);
        try {
            CompletableFuture<List<ProductDetail>> future = productApiClient.getSimilarProducts(productId);
            List<ProductDetail> products = future.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (products == null) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(products);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof HttpClientErrorException.NotFound) {
                log.warn("product_not_found productId={}", productId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            log.error("failed_fetch_similar_products productId={} error={}", productId, cause.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        } catch (TimeoutException e) {
            log.error("timeout_fetch_similar_products productId={}", productId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        } catch (Exception e) {
            log.error("failed_fetch_similar_products productId={} error={}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Fallback method invoked when all retry attempts fail.
     *
     * @param productId the base product ID
     * @param ex        the exception that caused the fallback
     * @return a 503 Service Unavailable response
     */
    public ResponseEntity<List<ProductDetail>> fallbackSimilarProducts(String productId, Exception ex) {
        log.warn("fallback_similar_products productId={} error={}", productId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
