package com.project.coupon_order_api.dto.request.validation;

import com.project.coupon_order_api.dto.request.CouponRequest;
import com.project.coupon_order_api.entity.CouponType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class ValidCouponValueValidator implements ConstraintValidator<ValidCouponValue, CouponRequest> {

    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100");

    @Override
    public boolean isValid(CouponRequest request, ConstraintValidatorContext context) {
        if (request == null || request.type() == null || request.value() == null) {
            return true;
        }
        if (request.type() == CouponType.PERCENTAGE) {
            return request.value().compareTo(MAX_PERCENTAGE) <= 0;
        }
        return true;
    }
}
