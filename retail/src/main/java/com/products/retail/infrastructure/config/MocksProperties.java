package com.products.retail.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mocks")
public record MocksProperties(Base base) {

    public record Base(String url) {
    }
}
