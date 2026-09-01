package com.project.coupon_order_api.service;

import com.project.coupon_order_api.dto.request.OrderItemRequest;
import com.project.coupon_order_api.dto.request.OrderRequest;
import com.project.coupon_order_api.dto.response.OrderResponse;
import com.project.coupon_order_api.entity.Coupon;
import com.project.coupon_order_api.entity.CouponType;
import com.project.coupon_order_api.entity.Order;
import com.project.coupon_order_api.entity.Product;
import com.project.coupon_order_api.exception.CouponExpiredException;
import com.project.coupon_order_api.exception.CouponNotFoundException;
import com.project.coupon_order_api.exception.CouponUsageLimitExceededException;
import com.project.coupon_order_api.exception.InsufficientStockException;
import com.project.coupon_order_api.exception.MinOrderAmountNotMetException;
import com.project.coupon_order_api.exception.ProductNotFoundException;
import com.project.coupon_order_api.repository.CouponRepository;
import com.project.coupon_order_api.repository.OrderRepository;
import com.project.coupon_order_api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void expiredCoupon_isRejected() {
        Product widget = product(1L, "Widget", "20.00", 10);
        Coupon coupon = coupon(1L, "EXPIRED10", CouponType.PERCENTAGE, "10", "0",
                LocalDateTime.now().minusDays(1), 5, 0);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("EXPIRED10")).thenReturn(Optional.of(coupon));

        OrderRequest request = orderRequest(items(item(1L, 1)), "EXPIRED10");

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CouponExpiredException.class);

        verify(productRepository, never()).save(any());
        verify(couponRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void couponExpiringMomentarily_isNotYetExpired_isAccepted() {
        // Rule 2: only `now > expirationDate` rejects, so a coupon that hasn't technically
        // expired yet - even by a slim margin - must still be honored. A few seconds of
        // buffer keeps this deterministic without needing to inject a Clock into OrderService.
        Product widget = product(1L, "Widget", "20.00", 10);
        Coupon coupon = coupon(1L, "JUSTINTIME", CouponType.FIXED, "5", "0",
                LocalDateTime.now().plusSeconds(2), 5, 0);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("JUSTINTIME")).thenReturn(Optional.of(coupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = orderRequest(items(item(1L, 1)), "JUSTINTIME");

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.discountAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void couponAtMaxUses_isRejected() {
        Product widget = product(1L, "Widget", "20.00", 10);
        Coupon coupon = coupon(1L, "MAXED", CouponType.FIXED, "5", "0",
                LocalDateTime.now().plusDays(1), 2, 2);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("MAXED")).thenReturn(Optional.of(coupon));

        OrderRequest request = orderRequest(items(item(1L, 1)), "MAXED");

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CouponUsageLimitExceededException.class);

        verify(productRepository, never()).save(any());
        verify(couponRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void subtotalBelowMinOrderAmount_isRejected() {
        Product widget = product(1L, "Widget", "20.00", 10);
        Coupon coupon = coupon(1L, "MIN100", CouponType.FIXED, "5", "100.00",
                LocalDateTime.now().plusDays(1), 5, 0);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("MIN100")).thenReturn(Optional.of(coupon));

        OrderRequest request = orderRequest(items(item(1L, 1)), "MIN100");

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(MinOrderAmountNotMetException.class);

        verify(productRepository, never()).save(any());
        verify(couponRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void subtotalEqualsMinOrderAmount_isAccepted() {
        Product widget = product(1L, "Widget", "20.00", 10);
        Coupon coupon = coupon(1L, "MIN20", CouponType.FIXED, "5", "20.00",
                LocalDateTime.now().plusDays(1), 5, 0);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("MIN20")).thenReturn(Optional.of(coupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = orderRequest(items(item(1L, 1)), "MIN20");

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.totalAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    void percentageDiscount_isComputedAndRounded() {
        Product widget = product(1L, "Widget", "10.00", 10);
        Coupon coupon = coupon(1L, "SAVE33", CouponType.PERCENTAGE, "33.33", "0",
                LocalDateTime.now().plusDays(1), 5, 0);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("SAVE33")).thenReturn(Optional.of(coupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = orderRequest(items(item(1L, 3)), "SAVE33");

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    void fixedDiscount_isComputed() {
        Product widget = product(1L, "Widget", "20.00", 10);
        Coupon coupon = coupon(1L, "FIXED5", CouponType.FIXED, "5.00", "0",
                LocalDateTime.now().plusDays(1), 5, 0);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("FIXED5")).thenReturn(Optional.of(coupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = orderRequest(items(item(1L, 1)), "FIXED5");

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.discountAmount()).isEqualByComparingTo("5.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    void fixedDiscountGreaterThanSubtotal_isCappedAndTotalFloorsAtZero() {
        Product widget = product(1L, "Widget", "40.00", 10);
        Coupon coupon = coupon(1L, "BIG100", CouponType.FIXED, "100.00", "0",
                LocalDateTime.now().plusDays(1), 5, 0);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("BIG100")).thenReturn(Optional.of(coupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = orderRequest(items(item(1L, 1)), "BIG100");

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.discountAmount()).isEqualByComparingTo("40.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void insufficientStockOnAnyLine_rejectsWholeOrder_noSideEffects() {
        Product widget = product(1L, "Widget", "20.00", 5);
        Product gadget = product(2L, "Gadget", "15.00", 1);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(gadget));

        OrderRequest request = orderRequest(items(item(1L, 2), item(2L, 3)), null);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(productRepository, never()).save(any());
        verify(couponRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void unknownCouponCode_returns404Exception() {
        Product widget = product(1L, "Widget", "20.00", 10);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("MISSING")).thenReturn(Optional.empty());

        OrderRequest request = orderRequest(items(item(1L, 1)), "MISSING");

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CouponNotFoundException.class);

        verify(productRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void unknownProductId_returns404Exception() {
        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        OrderRequest request = orderRequest(items(item(99L, 1)), "SAVE10");

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ProductNotFoundException.class);

        verify(couponRepository, never()).findByCodeForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void successfulOrder_incrementsTimesUsed() {
        Product widget = product(1L, "Widget", "20.00", 10);
        Coupon coupon = coupon(1L, "SAVE10", CouponType.PERCENTAGE, "10", "0",
                LocalDateTime.now().plusDays(1), 5, 1);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(couponRepository.findByCodeForUpdate("SAVE10")).thenReturn(Optional.of(coupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = orderRequest(items(item(1L, 1)), "SAVE10");

        orderService.createOrder(request);

        assertThat(coupon.getTimesUsed()).isEqualTo(2);
        verify(couponRepository).save(coupon);
    }

    @Test
    void noCouponProvided_zeroDiscount() {
        Product widget = product(1L, "Widget", "20.00", 10);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = orderRequest(items(item(1L, 2)), null);

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.discountAmount()).isEqualByComparingTo("0");
        assertThat(response.subtotal()).isEqualByComparingTo("40.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("40.00");
        verify(couponRepository, never()).findByCodeForUpdate(any());
    }

    @Test
    void happyPath_correctTotalsStockAndUsage() {
        Product widget = product(1L, "Widget", "20.00", 10);
        Product gadget = product(2L, "Gadget", "15.00", 10);
        Coupon coupon = coupon(1L, "SAVE10", CouponType.PERCENTAGE, "10", "0",
                LocalDateTime.now().plusDays(1), 5, 0);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));
        when(productRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(gadget));
        when(couponRepository.findByCodeForUpdate("SAVE10")).thenReturn(Optional.of(coupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderRequest request = orderRequest(items(item(1L, 2), item(2L, 1)), "SAVE10");

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.subtotal()).isEqualByComparingTo("55.00");
        assertThat(response.discountAmount()).isEqualByComparingTo("5.50");
        assertThat(response.totalAmount()).isEqualByComparingTo("49.50");
        assertThat(widget.getStock()).isEqualTo(8);
        assertThat(gadget.getStock()).isEqualTo(9);
        assertThat(coupon.getTimesUsed()).isEqualTo(1);
    }

    @Test
    void multipleFailures_stockCheckedBeforeCoupon() {
        Product widget = product(1L, "Widget", "20.00", 1);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(widget));

        OrderRequest request = orderRequest(items(item(1L, 5)), "EXPIRED");

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(couponRepository, never()).findByCodeForUpdate(any());
    }

    private static Product product(Long id, String name, String price, int stock) {
        return Product.builder()
                .id(id)
                .name(name)
                .price(new BigDecimal(price))
                .stock(stock)
                .build();
    }

    private static Coupon coupon(Long id, String code, CouponType type, String value, String minOrderAmount,
                                  LocalDateTime expirationDate, int maxUses, int timesUsed) {
        return Coupon.builder()
                .id(id)
                .code(code)
                .type(type)
                .value(new BigDecimal(value))
                .minOrderAmount(new BigDecimal(minOrderAmount))
                .expirationDate(expirationDate)
                .maxUses(maxUses)
                .timesUsed(timesUsed)
                .build();
    }

    private static OrderItemRequest item(Long productId, int quantity) {
        return new OrderItemRequest(productId, quantity);
    }

    private static List<OrderItemRequest> items(OrderItemRequest... items) {
        return List.of(items);
    }

    private static OrderRequest orderRequest(List<OrderItemRequest> items, String couponCode) {
        return new OrderRequest(items, couponCode);
    }
}
