package com.products.retail.service;

import com.products.retail.model.ProductDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para obtener productos similares a uno dado.
 */
@Service
public class SimilarProductsService {
    private final RestTemplate restTemplate;
    @Value("${mocks.base.url}")
    private String mocksBaseUrl;

    @Autowired
    public SimilarProductsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Obtiene los detalles de los productos similares a un producto dado.
     * Realiza reintentos automáticos en caso de error temporal.
     * @param productId ID del producto base.
     * @return Lista de detalles de productos similares o error adecuado.
     */
    @Retry(name = "similarProductsRetry", fallbackMethod = "fallbackSimilarProducts")
    public ResponseEntity<List<ProductDetail>> getSimilarProducts(String productId) {
        try {
            String idsUrl = mocksBaseUrl + "/product/" + productId + "/similarids";
            String[] similarIds = restTemplate.getForObject(idsUrl, String[].class);
            if (similarIds == null) {
                return ResponseEntity.ok(List.of());
            }
            List<ProductDetail> similarProducts = new ArrayList<>();
            for (String id : similarIds) {
                ProductDetail detail = getProductDetailById(id);
                if (detail != null) {
                    similarProducts.add(detail);
                }
            }
            return ResponseEntity.ok(similarProducts);
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene el detalle de un producto por su ID.
     * @param id ID del producto.
     * @return Detalle del producto o null si no se encuentra.
     */
    private ProductDetail getProductDetailById(String id) {
        try {
            String detailUrl = mocksBaseUrl + "/product/" + id;
            return restTemplate.getForObject(detailUrl, ProductDetail.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    /**
     * Fallback que se ejecuta si los reintentos fallan.
     * @param productId ID del producto base.
     * @param ex Excepción lanzada.
     * @return Respuesta de error 503 (Servicio no disponible).
     */
    public ResponseEntity<List<ProductDetail>> fallbackSimilarProducts(String productId, Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
} 