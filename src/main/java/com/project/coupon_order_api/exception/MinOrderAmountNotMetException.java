package com.project.coupon_order_api.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class MinOrderAmountNotMetException extends ApiException {

    public MinOrderAmountNotMetException(String code, BigDecimal subtotal, BigDecimal minOrderAmount) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "MIN_ORDER_AMOUNT_NOT_MET",
                "Coupon '" + code + "' requires a minimum order amount of " + minOrderAmount
                        + ", but the order subtotal was " + subtotal);
    }
}
