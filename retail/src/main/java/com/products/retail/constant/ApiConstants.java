package com.products.retail.constant;

public final class ApiConstants {

    private ApiConstants() {
    }

    public static final String CACHE_SIMILAR_IDS = "similarIds";
    public static final String CACHE_PRODUCT_DETAILS = "productDetails";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";

    public static final int DETAIL_TIMEOUT_SECONDS = 3;

    public static final String ERROR_PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
    public static final String ERROR_BAD_REQUEST = "BAD_REQUEST";
    public static final String ERROR_NOT_FOUND = "NOT_FOUND";
    public static final String ERROR_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String ERROR_TOO_MANY_REQUESTS = "TOO_MANY_REQUESTS";
    public static final String ERROR_INTERNAL = "INTERNAL_ERROR";
}
