package com.products.retail.service;

import com.products.retail.client.ProductApiClient;
import com.products.retail.exception.ProductNotFoundException;
import com.products.retail.model.ProductDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SimilarProductsService}.
 * <p>
 * Verifies delegation to {@link ProductApiClient}, exception propagation,
 * and fallback behavior. Cache behavior is tested via integration tests.
 */
@ExtendWith(MockitoExtension.class)
class SimilarProductsServiceTest {

    @Mock
    private ProductApiClient productApiClient;

    @InjectMocks
    private SimilarProductsService similarProductsService;

    @Test
    void getSimilarProductsReturnsListWithProductDetails() {
        String productId = "1";
        ProductDetail product2 = new ProductDetail("2", "Producto 2", new BigDecimal("20.00"), true);
        ProductDetail product3 = new ProductDetail("3", "Producto 3", new BigDecimal("30.00"), false);

        when(productApiClient.getSimilarProducts(productId)).thenReturn(List.of(product2, product3));

        List<ProductDetail> result = similarProductsService.getSimilarProducts(productId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Producto 2");
        assertThat(result.get(1).getName()).isEqualTo("Producto 3");
    }

    @Test
    void getSimilarProductsReturnsEmptyListWhenNoSimilarIds() {
        String productId = "1";

        when(productApiClient.getSimilarProducts(productId)).thenReturn(List.of());

        List<ProductDetail> result = similarProductsService.getSimilarProducts(productId);

        assertThat(result).isEmpty();
    }

    @Test
    void getSimilarProductsThrowsProductNotFoundExceptionWhenClientReturns404() {
        String productId = "notfound";

        when(productApiClient.getSimilarProducts(productId))
                .thenThrow(new ProductNotFoundException(productId));

        assertThatThrownBy(() -> similarProductsService.getSimilarProducts(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(productId);
    }

    @Test
    void getSimilarProductsThrowsRuntimeExceptionOnGenericFailure() {
        String productId = "error";

        when(productApiClient.getSimilarProducts(productId))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThatThrownBy(() -> similarProductsService.getSimilarProducts(productId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void fallbackSimilarProductsReturnsEmptyList() {
        List<ProductDetail> result = similarProductsService
                .fallbackSimilarProducts("1", new Exception("Service down"));

        assertThat(result).isEmpty();
    }
}
