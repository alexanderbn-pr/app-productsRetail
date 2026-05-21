package com.products.retail.service;

import com.products.retail.client.ProductApiClient;
import com.products.retail.model.ProductDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Unit tests for {@link SimilarProductsService}.
 * <p>
 * Verifies delegation to {@link ProductApiClient}, fallback behavior,
 * and response mapping. Cache and retry behavior are tested via integration tests.
 */
@ExtendWith(MockitoExtension.class)
class SimilarProductsServiceTest {

    @Mock
    private ProductApiClient productApiClient;

    @InjectMocks
    private SimilarProductsService similarProductsService;

    @Test
    void getSimilarProductsReturnsOkWithProductList() {
        String productId = "1";
        ProductDetail product2 = new ProductDetail("2", "Producto 2", 20.0, true);
        ProductDetail product3 = new ProductDetail("3", "Producto 3", 30.0, false);

        when(productApiClient.getSimilarProducts(productId))
                .thenReturn(CompletableFuture.completedFuture(List.of(product2, product3)));

        ResponseEntity<List<ProductDetail>> response = similarProductsService.getSimilarProducts(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Producto 2");
        assertThat(response.getBody().get(1).getName()).isEqualTo("Producto 3");
    }

    @Test
    void getSimilarProductsReturnsEmptyListWhenNoSimilarIds() {
        String productId = "1";

        when(productApiClient.getSimilarProducts(productId))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        ResponseEntity<List<ProductDetail>> response = similarProductsService.getSimilarProducts(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getSimilarProductsReturnsOkWithNullResponseFromClient() {
        String productId = "1";

        when(productApiClient.getSimilarProducts(productId))
                .thenReturn(CompletableFuture.completedFuture(null));

        ResponseEntity<List<ProductDetail>> response = similarProductsService.getSimilarProducts(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getSimilarProductsReturnsNotFoundWhenClientThrowsNotFound() {
        String productId = "notfound";

        when(productApiClient.getSimilarProducts(productId))
                .thenReturn(CompletableFuture.failedFuture(
                        HttpClientErrorException.create(
                                HttpStatus.NOT_FOUND, "Not found",
                                null, null, null)));

        ResponseEntity<List<ProductDetail>> response = similarProductsService.getSimilarProducts(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getSimilarProductsReturnsInternalErrorOnGenericFailure() {
        String productId = "error";

        when(productApiClient.getSimilarProducts(productId))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Unexpected error")));

        ResponseEntity<List<ProductDetail>> response = similarProductsService.getSimilarProducts(productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void fallbackSimilarProductsReturnsServiceUnavailable() {
        ResponseEntity<List<ProductDetail>> response = similarProductsService
                .fallbackSimilarProducts("1", new Exception("Service down"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNull();
    }
}
