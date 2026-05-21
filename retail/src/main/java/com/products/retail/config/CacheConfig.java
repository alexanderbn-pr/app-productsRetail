package com.products.retail.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.products.retail.constant.ApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @SuppressWarnings("unchecked")
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        Cache<Object, Object> similarIdsCache = Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        cacheManager.registerCustomCache(ApiConstants.CACHE_SIMILAR_IDS, similarIdsCache);

        Cache<Object, Object> productDetailsCache = Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
        cacheManager.registerCustomCache(ApiConstants.CACHE_PRODUCT_DETAILS, productDetailsCache);

        log.info("caffeine_cache_configured caches=[similarIds(10m,10k), productDetails(5m,10k)]");
        return cacheManager;
    }
}
