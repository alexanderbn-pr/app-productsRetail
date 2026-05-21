package com.products.retail.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    private final int maxTotal;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;

    public RestTemplateConfig(
            @Value("${httpclient.connection-pool.max-total:20}") int maxTotal,
            @Value("${httpclient.timeout.connect:2s}") String connectTimeout,
            @Value("${httpclient.timeout.read:5s}") String readTimeout) {
        this.maxTotal = maxTotal;
        this.connectTimeoutSeconds = parseSeconds(connectTimeout);
        this.readTimeoutSeconds = parseSeconds(readTimeout);
    }

    /**
     * Creates the primary {@link RestTemplate} bean with HC5 connection pooling.
     *
     * @return configured {@link RestTemplate} instance
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotal);
        connectionManager.setDefaultMaxPerRoute(maxTotal);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(connectTimeoutSeconds))
                .setResponseTimeout(Timeout.ofSeconds(readTimeoutSeconds))
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        log.info("http_client_configured maxTotal={} connectTimeout={}s readTimeout={}s",
                maxTotal, connectTimeoutSeconds, readTimeoutSeconds);

        return new RestTemplate(factory);
    }

    /** Parses {@code "2s"} or {@code "500ms"} into seconds (min 1). */
    private static int parseSeconds(String duration) {
        duration = duration.trim().toLowerCase();
        if (duration.endsWith("ms")) {
            long millis = Long.parseLong(duration.replace("ms", ""));
            return (int) Math.max(1, millis / 1000);
        } else if (duration.endsWith("s")) {
            return Integer.parseInt(duration.replace("s", ""));
        }
        return Integer.parseInt(duration);
    }
}
