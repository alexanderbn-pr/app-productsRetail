package com.products.retail.application.service;

import com.products.retail.application.dto.ProductDetailResponse;
import com.products.retail.application.mapper.ProductDetailMapper;
import com.products.retail.application.port.outbound.ProductRepository;
import com.products.retail.domain.exception.ProductNotFoundException;
import com.products.retail.domain.model.ProductDetail;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class SimilarProductsServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductDetailMapper productDetailMapper;

    @InjectMocks
    private SimilarProductsService similarProductsService;

    @Test
    void getSimilarProductsShouldBeAnnotatedWithRateLimiter() throws Exception {
        var method = SimilarProductsService.class.getMethod("getSimilarProducts", String.class);
        var annotation = method.getAnnotation(RateLimiter.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("similarProducts");
    }

    @Test
    void getSimilarProductsReturnsListWithProductDetails() {
        String productId = "1";
        ProductDetail product2 = new ProductDetail("2", "Producto 2", new BigDecimal("20.00"), true);
        ProductDetail product3 = new ProductDetail("3", "Producto 3", new BigDecimal("30.00"), false);
        ProductDetailResponse response2 = new ProductDetailResponse("2", "Producto 2", new BigDecimal("20.00"), true);
        ProductDetailResponse response3 = new ProductDetailResponse("3", "Producto 3", new BigDecimal("30.00"), false);

        when(productRepository.getSimilarProducts(productId)).thenReturn(List.of(product2, product3));
        when(productDetailMapper.toProductDetailResponse(any(ProductDetail.class)))
                .thenAnswer(invocation -> {
                    ProductDetail d = invocation.getArgument(0);
                    return new ProductDetailResponse(d.id(), d.name(), d.price(), d.availability());
                });

        List<ProductDetailResponse> result = similarProductsService.getSimilarProducts(productId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("2");
        assertThat(result.get(0).name()).isEqualTo("Producto 2");
        assertThat(result.get(1).id()).isEqualTo("3");
        assertThat(result.get(1).name()).isEqualTo("Producto 3");
    }

    @Test
    void getSimilarProductsReturnsEmptyListWhenNoSimilarIds() {
        String productId = "1";

        when(productRepository.getSimilarProducts(productId)).thenReturn(List.of());

        List<ProductDetailResponse> result = similarProductsService.getSimilarProducts(productId);

        assertThat(result).isEmpty();
    }

    @Test
    void getSimilarProductsThrowsProductNotFoundExceptionWhenClientReturns404() {
        String productId = "notfound";

        when(productRepository.getSimilarProducts(productId))
                .thenThrow(new ProductNotFoundException(productId));

        assertThatThrownBy(() -> similarProductsService.getSimilarProducts(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(productId);
    }

    @Test
    void getSimilarProductsThrowsRuntimeExceptionOnGenericFailure() {
        String productId = "error";

        when(productRepository.getSimilarProducts(productId))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThatThrownBy(() -> similarProductsService.getSimilarProducts(productId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void fallbackSimilarProductsReturnsEmptyList() {
        List<ProductDetailResponse> result = similarProductsService
                .fallbackSimilarProducts("1", new Exception("Service down"));

        assertThat(result).isEmpty();
    }
}
