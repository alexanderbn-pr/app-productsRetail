package com.products.retail.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDetailRecordTest {

    @Test
    void shouldUseRecordAccessorPattern() {
        ProductDetail detail = new ProductDetail("1", "Test", new BigDecimal("10.00"), true);
        assertThat(detail.id()).isEqualTo("1");
        assertThat(detail.name()).isEqualTo("Test");
        assertThat(detail.price()).isEqualTo(new BigDecimal("10.00"));
        assertThat(detail.availability()).isTrue();
    }
}
