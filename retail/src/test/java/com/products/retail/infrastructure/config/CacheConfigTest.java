package com.products.retail.infrastructure.config;

import com.products.retail.domain.model.ProductDetail;
import com.products.retail.infrastructure.constant.InfrastructureConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


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
        Cache cache = cacheManager.getCache(InfrastructureConstants.CACHE_SIMILAR_IDS);
        assertThat(cache).isNotNull();
    }

    @Test
    void shouldHaveProductDetailsCacheConfigured() {
        Cache cache = cacheManager.getCache(InfrastructureConstants.CACHE_PRODUCT_DETAILS);
        assertThat(cache).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void similarIdsCacheStoresAndRetrievesValues() {
        Cache cache = cacheManager.getCache(InfrastructureConstants.CACHE_SIMILAR_IDS);
        assertThat(cache).isNotNull();

        ProductDetail detail = new ProductDetail("1", "cached-product", new BigDecimal("15.00"), true);
        cache.put("test-key", List.of(detail));

        Cache.ValueWrapper wrapper = cache.get("test-key");
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.get()).isNotNull();

        List<ProductDetail> cached = (List<ProductDetail>) wrapper.get();
        assertThat(cached).hasSize(1);
        assertThat(cached.get(0).name()).isEqualTo("cached-product");
    }

    @Test
    void productDetailsCacheStoresAndRetrievesValues() {
        Cache cache = cacheManager.getCache(InfrastructureConstants.CACHE_PRODUCT_DETAILS);
        assertThat(cache).isNotNull();

        ProductDetail detail = new ProductDetail("5", "detail-cached", new BigDecimal("99.99"), true);
        cache.put("test-key", detail);

        Cache.ValueWrapper wrapper = cache.get("test-key");
        assertThat(wrapper).isNotNull();

        ProductDetail cached = (ProductDetail) wrapper.get();
        assertThat(cached.name()).isEqualTo("detail-cached");
        assertThat(cached.price()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    void cacheMissReturnsNull() {
        Cache cache = cacheManager.getCache(InfrastructureConstants.CACHE_SIMILAR_IDS);
        assertThat(cache).isNotNull();

        Cache.ValueWrapper wrapper = cache.get("non-existent-key");
        assertThat(wrapper).isNull();
    }
}
