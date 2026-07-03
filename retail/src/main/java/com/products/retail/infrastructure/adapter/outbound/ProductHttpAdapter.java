package com.products.retail.infrastructure.adapter.outbound;

import com.products.retail.application.port.outbound.ProductRepository;
import com.products.retail.infrastructure.config.MocksProperties;
import com.products.retail.domain.exception.ProductNotFoundException;
import com.products.retail.domain.model.ProductDetail;
import com.products.retail.infrastructure.constant.InfrastructureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HTTP adapter for the external product API — implements {@link ProductRepository}.
 * <p>
 * Fetches product details in parallel via {@link CompletableFuture} with a
 * bounded thread pool. Individual fetches timeout after 3 seconds and
 * failures are logged and skipped so a single timeout does not collapse
 * the entire batch.
 */
@Service
public class ProductHttpAdapter implements ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductHttpAdapter.class);

    private final RestTemplate restTemplate;
    private final ExecutorService executor;
    private final String mocksBaseUrl;
    private final CacheManager cacheManager;

    @Autowired
    public ProductHttpAdapter(
            RestTemplate restTemplate,
            CacheManager cacheManager,
            MocksProperties mocksProperties,
            @Qualifier("productDetailExecutor") ExecutorService productDetailExecutor) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
        this.mocksBaseUrl = mocksProperties.base().url();
        this.executor = productDetailExecutor;
        log.info("product_http_adapter_initialized");
    }

    /** Package-private for testing with explicit executor. */
    ProductHttpAdapter(RestTemplate restTemplate, ExecutorService executor, String mocksBaseUrl, CacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.executor = executor;
        this.mocksBaseUrl = mocksBaseUrl;
        this.cacheManager = cacheManager;
    }

    @Override
    public ProductDetail getProductDetail(String id) {
        return getProductDetailAsync(id).join();
    }

    private CompletableFuture<ProductDetail> getProductDetailAsync(String id) {
        log.info("fetching_product_detail id={}", id);

        Cache cache = cacheManager.getCache(InfrastructureConstants.CACHE_PRODUCT_DETAILS);
        ProductDetail cached = cache.get(id, ProductDetail.class);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture
                .supplyAsync(() -> {
                    String url = mocksBaseUrl + "/product/" + id;
                    log.debug("calling_product_api url={}", url);
                    ProductDetail detail = restTemplate.getForObject(url, ProductDetail.class);
                    if (detail != null) {
                        cache.put(id, detail);
                    }
                    return detail;
                }, executor)
                .orTimeout(InfrastructureConstants.DETAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * @param productId the base product ID
     * @return similar products (never {@code null})
     * @throws ProductNotFoundException if the upstream API returns 404
     */
    @Override
    public List<ProductDetail> getSimilarProducts(String productId) {
        log.info("fetching_similar_products productId={}", productId);
        try {
            String idsUrl = mocksBaseUrl + "/product/" + productId + "/similarids";
            log.debug("calling_product_api url={}", idsUrl);
            String[] similarIds = restTemplate.getForObject(idsUrl, String[].class);

            if (similarIds == null || similarIds.length == 0) {
                log.info("no_similar_ids_found productId={}", productId);
                return List.of();
            }

            log.info("fetched_similar_ids productId={} count={}", productId, similarIds.length);
            return fetchAllDetailsInParallel(similarIds);

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("product_not_found productId={}", productId);
            throw new ProductNotFoundException(productId);
        }
    }

    private List<ProductDetail> fetchAllDetailsInParallel(String[] ids) {
        List<CompletableFuture<ProductDetail>> futures = Arrays.stream(ids)
                .map(id -> getProductDetailAsync(id)
                        .exceptionally(ex -> {
                            log.warn("failed_fetch_detail id={} error={}", id, ex.getMessage());
                            return null;
                        }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
                .join();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }
}
