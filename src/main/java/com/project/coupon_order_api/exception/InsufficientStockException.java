package com.project.coupon_order_api.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ApiException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_STOCK",
                "Product " + productId + " has insufficient stock: requested " + requested + ", available " + available);
    }
}
