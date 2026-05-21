package com.products.retail;

import com.products.retail.client.ProductApiClient;
import com.products.retail.constant.ApiConstants;
import com.products.retail.exception.GlobalExceptionHandler;
import com.products.retail.service.SimilarProductsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies the full Spring application context loads
 * successfully with all expected beans registered.
 * <p>
 * Validates wiring correctness: DI, auto-configuration, and custom bean
 * definitions from config classes.
 */
@SpringBootTest
class RetailApplicationTest {

    @Autowired(required = false)
    private SimilarProductsService similarProductsService;

    @Autowired(required = false)
    private ProductApiClient productApiClient;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void applicationContextLoadsWithAllCoreBeans() {
        assertThat(similarProductsService).isNotNull();
        assertThat(productApiClient).isNotNull();
        assertThat(cacheManager).isNotNull();
        assertThat(restTemplate).isNotNull();
        assertThat(globalExceptionHandler).isNotNull();
    }

    @Test
    void cacheManagerIsCaffeineBasedWithExpectedCaches() {
        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);

        assertThat(cacheManager.getCacheNames())
                .hasSize(2)
                .containsExactlyInAnyOrder(ApiConstants.CACHE_SIMILAR_IDS, ApiConstants.CACHE_PRODUCT_DETAILS);
    }
}
