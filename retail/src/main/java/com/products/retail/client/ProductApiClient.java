package com.products.retail.client;

import com.products.retail.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
 * Provides asynchronous methods for fetching product details and
 * similar product lists using {@link CompletableFuture} with a bounded
 * thread pool. Individual detail fetches timeout after 3 seconds;
 * the entire similar-products batch timeout is 5 seconds.
 * <p>
 * Failed individual detail fetches are logged and skipped (returning
 * {@code null}) so that a partial failure does not collapse the entire
 * similar-products response.
 */
@Service
public class ProductApiClient {

    private static final Logger log = LoggerFactory.getLogger(ProductApiClient.class);

    private static final long DETAIL_TIMEOUT_SECONDS = 3;
    private static final long BATCH_TIMEOUT_SECONDS = 5;

    private final RestTemplate restTemplate;
    private final ExecutorService executor;
    private final String mocksBaseUrl;

    /**
     * Constructs the client with Spring-managed dependencies.
     * Creates a bounded thread pool from application properties.
     *
     * @param restTemplate   the HTTP client
     * @param mocksBaseUrl   base URL of the external product API
     * @param corePoolSize   core thread pool size
     * @param maxPoolSize    maximum thread pool size
     * @param queueCapacity  capacity of the work queue
     */
    @Autowired
    public ProductApiClient(
            RestTemplate restTemplate,
            @Value("${mocks.base.url}") String mocksBaseUrl,
            @Value("${app.thread-pool.core-size:5}") int corePoolSize,
            @Value("${app.thread-pool.max-size:10}") int maxPoolSize,
            @Value("${app.thread-pool.queue-capacity:50}") int queueCapacity) {
        this.restTemplate = restTemplate;
        this.mocksBaseUrl = mocksBaseUrl;
        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy());
        log.info("product_api_client_initialized pool={}/{} queue={}",
                corePoolSize, maxPoolSize, queueCapacity);
    }

    /**
     * Package-private constructor for testing that accepts an explicit executor.
     *
     * @param restTemplate the HTTP client
     * @param executor     the executor for async operations
     * @param mocksBaseUrl base URL of the external product API
     */
    ProductApiClient(RestTemplate restTemplate, ExecutorService executor, String mocksBaseUrl) {
        this.restTemplate = restTemplate;
        this.executor = executor;
        this.mocksBaseUrl = mocksBaseUrl;
    }

    /**
     * Fetches the detail of a single product by its ID asynchronously.
     * <p>
     * The returned future completes exceptionally with a
     * {@link java.util.concurrent.TimeoutException} if the HTTP call
     * does not complete within 3 seconds.
     *
     * @param id the product ID
     * @return a future that completes with the {@link ProductDetail}
     */
    public CompletableFuture<ProductDetail> getProductDetail(String id) {
        log.info("fetching_product_detail id={}", id);
        return CompletableFuture
                .supplyAsync(() -> {
                    String url = mocksBaseUrl + "/product/" + id;
                    log.debug("calling_product_api url={}", url);
                    return restTemplate.getForObject(url, ProductDetail.class);
                }, executor)
                .orTimeout(DETAIL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Fetches the list of similar product details for a given product ID.
     * <p>
     * First obtains the array of similar IDs from the API, then fans out
     * parallel detail fetches via {@link #getProductDetail(String)}.
     * Failed individual fetches are skipped.
     *
     * @param productId the base product ID
     * @return a future that completes with the list of {@link ProductDetail}
     */
    public CompletableFuture<List<ProductDetail>> getSimilarProducts(String productId) {
        log.info("fetching_similar_products productId={}", productId);
        return CompletableFuture
                .supplyAsync(() -> {
                    String idsUrl = mocksBaseUrl + "/product/" + productId + "/similarids";
                    log.debug("calling_product_api url={}", idsUrl);
                    String[] similarIds = restTemplate.getForObject(idsUrl, String[].class);

                    if (similarIds == null || similarIds.length == 0) {
                        log.info("no_similar_ids_found productId={}", productId);
                        return List.<ProductDetail>of();
                    }

                    log.info("fetched_similar_ids productId={} count={}", productId, similarIds.length);
                    return fetchAllDetailsInParallel(similarIds);
                }, executor)
                .orTimeout(BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Fetches all product details in parallel using {@link CompletableFuture#allOf}.
     * <p>
     * Each fetch is wrapped with an {@code exceptionally} handler so that
     * individual failures produce {@code null} instead of collapsing the batch.
     *
     * @param ids the product IDs to fetch
     * @return the list of successfully fetched details (never {@code null})
     */
    private List<ProductDetail> fetchAllDetailsInParallel(String[] ids) {
        List<CompletableFuture<ProductDetail>> futures = Arrays.stream(ids)
                .map(id -> getProductDetail(id)
                        .exceptionally(ex -> {
                            log.warn("failed_fetch_detail id={} error={}", id, ex.getMessage());
                            return null;
                        }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }
}
