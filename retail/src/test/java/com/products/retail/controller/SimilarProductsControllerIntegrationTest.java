package com.products.retail.controller;

import com.products.retail.client.ProductApiClient;
import com.products.retail.health.ExternalApiHealthIndicator;
import com.products.retail.model.ProductDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-context integration test for {@link SimilarProductsController}.
 * <p>
 * Loads the entire Spring application context but mocks the external
 * {@link ProductApiClient} to avoid real HTTP calls. Verifies that:
 * <ul>
 *   <li>the product endpoint returns correct JSON structure</li>
 *   <li>actuator health reports UP</li>
 *   <li>Swagger UI redirects to the API docs page</li>
 *   <li>OpenAPI /v3/api-docs returns a valid spec document</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SimilarProductsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductApiClient productApiClient;

    @MockBean
    private ExternalApiHealthIndicator externalApiHealthIndicator;

    @Test
    void getSimilarProductsReturns200WithValidJsonStructure() throws Exception {
        String productId = "1";
        ProductDetail detail = new ProductDetail("1", "Producto 1", new BigDecimal("10.00"), true);

        when(productApiClient.getSimilarProducts(productId))
                .thenReturn(List.of(detail));

        mockMvc.perform(get("/product/{productId}/similar", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("Producto 1"))
                .andExpect(jsonPath("$[0].price").value(10.00))
                .andExpect(jsonPath("$[0].availability").value(true));
    }

    @Test
    void getSimilarProductsReturns200WithMultipleItems() throws Exception {
        String productId = "2";
        ProductDetail detail2 = new ProductDetail("2", "Producto 2", new BigDecimal("20.00"), true);
        ProductDetail detail3 = new ProductDetail("3", "Producto 3", new BigDecimal("30.00"), false);

        when(productApiClient.getSimilarProducts(productId))
                .thenReturn(List.of(detail2, detail3));

        mockMvc.perform(get("/product/{productId}/similar", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("2"))
                .andExpect(jsonPath("$[0].name").value("Producto 2"))
                .andExpect(jsonPath("$[1].id").value("3"))
                .andExpect(jsonPath("$[1].name").value("Producto 3"))
                .andExpect(jsonPath("$[1].availability").value(false));
    }

    @Test
    void actuatorHealthEndpointReturns200() throws Exception {
        when(externalApiHealthIndicator.health()).thenReturn(Health.up().build());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void swaggerUiRedirectsToApiDocsPage() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void apiDocsEndpointReturnsOpenApiSpec() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isString())
                .andExpect(jsonPath("$.info.title").value("Retail Products API"))
                .andExpect(jsonPath("$.info.version").value("1.0"));
    }
}
