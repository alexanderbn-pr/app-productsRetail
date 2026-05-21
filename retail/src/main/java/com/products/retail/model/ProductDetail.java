package com.products.retail.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Deserialized from the external API — mutable (Lombok) for Jackson. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetail {

    private String id;
    private String name;
    private BigDecimal price;
    private boolean availability;
}