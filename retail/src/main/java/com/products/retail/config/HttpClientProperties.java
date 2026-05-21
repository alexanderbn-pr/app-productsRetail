package com.products.retail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "httpclient")
public record HttpClientProperties(ConnectionPool connectionPool, Timeout timeout) {

    public record ConnectionPool(int maxTotal) {
    }

    public record Timeout(Duration connect, Duration read) {
    }
}
