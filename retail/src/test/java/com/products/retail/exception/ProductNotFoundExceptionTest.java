package com.products.retail.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

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

    @Test
    void exceptionHasNotFoundResponseStatus() {
        ResponseStatus status = ProductNotFoundException.class.getAnnotation(ResponseStatus.class);
        assertThat(status).isNotNull();
        assertThat(status.value()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
