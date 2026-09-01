package com.project.coupon_order_api.service;

import com.project.coupon_order_api.dto.request.OrderItemRequest;
import com.project.coupon_order_api.dto.request.OrderRequest;
import com.project.coupon_order_api.entity.Coupon;
import com.project.coupon_order_api.entity.CouponType;
import com.project.coupon_order_api.entity.Product;
import com.project.coupon_order_api.exception.CouponUsageLimitExceededException;
import com.project.coupon_order_api.exception.InsufficientStockException;
import com.project.coupon_order_api.repository.CouponRepository;
import com.project.coupon_order_api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Spring context, real H2 DB, real threads — no mocks. Verifies spec.md rules 20-21,
 * which depend on the pessimistic locks in ProductRepository/CouponRepository actually
 * serializing concurrent transactions; that behavior can't be exercised with Mockito.
 */
@SpringBootTest
class OrderConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    void concurrentOrders_neverExceedCouponUsageLimit() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("ConcurrencyWidget")
                .price(new BigDecimal("10.00"))
                .stock(100)
                .build());

        Coupon coupon = couponRepository.save(Coupon.builder()
                .code("CONCURRENT-" + System.nanoTime())
                .type(CouponType.FIXED)
                .value(new BigDecimal("1.00"))
                .minOrderAmount(BigDecimal.ZERO)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .maxUses(2)
                .timesUsed(1)
                .build());

        Callable<Object> task = () -> {
            try {
                return orderService.createOrder(new OrderRequest(
                        List.of(new OrderItemRequest(product.getId(), 1)), coupon.getCode()));
            } catch (Exception e) {
                return e;
            }
        };

        List<Object> results = runConcurrently(task, task);

        long successes = results.stream().filter(r -> !(r instanceof Exception)).count();
        long usageLimitFailures = results.stream().filter(r -> r instanceof CouponUsageLimitExceededException).count();

        assertThat(successes).isEqualTo(1);
        assertThat(usageLimitFailures).isEqualTo(1);

        Coupon finalCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(finalCoupon.getTimesUsed()).isEqualTo(2);
    }

    @Test
    void concurrentOrders_neverOversellStock() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("ScarceConcurrencyWidget")
                .price(new BigDecimal("10.00"))
                .stock(1)
                .build());

        Callable<Object> task = () -> {
            try {
                return orderService.createOrder(new OrderRequest(
                        List.of(new OrderItemRequest(product.getId(), 1)), null));
            } catch (Exception e) {
                return e;
            }
        };

        List<Object> results = runConcurrently(task, task);

        long successes = results.stream().filter(r -> !(r instanceof Exception)).count();
        long stockFailures = results.stream().filter(r -> r instanceof InsufficientStockException).count();

        assertThat(successes).isEqualTo(1);
        assertThat(stockFailures).isEqualTo(1);

        Product finalProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(0);
    }

    private List<Object> runConcurrently(Callable<Object> task1, Callable<Object> task2) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Object> wrapped1 = () -> {
            startLatch.await();
            return task1.call();
        };
        Callable<Object> wrapped2 = () -> {
            startLatch.await();
            return task2.call();
        };

        Future<Object> future1 = executor.submit(wrapped1);
        Future<Object> future2 = executor.submit(wrapped2);

        startLatch.countDown();

        List<Object> results = List.of(future1.get(10, TimeUnit.SECONDS), future2.get(10, TimeUnit.SECONDS));
        executor.shutdown();
        return results;
    }
}
