package com.project.coupon_order_api.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApiException {

    public ProductNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product with id " + id + " does not exist");
    }
}
