package com.products.retail.controller;

import com.products.retail.dto.ProductDetailResponse;
import com.products.retail.service.SimilarProductsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/product")
@Validated
@Tag(name = "Similar Products", description = "Endpoints for retrieving similar retail products")
@ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json"))
@ApiResponse(responseCode = "503", description = "Service temporarily unavailable", content = @Content(mediaType = "application/json"))
public class SimilarProductsController {

    private final SimilarProductsService similarProductsService;

    public SimilarProductsController(SimilarProductsService similarProductsService) {
        this.similarProductsService = similarProductsService;
    }

    @GetMapping("/{productId}/similar")
    @Operation(summary = "Get similar products")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved similar products",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ProductDetailResponse.class))))
    public ResponseEntity<List<ProductDetailResponse>> getSimilarProducts(@PathVariable String productId) {
        var details = similarProductsService.getSimilarProducts(productId);
        var response = details.stream()
                .map(ProductDetailResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }
}
