package com.products.retail.service;

import com.products.retail.model.ProductDetail;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimilarProductsServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SimilarProductsService similarProductsService;

    @BeforeEach
    void setUp() {
        similarProductsService = new SimilarProductsService(restTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(similarProductsService, "mocksBaseUrl", "http://mock");
    }

    @Test
    void getSimilarProductsReturnsOk() {
        String productId = "1";
        String[] similarIds = {"2", "3"};
        ProductDetail product2 = new ProductDetail("2", "Producto 2", 20.0, true);
        ProductDetail product3 = new ProductDetail("3", "Producto 3", 30.0, false);

        when(restTemplate.getForObject("http://mock/product/1/similarids", String[].class)).thenReturn(similarIds);
        when(restTemplate.getForObject("http://mock/product/2", ProductDetail.class)).thenReturn(product2);
        when(restTemplate.getForObject("http://mock/product/3", ProductDetail.class)).thenReturn(product3);

        ResponseEntity<List<ProductDetail>> response = similarProductsService.getSimilarProducts(productId);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(2, response.getBody().size());
        Assertions.assertEquals("Producto 2", response.getBody().get(0).getName());
        Assertions.assertEquals("Producto 3", response.getBody().get(1).getName());
        verify(restTemplate, times(1)).getForObject("http://mock/product/1/similarids", String[].class);
        verify(restTemplate, times(1)).getForObject("http://mock/product/2", ProductDetail.class);
        verify(restTemplate, times(1)).getForObject("http://mock/product/3", ProductDetail.class);
    }

    @Test
    void getSimilarProductsReturnsEmptyListWhenNoSimilarIds() {
        String productId = "1";
        when(restTemplate.getForObject("http://mock/product/1/similarids", String[].class)).thenReturn(null);

        ResponseEntity<List<ProductDetail>> response = similarProductsService.getSimilarProducts(productId);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertTrue(response.getBody().isEmpty());
        verify(restTemplate, times(1)).getForObject("http://mock/product/1/similarids", String[].class);
    }
    
    @Test
    void getSimilarProductsReturnsInternalServerError() {
        String productId = "error";
        when(restTemplate.getForObject("http://mock/product/error/similarids", String[].class))
                .thenThrow(new RuntimeException("error"));

        ResponseEntity<List<ProductDetail>> response = similarProductsService.getSimilarProducts(productId);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertNull(response.getBody());
        verify(restTemplate, times(1)).getForObject("http://mock/product/error/similarids", String[].class);
    }

    @Test
    void fallbackSimilarProductsReturnsServiceUnavailable() {
        ResponseEntity<List<ProductDetail>> response = similarProductsService.fallbackSimilarProducts("1", new Exception("error"));
        Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        Assertions.assertNull(response.getBody());
    }
}