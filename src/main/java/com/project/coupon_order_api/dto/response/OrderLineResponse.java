package com.project.coupon_order_api.dto.response;

import java.math.BigDecimal;

public record OrderLineResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineSubtotal
) {
}
