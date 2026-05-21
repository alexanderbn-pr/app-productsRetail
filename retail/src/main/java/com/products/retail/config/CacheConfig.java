package com.products.retail.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for the Caffeine cache layer.
 * <p>
 * Defines a {@link CacheManager} backed by Caffeine with two named caches:
 * <ul>
 *   <li>{@code similarIds} — TTL 10 minutes, max 1 000 entries</li>
 *   <li>{@code productDetails} — TTL 5 minutes, max 10 000 entries</li>
 * </ul>
 * <p>
 * Enables Spring's {@link org.springframework.cache.annotation.Cacheable @Cacheable}
 * annotation support across the application.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /**
     * Creates the {@link CacheManager} bean with Caffeine as the backing store.
     *
     * @return configured {@link CaffeineCacheManager}
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.registerCustomCache("similarIds",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1_000)
                        .build());

        cacheManager.registerCustomCache("productDetails",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(10_000)
                        .build());

        log.info("caffeine_cache_configured caches=[similarIds(10m,1k), productDetails(5m,10k)]");
        return cacheManager;
    }
}
