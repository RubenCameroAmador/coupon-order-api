package com.project.coupon_order_api.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApiException {

    public OrderNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order with id " + id + " does not exist");
    }
}
