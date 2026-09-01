package com.project.coupon_order_api.exception;

import org.springframework.http.HttpStatus;

public class CouponUsageLimitExceededException extends ApiException {

    public CouponUsageLimitExceededException(String code) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "COUPON_USAGE_LIMIT_EXCEEDED", "Coupon '" + code + "' has reached its usage limit");
    }
}
