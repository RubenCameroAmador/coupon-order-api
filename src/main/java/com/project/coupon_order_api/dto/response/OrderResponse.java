package com.project.coupon_order_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        List<OrderLineResponse> items,
        BigDecimal subtotal,
        String couponCode,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
}
