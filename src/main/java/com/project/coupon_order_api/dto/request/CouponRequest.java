package com.project.coupon_order_api.dto.request;

import com.project.coupon_order_api.dto.request.validation.ValidCouponValue;
import com.project.coupon_order_api.entity.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ValidCouponValue
public record CouponRequest(
        @NotBlank String code,
        @NotNull CouponType type,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal value,
        @NotNull @DecimalMin(value = "0.0") BigDecimal minOrderAmount,
        @NotNull @Future LocalDateTime expirationDate,
        @NotNull @Positive Integer maxUses
) {
}
