package com.project.coupon_order_api.mapper;

import com.project.coupon_order_api.dto.response.OrderLineResponse;
import com.project.coupon_order_api.dto.response.OrderResponse;
import com.project.coupon_order_api.entity.Order;
import com.project.coupon_order_api.entity.OrderLine;

import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        List<OrderLineResponse> items = order.getLines().stream()
                .map(OrderMapper::toLineResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                items,
                order.getSubtotal(),
                order.getAppliedCouponCode(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }

    private static OrderLineResponse toLineResponse(OrderLine line) {
        return new OrderLineResponse(
                line.getProductId(),
                line.getProductName(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getLineSubtotal()
        );
    }
}
