package com.project.coupon_order_api.controller;

import com.project.coupon_order_api.dto.request.OrderItemRequest;
import com.project.coupon_order_api.dto.request.OrderRequest;
import com.project.coupon_order_api.dto.response.OrderLineResponse;
import com.project.coupon_order_api.dto.response.OrderResponse;
import com.project.coupon_order_api.exception.CouponExpiredException;
import com.project.coupon_order_api.exception.CouponNotFoundException;
import com.project.coupon_order_api.exception.InsufficientStockException;
import com.project.coupon_order_api.exception.ProductNotFoundException;
import com.project.coupon_order_api.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void create_returns201() throws Exception {
        OrderResponse response = new OrderResponse(
                1L,
                List.of(new OrderLineResponse(1L, "Widget", 2, new BigDecimal("20.00"), new BigDecimal("40.00"))),
                new BigDecimal("40.00"), null, BigDecimal.ZERO, new BigDecimal("40.00"), LocalDateTime.now());
        when(orderService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderRequest(List.of(new OrderItemRequest(1L, 2)), null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(40.00))
                .andExpect(jsonPath("$.totalAmount").value(40.00));
    }

    @Test
    void create_withCoupon_responseFieldIsNamedCouponCode() throws Exception {
        // spec.md's Order entity table names this response field `couponCode` (not `appliedCouponCode`)
        OrderResponse response = new OrderResponse(
                1L,
                List.of(new OrderLineResponse(1L, "Widget", 2, new BigDecimal("20.00"), new BigDecimal("40.00"))),
                new BigDecimal("40.00"), "SAVE10", new BigDecimal("4.00"), new BigDecimal("36.00"), LocalDateTime.now());
        when(orderService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderRequest(List.of(new OrderItemRequest(1L, 2)), "SAVE10"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.couponCode").value("SAVE10"))
                .andExpect(jsonPath("$.appliedCouponCode").doesNotExist());
    }

    @Test
    void create_emptyItems_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[],\"couponCode\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$..stackTrace").doesNotExist());
    }

    @Test
    void create_unknownProduct_returns404() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderRequest(List.of(new OrderItemRequest(99L, 1)), null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$..stackTrace").doesNotExist());
    }

    @Test
    void create_unknownCoupon_returns404() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new CouponNotFoundException("MISSING"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderRequest(List.of(new OrderItemRequest(1L, 1)), "MISSING"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COUPON_NOT_FOUND"));
    }

    @Test
    void create_expiredCoupon_returns422() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new CouponExpiredException("EXPIRED10"));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderRequest(List.of(new OrderItemRequest(1L, 1)), "EXPIRED10"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("COUPON_EXPIRED"))
                .andExpect(jsonPath("$..stackTrace").doesNotExist());
    }

    @Test
    void create_insufficientStock_returns422() throws Exception {
        when(orderService.createOrder(any())).thenThrow(new InsufficientStockException(1L, 5, 2));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderRequest(List.of(new OrderItemRequest(1L, 5)), null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$..stackTrace").doesNotExist());
    }

    @Test
    void getById_returns200() throws Exception {
        OrderResponse response = new OrderResponse(1L, List.of(), new BigDecimal("40.00"), null,
                BigDecimal.ZERO, new BigDecimal("40.00"), LocalDateTime.now());
        when(orderService.getOrderById(1L)).thenReturn(response);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAll_returns200() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk());
    }
}
