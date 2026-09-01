package com.project.coupon_order_api.service;

import com.project.coupon_order_api.dto.request.CouponRequest;
import com.project.coupon_order_api.dto.response.CouponResponse;
import com.project.coupon_order_api.entity.Coupon;
import com.project.coupon_order_api.entity.CouponType;
import com.project.coupon_order_api.exception.CouponInUseException;
import com.project.coupon_order_api.exception.CouponNotFoundException;
import com.project.coupon_order_api.exception.DuplicateCouponCodeException;
import com.project.coupon_order_api.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    void create_duplicateCode_isRejected() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        CouponRequest request = request("SAVE10");

        assertThatThrownBy(() -> couponService.create(request))
                .isInstanceOf(DuplicateCouponCodeException.class);

        verify(couponRepository, never()).save(any());
    }

    @Test
    void update_success_preservesTimesUsed() {
        Coupon existing = coupon(1L, 3);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(couponRepository.existsByCodeAndIdNot("SAVE10", 1L)).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponRequest updateRequest = new CouponRequest("SAVE10", CouponType.FIXED, new BigDecimal("7"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(2), 10);

        CouponResponse response = couponService.update(1L, updateRequest);

        assertThat(response.value()).isEqualByComparingTo("7");
        assertThat(response.maxUses()).isEqualTo(10);
        assertThat(response.timesUsed()).isEqualTo(3);
    }

    @Test
    void update_nonExistent_throwsNotFound() {
        when(couponRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.update(99L, request("SAVE10")))
                .isInstanceOf(CouponNotFoundException.class);

        verify(couponRepository, never()).save(any());
    }

    @Test
    void update_codeCollisionWithAnotherCoupon_isRejected() {
        Coupon existing = coupon(1L, 0);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(couponRepository.existsByCodeAndIdNot("TAKEN", 1L)).thenReturn(true);

        CouponRequest updateRequest = new CouponRequest("TAKEN", CouponType.FIXED, new BigDecimal("5"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 5);

        assertThatThrownBy(() -> couponService.update(1L, updateRequest))
                .isInstanceOf(DuplicateCouponCodeException.class);

        verify(couponRepository, never()).save(any());
    }

    @Test
    void delete_unused_succeeds() {
        Coupon coupon = coupon(1L, 0);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        couponService.delete(1L);

        verify(couponRepository, times(1)).delete(coupon);
    }

    @Test
    void delete_used_isBlocked() {
        Coupon coupon = coupon(1L, 3);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.delete(1L))
                .isInstanceOf(CouponInUseException.class);

        verify(couponRepository, never()).delete(any());
    }

    @Test
    void delete_nonExistent_throwsNotFound() {
        when(couponRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.delete(99L))
                .isInstanceOf(CouponNotFoundException.class);
    }

    private static CouponRequest request(String code) {
        return new CouponRequest(code, CouponType.FIXED, new BigDecimal("5"), BigDecimal.ZERO,
                LocalDateTime.now().plusDays(1), 5);
    }

    private static Coupon coupon(Long id, int timesUsed) {
        return Coupon.builder()
                .id(id)
                .code("SAVE10")
                .type(CouponType.FIXED)
                .value(new BigDecimal("5"))
                .minOrderAmount(BigDecimal.ZERO)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .maxUses(5)
                .timesUsed(timesUsed)
                .build();
    }
}
