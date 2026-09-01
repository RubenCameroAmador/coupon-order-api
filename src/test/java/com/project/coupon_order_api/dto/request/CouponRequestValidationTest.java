package com.project.coupon_order_api.dto.request;

import com.project.coupon_order_api.entity.CouponType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CouponRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void percentageValueAbove100_isRejected() {
        CouponRequest request = new CouponRequest(
                "SAVE150", CouponType.PERCENTAGE, new BigDecimal("150"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 10
        );

        Set<ConstraintViolation<CouponRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void percentageValueAt100_isAccepted() {
        CouponRequest request = new CouponRequest(
                "SAVE100", CouponType.PERCENTAGE, new BigDecimal("100"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 10
        );

        Set<ConstraintViolation<CouponRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void fixedValueAbove100_isAccepted() {
        CouponRequest request = new CouponRequest(
                "SAVE150FIXED", CouponType.FIXED, new BigDecimal("150"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 10
        );

        Set<ConstraintViolation<CouponRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void expirationDateInPast_isRejected() {
        CouponRequest request = new CouponRequest(
                "EXPIREDCREATE", CouponType.FIXED, new BigDecimal("10"),
                BigDecimal.ZERO, LocalDateTime.now().minusDays(1), 10
        );

        Set<ConstraintViolation<CouponRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}
