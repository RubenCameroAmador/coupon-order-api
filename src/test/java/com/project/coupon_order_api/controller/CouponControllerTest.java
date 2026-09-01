package com.project.coupon_order_api.controller;

import com.project.coupon_order_api.dto.request.CouponRequest;
import com.project.coupon_order_api.dto.response.CouponResponse;
import com.project.coupon_order_api.entity.CouponType;
import com.project.coupon_order_api.exception.CouponInUseException;
import com.project.coupon_order_api.exception.CouponNotFoundException;
import com.project.coupon_order_api.exception.DuplicateCouponCodeException;
import com.project.coupon_order_api.service.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @Test
    void create_returns201() throws Exception {
        CouponResponse response = new CouponResponse(1L, "SAVE10", CouponType.PERCENTAGE, new BigDecimal("10"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 5, 0);
        when(couponService.create(any())).thenReturn(response);

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SAVE10"))
                .andExpect(jsonPath("$.timesUsed").value(0));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        when(couponService.create(any())).thenThrow(new DuplicateCouponCodeException("SAVE10"));

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_COUPON_CODE"))
                .andExpect(jsonPath("$..stackTrace").doesNotExist());
    }

    @Test
    void create_percentageAbove100_returns400() throws Exception {
        CouponRequest invalid = new CouponRequest("SAVE150", CouponType.PERCENTAGE, new BigDecimal("150"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 5);

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void create_invalidEnumLiteral_returns400() throws Exception {
        String bogusTypeBody = "{\"code\":\"SAVE10\",\"type\":\"BOGUS\",\"value\":10,"
                + "\"minOrderAmount\":0,\"expirationDate\":\"2030-01-01T00:00:00\",\"maxUses\":5}";

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bogusTypeBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$..stackTrace").doesNotExist());
    }

    @Test
    void update_returns200() throws Exception {
        CouponResponse response = new CouponResponse(1L, "SAVE20", CouponType.FIXED, new BigDecimal("5"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(2), 10, 0);
        when(couponService.update(eq(1L), any())).thenReturn(response);

        CouponRequest updateRequest = new CouponRequest("SAVE20", CouponType.FIXED, new BigDecimal("5"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(2), 10);

        mockMvc.perform(put("/coupons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE20"))
                .andExpect(jsonPath("$.type").value("FIXED"));
    }

    @Test
    void update_duplicateCode_returns409() throws Exception {
        when(couponService.update(eq(1L), any())).thenThrow(new DuplicateCouponCodeException("TAKEN"));

        CouponRequest updateRequest = new CouponRequest("TAKEN", CouponType.FIXED, new BigDecimal("5"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(2), 10);

        mockMvc.perform(put("/coupons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_COUPON_CODE"))
                .andExpect(jsonPath("$..stackTrace").doesNotExist());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(couponService.getById(99L)).thenThrow(new CouponNotFoundException(99L));

        mockMvc.perform(get("/coupons/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COUPON_NOT_FOUND"));
    }

    @Test
    void delete_unused_returns204() throws Exception {
        mockMvc.perform(delete("/coupons/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_used_returns409() throws Exception {
        doThrow(new CouponInUseException(1L)).when(couponService).delete(1L);

        mockMvc.perform(delete("/coupons/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_IN_USE"))
                .andExpect(jsonPath("$..stackTrace").doesNotExist());
    }

    private static CouponRequest validRequest() {
        return new CouponRequest("SAVE10", CouponType.PERCENTAGE, new BigDecimal("10"),
                BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 5);
    }
}
