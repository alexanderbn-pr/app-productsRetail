package com.products.retail.mapper;

import com.products.retail.dto.ProductDetailResponse;
import com.products.retail.model.ProductDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDetailMapperTest {

    private final ProductDetailMapper mapper = new ProductDetailMapperImpl();

    @Test
    void shouldMapAllFields() {
        ProductDetail detail = new ProductDetail("1", "Producto 1", new BigDecimal("10.00"), true);
        ProductDetailResponse response = mapper.toProductDetailResponse(detail);

        assertThat(response.id()).isEqualTo("1");
        assertThat(response.name()).isEqualTo("Producto 1");
        assertThat(response.price()).isEqualTo(new BigDecimal("10.00"));
        assertThat(response.availability()).isTrue();
    }

    @Test
    void shouldMapWithNullName() {
        ProductDetail detail = new ProductDetail("2", null, new BigDecimal("20.00"), false);
        ProductDetailResponse response = mapper.toProductDetailResponse(detail);

        assertThat(response.id()).isEqualTo("2");
        assertThat(response.name()).isNull();
        assertThat(response.price()).isEqualTo(new BigDecimal("20.00"));
        assertThat(response.availability()).isFalse();
    }

    @Test
    void shouldMapWithNullPrice() {
        ProductDetail detail = new ProductDetail("3", "Producto 3", null, true);
        ProductDetailResponse response = mapper.toProductDetailResponse(detail);

        assertThat(response.id()).isEqualTo("3");
        assertThat(response.name()).isEqualTo("Producto 3");
        assertThat(response.price()).isNull();
        assertThat(response.availability()).isTrue();
    }
}
