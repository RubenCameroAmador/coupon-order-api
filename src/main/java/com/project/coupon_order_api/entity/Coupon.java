package com.project.coupon_order_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType type;

    @Column(name = "coupon_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal value;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private Integer maxUses;

    @Column(nullable = false)
    @Builder.Default
    private Integer timesUsed = 0;
}
