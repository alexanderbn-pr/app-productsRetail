package com.products.retail;

import com.products.retail.application.service.SimilarProductsService;
import com.products.retail.infrastructure.adapter.outbound.ProductHttpAdapter;
import com.products.retail.infrastructure.constant.InfrastructureConstants;
import com.products.retail.infrastructure.exception.GlobalExceptionHandler;
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
    private ProductHttpAdapter productHttpAdapter;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void applicationContextLoadsWithAllCoreBeans() {
        assertThat(similarProductsService).isNotNull();
        assertThat(productHttpAdapter).isNotNull();
        assertThat(cacheManager).isNotNull();
        assertThat(restTemplate).isNotNull();
        assertThat(globalExceptionHandler).isNotNull();
    }

    @Test
    void cacheManagerIsCaffeineBasedWithExpectedCaches() {
        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);

        assertThat(cacheManager.getCacheNames())
                .hasSize(2)
                .containsExactlyInAnyOrder(InfrastructureConstants.CACHE_SIMILAR_IDS, InfrastructureConstants.CACHE_PRODUCT_DETAILS);
    }
}
