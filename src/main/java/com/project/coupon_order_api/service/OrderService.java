package com.project.coupon_order_api.service;

import com.project.coupon_order_api.dto.request.OrderRequest;
import com.project.coupon_order_api.dto.response.OrderResponse;
import com.project.coupon_order_api.entity.Coupon;
import com.project.coupon_order_api.entity.CouponType;
import com.project.coupon_order_api.entity.Order;
import com.project.coupon_order_api.entity.OrderLine;
import com.project.coupon_order_api.entity.Product;
import com.project.coupon_order_api.exception.CouponExpiredException;
import com.project.coupon_order_api.exception.CouponNotFoundException;
import com.project.coupon_order_api.exception.CouponUsageLimitExceededException;
import com.project.coupon_order_api.exception.InsufficientStockException;
import com.project.coupon_order_api.exception.MinOrderAmountNotMetException;
import com.project.coupon_order_api.exception.OrderNotFoundException;
import com.project.coupon_order_api.exception.ProductNotFoundException;
import com.project.coupon_order_api.mapper.OrderMapper;
import com.project.coupon_order_api.repository.CouponRepository;
import com.project.coupon_order_api.repository.OrderRepository;
import com.project.coupon_order_api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Order lines referencing the same productId are merged (quantities summed) before any
 * validation, and products are locked in ascending-id order — both needed so two concurrent
 * orders can never oversell the same product (see spec.md rules 9, 20, 21).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Map<Long, Integer> quantityByProductId = request.items().stream()
                .collect(Collectors.toMap(
                        item -> item.productId(),
                        item -> item.quantity(),
                        Integer::sum,
                        LinkedHashMap::new
                ));

        List<Long> lockOrderedProductIds = quantityByProductId.keySet().stream().sorted().toList();

        List<ResolvedLine> resolvedLines = new ArrayList<>();
        for (Long productId : lockOrderedProductIds) {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            resolvedLines.add(new ResolvedLine(product, quantityByProductId.get(productId)));
        }

        for (ResolvedLine line : resolvedLines) {
            if (line.product().getStock() < line.quantity()) {
                throw new InsufficientStockException(line.product().getId(), line.quantity(), line.product().getStock());
            }
        }

        BigDecimal subtotal = resolvedLines.stream()
                .map(line -> line.product().getPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Coupon coupon = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (StringUtils.hasText(request.couponCode())) {
            coupon = couponRepository.findByCodeForUpdate(request.couponCode())
                    .orElseThrow(() -> new CouponNotFoundException(request.couponCode()));

            if (LocalDateTime.now().isAfter(coupon.getExpirationDate())) {
                throw new CouponExpiredException(coupon.getCode());
            }
            if (coupon.getTimesUsed() >= coupon.getMaxUses()) {
                throw new CouponUsageLimitExceededException(coupon.getCode());
            }
            if (subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
                throw new MinOrderAmountNotMetException(coupon.getCode(), subtotal, coupon.getMinOrderAmount());
            }

            discount = coupon.getType() == CouponType.PERCENTAGE
                    ? subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : coupon.getValue().min(subtotal);
        }

        BigDecimal effectiveDiscount = discount.min(subtotal);
        BigDecimal totalAmount = subtotal.subtract(effectiveDiscount).max(BigDecimal.ZERO);

        for (ResolvedLine line : resolvedLines) {
            Product product = line.product();
            product.setStock(product.getStock() - line.quantity());
            productRepository.save(product);
        }

        if (coupon != null) {
            coupon.setTimesUsed(coupon.getTimesUsed() + 1);
            couponRepository.save(coupon);
        }

        Order order = Order.builder()
                .subtotal(subtotal)
                .appliedCouponCode(coupon != null ? coupon.getCode() : null)
                .discountAmount(effectiveDiscount)
                .totalAmount(totalAmount)
                .build();

        for (ResolvedLine line : resolvedLines) {
            Product product = line.product();
            OrderLine orderLine = OrderLine.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(line.quantity())
                    .lineSubtotal(product.getPrice().multiply(BigDecimal.valueOf(line.quantity())))
                    .build();
            order.addLine(orderLine);
        }

        Order saved = orderRepository.save(order);
        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithLines(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllWithLines().stream().map(OrderMapper::toResponse).toList();
    }

    private record ResolvedLine(Product product, Integer quantity) {
    }
}
