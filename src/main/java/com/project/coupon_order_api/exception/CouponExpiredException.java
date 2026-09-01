package com.project.coupon_order_api.exception;

import org.springframework.http.HttpStatus;

public class CouponExpiredException extends ApiException {

    public CouponExpiredException(String code) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "COUPON_EXPIRED", "Coupon '" + code + "' has expired");
    }
}
