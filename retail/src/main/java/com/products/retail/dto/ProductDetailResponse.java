package com.products.retail.dto;

import com.products.retail.model.ProductDetail;

import java.math.BigDecimal;

public record ProductDetailResponse(String id, String name, BigDecimal price, boolean availability) {

    public static ProductDetailResponse from(ProductDetail detail) {
        return new ProductDetailResponse(
                detail.getId(),
                detail.getName(),
                detail.getPrice(),
                detail.isAvailability());
    }
}
