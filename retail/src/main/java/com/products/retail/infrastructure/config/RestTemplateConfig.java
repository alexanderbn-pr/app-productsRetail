package com.products.retail.infrastructure.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    private final HttpClientProperties properties;

    public RestTemplateConfig(HttpClientProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(properties.connectionPool().maxTotal());
        connectionManager.setDefaultMaxPerRoute(properties.connectionPool().maxTotal());

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(properties.timeout().connect().toSeconds()))
                .setResponseTimeout(Timeout.ofSeconds(properties.timeout().read().toSeconds()))
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        log.info("http_client_configured maxTotal={} connectTimeout={}s readTimeout={}s",
                properties.connectionPool().maxTotal(),
                properties.timeout().connect().toSeconds(),
                properties.timeout().read().toSeconds());

        return new RestTemplate(factory);
    }
}
