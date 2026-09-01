package com.project.coupon_order_api.service;

import com.project.coupon_order_api.dto.request.CouponRequest;
import com.project.coupon_order_api.dto.response.CouponResponse;
import com.project.coupon_order_api.entity.Coupon;
import com.project.coupon_order_api.exception.CouponInUseException;
import com.project.coupon_order_api.exception.CouponNotFoundException;
import com.project.coupon_order_api.exception.DuplicateCouponCodeException;
import com.project.coupon_order_api.mapper.CouponMapper;
import com.project.coupon_order_api.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponResponse create(CouponRequest request) {
        if (couponRepository.existsByCode(request.code())) {
            throw new DuplicateCouponCodeException(request.code());
        }
        Coupon saved = couponRepository.save(CouponMapper.toEntity(request));
        return CouponMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CouponResponse getById(Long id) {
        return CouponMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getAll() {
        return couponRepository.findAll().stream().map(CouponMapper::toResponse).toList();
    }

    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = findOrThrow(id);
        if (couponRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateCouponCodeException(request.code());
        }
        CouponMapper.updateEntity(coupon, request);
        return CouponMapper.toResponse(couponRepository.save(coupon));
    }

    public void delete(Long id) {
        Coupon coupon = findOrThrow(id);
        if (coupon.getTimesUsed() > 0) {
            throw new CouponInUseException(id);
        }
        couponRepository.delete(coupon);
    }

    private Coupon findOrThrow(Long id) {
        return couponRepository.findById(id).orElseThrow(() -> new CouponNotFoundException(id));
    }
}
