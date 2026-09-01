package com.project.coupon_order_api.dto.request.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidCouponValueValidator.class)
public @interface ValidCouponValue {

    String message() default "value must be <= 100 when type is PERCENTAGE";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
