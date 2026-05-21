package com.products.retail.config;

import com.products.retail.constant.ApiConstants;
import com.products.retail.model.ProductDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CacheConfig}.
 * <p>
 * Verifies that the Caffeine cache manager is correctly configured
 * with both named caches and that caching behavior works end-to-end.
 */
@SpringBootTest
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void cacheManagerShouldBeCaffeineInstance() {
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getClass().getName())
                .contains("CaffeineCacheManager");
    }

    @Test
    void shouldHaveSimilarIdsCacheConfigured() {
        Cache cache = cacheManager.getCache(ApiConstants.CACHE_SIMILAR_IDS);
        assertThat(cache).isNotNull();
    }

    @Test
    void shouldHaveProductDetailsCacheConfigured() {
        Cache cache = cacheManager.getCache(ApiConstants.CACHE_PRODUCT_DETAILS);
        assertThat(cache).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void similarIdsCacheStoresAndRetrievesValues() {
        Cache cache = cacheManager.getCache(ApiConstants.CACHE_SIMILAR_IDS);
        assertThat(cache).isNotNull();

        ProductDetail detail = new ProductDetail("1", "cached-product", new BigDecimal("15.00"), true);
        cache.put("test-key", List.of(detail));

        Cache.ValueWrapper wrapper = cache.get("test-key");
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.get()).isNotNull();

        List<ProductDetail> cached = (List<ProductDetail>) wrapper.get();
        assertThat(cached).hasSize(1);
        assertThat(cached.get(0).getName()).isEqualTo("cached-product");
    }

    @Test
    void productDetailsCacheStoresAndRetrievesValues() {
        Cache cache = cacheManager.getCache(ApiConstants.CACHE_PRODUCT_DETAILS);
        assertThat(cache).isNotNull();

        ProductDetail detail = new ProductDetail("5", "detail-cached", new BigDecimal("99.99"), true);
        cache.put("test-key", detail);

        Cache.ValueWrapper wrapper = cache.get("test-key");
        assertThat(wrapper).isNotNull();

        ProductDetail cached = (ProductDetail) wrapper.get();
        assertThat(cached.getName()).isEqualTo("detail-cached");
        assertThat(cached.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    void cacheMissReturnsNull() {
        Cache cache = cacheManager.getCache(ApiConstants.CACHE_SIMILAR_IDS);
        assertThat(cache).isNotNull();

        Cache.ValueWrapper wrapper = cache.get("non-existent-key");
        assertThat(wrapper).isNull();
    }
}
