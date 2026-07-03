package com.products.retail.mapper;

import com.products.retail.dto.ProductDetailResponse;
import com.products.retail.model.ProductDetail;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductDetailMapper {

    ProductDetailResponse toProductDetailResponse(ProductDetail detail);
}
