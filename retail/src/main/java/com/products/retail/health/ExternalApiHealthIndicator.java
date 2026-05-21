package com.products.retail.health;

import com.products.retail.config.MocksProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiHealthIndicator.class);

    private final RestTemplate restTemplate;
    private final String mocksBaseUrl;

    public ExternalApiHealthIndicator(RestTemplate restTemplate, MocksProperties mocksProperties) {
        this.restTemplate = restTemplate;
        this.mocksBaseUrl = mocksProperties.base().url();
    }

    @Override
    public Health health() {
        try {
            String url = mocksBaseUrl + "/product/1";
            log.debug("checking_external_api_health url={}", url);
            restTemplate.getForObject(url, String.class);
            return Health.up().build();
        } catch (Exception e) {
            log.warn("external_api_down error={}", e.getMessage());
            return Health.down(e).build();
        }
    }
}
