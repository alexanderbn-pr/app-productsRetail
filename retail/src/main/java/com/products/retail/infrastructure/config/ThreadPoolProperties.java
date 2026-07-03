package com.products.retail.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.thread-pool")
public record ThreadPoolProperties(int coreSize, int maxSize, int queueCapacity) {
}
