package com.products.retail.application.port.inbound;

import com.products.retail.application.dto.ProductDetailResponse;

import java.util.List;

public interface GetSimilarProductsUseCase {
    List<ProductDetailResponse> getSimilarProducts(String productId);
}
