package com.products.retail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mocks")
public record MocksProperties(Base base) {

    public record Base(String url) {
    }
}
