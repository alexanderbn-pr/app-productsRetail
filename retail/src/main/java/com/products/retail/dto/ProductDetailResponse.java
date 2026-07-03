package com.products.retail.dto;

import java.math.BigDecimal;

public record ProductDetailResponse(String id, String name, BigDecimal price, boolean availability) {
}
