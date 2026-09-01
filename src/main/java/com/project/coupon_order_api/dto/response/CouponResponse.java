package com.project.coupon_order_api.dto.response;

import com.project.coupon_order_api.entity.CouponType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        CouponType type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        LocalDateTime expirationDate,
        Integer maxUses,
        Integer timesUsed
) {
}
