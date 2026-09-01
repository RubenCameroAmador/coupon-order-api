package com.project.coupon_order_api.exception;

import org.springframework.http.HttpStatus;

public class CouponNotFoundException extends ApiException {

    public CouponNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "Coupon with id " + id + " does not exist");
    }

    public CouponNotFoundException(String code) {
        super(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "Coupon with code '" + code + "' does not exist");
    }
}
