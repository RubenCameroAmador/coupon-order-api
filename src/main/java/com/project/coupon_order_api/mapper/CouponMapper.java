package com.project.coupon_order_api.mapper;

import com.project.coupon_order_api.dto.request.CouponRequest;
import com.project.coupon_order_api.dto.response.CouponResponse;
import com.project.coupon_order_api.entity.Coupon;

public final class CouponMapper {

    private CouponMapper() {
    }

    public static Coupon toEntity(CouponRequest request) {
        return Coupon.builder()
                .code(request.code())
                .type(request.type())
                .value(request.value())
                .minOrderAmount(request.minOrderAmount())
                .expirationDate(request.expirationDate())
                .maxUses(request.maxUses())
                .timesUsed(0)
                .build();
    }

    public static void updateEntity(Coupon coupon, CouponRequest request) {
        coupon.setCode(request.code());
        coupon.setType(request.type());
        coupon.setValue(request.value());
        coupon.setMinOrderAmount(request.minOrderAmount());
        coupon.setExpirationDate(request.expirationDate());
        coupon.setMaxUses(request.maxUses());
    }

    public static CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getType(),
                coupon.getValue(),
                coupon.getMinOrderAmount(),
                coupon.getExpirationDate(),
                coupon.getMaxUses(),
                coupon.getTimesUsed()
        );
    }
}
