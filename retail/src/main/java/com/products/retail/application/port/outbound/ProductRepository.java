package com.products.retail.application.port.outbound;

import com.products.retail.domain.model.ProductDetail;
import com.products.retail.domain.exception.ProductNotFoundException;

import java.util.List;

public interface ProductRepository {
    ProductDetail getProductDetail(String id);
    List<ProductDetail> getSimilarProducts(String productId) throws ProductNotFoundException;
}
