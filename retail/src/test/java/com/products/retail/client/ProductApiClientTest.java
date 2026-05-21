package com.products.retail.client;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.products.retail.constant.ApiConstants;
import com.products.retail.exception.ProductNotFoundException;
import com.products.retail.model.ProductDetail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ProductApiClientTest {

    private static final String MOCK_BASE_URL = "http://mock";

    @Mock
    private RestTemplate restTemplate;

    private ProductApiClient productApiClient;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        // Use 3 threads: 1 for outer supplyAsync + up to 2 for inner getProductDetail futures
        executor = Executors.newFixedThreadPool(3);

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache(ApiConstants.CACHE_PRODUCT_DETAILS,
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(5, java.util.concurrent.TimeUnit.MINUTES)
                        .maximumSize(10_000)
                        .build());

        productApiClient = new ProductApiClient(restTemplate, executor, MOCK_BASE_URL, cacheManager);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void getProductDetailReturnsProductWhenFound() {
        String id = "1";
        ProductDetail expected = new ProductDetail("1", "Product 1", new BigDecimal("10.00"), true);
        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/1", ProductDetail.class))
                .thenReturn(expected);

        CompletableFuture<ProductDetail> future = productApiClient.getProductDetail(id);
        ProductDetail result = future.join();

        assertThat(result).isEqualTo(expected);
        assertThat(result.getId()).isEqualTo("1");
        assertThat(result.getName()).isEqualTo("Product 1");
        verify(restTemplate).getForObject(MOCK_BASE_URL + "/product/1", ProductDetail.class);
    }

    @Test
    void getProductDetailThrowsTimeoutWhenSlow() {
        String id = "1";
        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/1", ProductDetail.class))
                .thenAnswer(invocation -> {
                    Thread.sleep(5_000);
                    return null;
                });

        CompletableFuture<ProductDetail> future = productApiClient.getProductDetail(id);

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void getSimilarProductsReturnsListWhenAllDetailsFound() {
        String productId = "1";
        String[] similarIds = {"2", "3"};
        ProductDetail detail2 = new ProductDetail("2", "Product 2", new BigDecimal("20.00"), true);
        ProductDetail detail3 = new ProductDetail("3", "Product 3", new BigDecimal("30.00"), false);

        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/1/similarids", String[].class))
                .thenReturn(similarIds);
        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/2", ProductDetail.class))
                .thenReturn(detail2);
        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/3", ProductDetail.class))
                .thenReturn(detail3);

        List<ProductDetail> result = productApiClient.getSimilarProducts(productId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Product 2");
        assertThat(result.get(1).getName()).isEqualTo("Product 3");
        verify(restTemplate).getForObject(MOCK_BASE_URL + "/product/1/similarids", String[].class);
        verify(restTemplate).getForObject(MOCK_BASE_URL + "/product/2", ProductDetail.class);
        verify(restTemplate).getForObject(MOCK_BASE_URL + "/product/3", ProductDetail.class);
    }

    @Test
    void getSimilarProductsReturnsEmptyListWhenNoIds() {
        String productId = "1";
        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/1/similarids", String[].class))
                .thenReturn(null);

        List<ProductDetail> result = productApiClient.getSimilarProducts(productId);

        assertThat(result).isEmpty();
    }

    @Test
    void getSimilarProductsReturnsEmptyListWhenEmptyIds() {
        String productId = "1";
        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/1/similarids", String[].class))
                .thenReturn(new String[0]);

        List<ProductDetail> result = productApiClient.getSimilarProducts(productId);

        assertThat(result).isEmpty();
    }

    @Test
    void getSimilarProductsSkipsFailedDetails() {
        String productId = "1";
        String[] similarIds = {"2", "3"};
        ProductDetail detail2 = new ProductDetail("2", "Product 2", new BigDecimal("20.00"), true);

        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/1/similarids", String[].class))
                .thenReturn(similarIds);
        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/2", ProductDetail.class))
                .thenReturn(detail2);
        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/3", ProductDetail.class))
                .thenThrow(new RuntimeException("API error"));

        List<ProductDetail> result = productApiClient.getSimilarProducts(productId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("2");
    }

    @Test
    void getSimilarProductsThrowsProductNotFoundOn404() {
        String productId = "999";
        when(restTemplate.getForObject(MOCK_BASE_URL + "/product/999/similarids", String[].class))
                .thenThrow(HttpClientErrorException.NotFound.class);

        assertThatThrownBy(() -> productApiClient.getSimilarProducts(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");
    }
}
