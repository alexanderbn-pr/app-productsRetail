package com.products.retail.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class ProductNotFoundExceptionTest {

    @Test
    void exceptionContainsProductIdInMessage() {
        var ex = new ProductNotFoundException("123");
        assertThat(ex.getMessage()).contains("123");
    }

    @Test
    void exceptionIsRuntimeException() {
        var ex = new ProductNotFoundException("456");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
