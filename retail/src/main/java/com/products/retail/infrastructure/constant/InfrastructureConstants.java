package com.products.retail.infrastructure.constant;

public final class InfrastructureConstants {

    private InfrastructureConstants() {
    }

    public static final String CACHE_SIMILAR_IDS = "similarIds";
    public static final String CACHE_PRODUCT_DETAILS = "productDetails";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";
    public static final int DETAIL_TIMEOUT_SECONDS = 3;
}
