package com.products.retail.infrastructure.adapter.inbound;

import com.products.retail.application.constant.ApplicationConstants;
import com.products.retail.application.dto.ProductDetailResponse;
import com.products.retail.application.port.inbound.GetSimilarProductsUseCase;
import com.products.retail.domain.exception.ProductNotFoundException;
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


@WebMvcTest(SimilarProductsController.class)
class SimilarProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetSimilarProductsUseCase getSimilarProductsUseCase;

    @Test
    void getSimilarProductsReturns200WithProductList() throws Exception {
        String productId = "1";
        ProductDetailResponse response = new ProductDetailResponse("1", "Producto 1", new BigDecimal("10.00"), true);

        when(getSimilarProductsUseCase.getSimilarProducts(productId))
                .thenReturn(List.of(response));

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

        when(getSimilarProductsUseCase.getSimilarProducts(productId))
                .thenReturn(List.of());

        mockMvc.perform(get("/product/{productId}/similar", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getSimilarProductsReturns404WhenProductNotFound() throws Exception {
        String productId = "notfound";

        when(getSimilarProductsUseCase.getSimilarProducts(productId))
                .thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(get("/product/{productId}/similar", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(ApplicationConstants.ERROR_PRODUCT_NOT_FOUND))
                .andExpect(jsonPath("$.detail").value("Product not found: notfound"));
    }

    @Test
    void getSimilarProductsReturns500OnGenericError() throws Exception {
        String productId = "error";

        when(getSimilarProductsUseCase.getSimilarProducts(productId))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/product/{productId}/similar", productId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value(ApplicationConstants.ERROR_INTERNAL))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }
}
