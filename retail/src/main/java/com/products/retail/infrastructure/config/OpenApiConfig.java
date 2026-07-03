package com.products.retail.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Retail Products API",
                version = "1.0",
                description = "API for retrieving similar retail products with caching, resilience patterns, and observability"
        ),
        servers = @Server(url = "http://localhost:5000")
)
public class OpenApiConfig {
}
