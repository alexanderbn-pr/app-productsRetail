package com.products.retail.client;

import com.products.retail.config.MocksProperties;
import com.products.retail.config.ThreadPoolProperties;
import com.products.retail.constant.ApiConstants;
import com.products.retail.exception.ProductNotFoundException;
import com.products.retail.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for the external product API.
 * <p>
 * Fetches product details in parallel via {@link CompletableFuture} with a
 * bounded thread pool. Individual fetches timeout after 3 seconds and
 * failures are logged and skipped so a single timeout does not collapse
 * the entire batch.
 */
@Service
public class ProductApiClient {

    private static final Logger log = LoggerFactory.getLogger(ProductApiClient.class);

    private final RestTemplate restTemplate;
    private final ExecutorService executor;
    private final String mocksBaseUrl;
    private final CacheManager cacheManager;

    @Autowired
    public ProductApiClient(
            RestTemplate restTemplate,
            CacheManager cacheManager,
            MocksProperties mocksProperties,
            ThreadPoolProperties threadPoolProperties) {
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
        this.mocksBaseUrl = mocksProperties.base().url();
        this.executor = new ThreadPoolExecutor(
                threadPoolProperties.coreSize(),
                threadPoolProperties.maxSize(),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(threadPoolProperties.queueCapacity()),
                new ThreadPoolExecutor.CallerRunsPolicy());
        log.info("product_api_client_initialized pool={}/{} queue={}",
                threadPoolProperties.coreSize(), threadPoolProperties.maxSize(),
                threadPoolProperties.queueCapacity());
    }

    /** Package-private for testing with explicit executor. */
    ProductApiClient(RestTemplate restTemplate, ExecutorService executor, String mocksBaseUrl, CacheManager cacheManager) {
        this.restTemplate = restTemplate;
        this.executor = executor;
        this.mocksBaseUrl = mocksBaseUrl;
        this.cacheManager = cacheManager;
    }

    public CompletableFuture<ProductDetail> getProductDetail(String id) {
        log.info("fetching_product_detail id={}", id);

        Cache cache = cacheManager.getCache(ApiConstants.CACHE_PRODUCT_DETAILS);
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
                .orTimeout(ApiConstants.DETAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * @param productId the base product ID
     * @return similar products (never {@code null})
     * @throws ProductNotFoundException if the upstream API returns 404
     */
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
                .map(id -> getProductDetail(id)
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
