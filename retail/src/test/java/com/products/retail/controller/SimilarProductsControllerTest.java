package com.products.retail.controller;

import com.products.retail.model.ProductDetail;
import com.products.retail.service.SimilarProductsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimilarProductsControllerTest {

    @InjectMocks
    private SimilarProductsController similarProductsController;

    @Mock
    private SimilarProductsService similarProductsService;

    @Test
    void getSimilarProductsReturnsOk() {
        String productId = "1";
        ProductDetail product = new ProductDetail("1", "Producto 1", 10.0, true);
        List<ProductDetail> productList = List.of(product);

        when(similarProductsService.getSimilarProducts(productId))
                .thenReturn(ResponseEntity.ok(productList));

        ResponseEntity<List<ProductDetail>> response = similarProductsController.getSimilarProducts(productId);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(1, response.getBody().size());
        Assertions.assertEquals("Producto 1", response.getBody().get(0).getName());
        verify(similarProductsService, times(1)).getSimilarProducts(productId);
    }

    @Test
    void getSimilarProductsReturnsNotFound() {
        String productId = "999";
        when(similarProductsService.getSimilarProducts(productId))
                .thenReturn(ResponseEntity.notFound().build());

        ResponseEntity<List<ProductDetail>> response = similarProductsController.getSimilarProducts(productId);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Assertions.assertNull(response.getBody());
        verify(similarProductsService, times(1)).getSimilarProducts(productId);
    }

    @Test
    void getSimilarProductsReturnsServerError() {
        String productId = "error";
        when(similarProductsService.getSimilarProducts(productId))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        ResponseEntity<List<ProductDetail>> response = similarProductsController.getSimilarProducts(productId);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertNull(response.getBody());
        verify(similarProductsService, times(1)).getSimilarProducts(productId);
    }
}