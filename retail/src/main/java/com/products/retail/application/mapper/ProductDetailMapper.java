package com.products.retail.application.mapper;

import com.products.retail.application.dto.ProductDetailResponse;
import com.products.retail.domain.model.ProductDetail;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductDetailMapper {

    ProductDetailResponse toProductDetailResponse(ProductDetail detail);
}
