package com.project.coupon_order_api.exception;

import org.springframework.http.HttpStatus;

public class CouponInUseException extends ApiException {

    public CouponInUseException(Long id) {
        super(HttpStatus.CONFLICT, "COUPON_IN_USE", "Coupon with id " + id + " has already been used and cannot be deleted");
    }
}
