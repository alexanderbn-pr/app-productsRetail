package com.products.retail.controller;

import com.products.retail.dto.ProductDetailResponse;
import com.products.retail.exception.ProductNotFoundException;
import com.products.retail.model.ProductDetail;
import com.products.retail.service.SimilarProductsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
 * Web-layer tests for {@link SimilarProductsController}.
 * <p>
 * Uses {@link WebMvcTest @WebMvcTest} to load only the controller and
 * its {@link com.products.retail.exception.GlobalExceptionHandler GlobalExceptionHandler}.
 * The {@link SimilarProductsService} is mocked to isolate HTTP
 * serialisation, DTO mapping, and error-response formatting.
 */
@WebMvcTest(SimilarProductsController.class)
class SimilarProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimilarProductsService similarProductsService;

    @Test
    void getSimilarProductsReturns200WithProductList() throws Exception {
        String productId = "1";
        ProductDetail detail = new ProductDetail("1", "Producto 1", new BigDecimal("10.00"), true);

        when(similarProductsService.getSimilarProducts(productId))
                .thenReturn(List.of(detail));

        mockMvc.perform(get("/product/{productId}/similar", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("Producto 1"))
                .andExpect(jsonPath("$[0].price").value(10.00))
                .andExpect(jsonPath("$[0].availability").value(true));
    }

    @Test
    void getSimilarProductsReturns200WithEmptyList() throws Exception {
        String productId = "1";

        when(similarProductsService.getSimilarProducts(productId))
                .thenReturn(List.of());

        mockMvc.perform(get("/product/{productId}/similar", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getSimilarProductsReturns404WhenProductNotFound() throws Exception {
        String productId = "notfound";

        when(similarProductsService.getSimilarProducts(productId))
                .thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(get("/product/{productId}/similar", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found: notfound"));
    }

    @Test
    void getSimilarProductsReturns500OnGenericError() throws Exception {
        String productId = "error";

        when(similarProductsService.getSimilarProducts(productId))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/product/{productId}/similar", productId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
