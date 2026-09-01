package com.project.coupon_order_api.exception;

import org.springframework.http.HttpStatus;

public class DuplicateCouponCodeException extends ApiException {

    public DuplicateCouponCodeException(String code) {
        super(HttpStatus.CONFLICT, "DUPLICATE_COUPON_CODE", "Coupon code '" + code + "' already exists");
    }
}
