package com.project.coupon_order_api.integration;

import com.project.coupon_order_api.dto.request.CouponRequest;
import com.project.coupon_order_api.dto.request.OrderItemRequest;
import com.project.coupon_order_api.dto.request.OrderRequest;
import com.project.coupon_order_api.dto.request.ProductRequest;
import com.project.coupon_order_api.entity.CouponType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CouponIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullCrudLifecycle() throws Exception {
        CouponRequest createRequest = new CouponRequest("SAVE10-" + System.nanoTime(), CouponType.PERCENTAGE,
                new BigDecimal("10"), BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 5);

        String createResponse = mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timesUsed").value(0))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/coupons/{id}", id))
                .andExpect(status().isOk());

        CouponRequest updateRequest = new CouponRequest(createRequest.code(), CouponType.FIXED,
                new BigDecimal("5"), BigDecimal.ZERO, LocalDateTime.now().plusDays(2), 10);
        mockMvc.perform(put("/coupons/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FIXED"));

        mockMvc.perform(delete("/coupons/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/coupons/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        CouponRequest request = new CouponRequest("DUP-" + System.nanoTime(), CouponType.FIXED,
                new BigDecimal("5"), BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 5);

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_COUPON_CODE"));
    }

    @Test
    void delete_afterBeingUsedByAnOrder_returns409() throws Exception {
        String productResponse = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductRequest("Widget", new BigDecimal("20.00"), 10))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long productId = objectMapper.readTree(productResponse).get("id").asLong();

        String couponCode = "USEME-" + System.nanoTime();
        CouponRequest couponRequest = new CouponRequest(couponCode, CouponType.PERCENTAGE,
                new BigDecimal("10"), BigDecimal.ZERO, LocalDateTime.now().plusDays(1), 5);
        String couponResponse = mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long couponId = objectMapper.readTree(couponResponse).get("id").asLong();

        OrderRequest orderRequest = new OrderRequest(List.of(new OrderItemRequest(productId, 1)), couponCode);
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/coupons/{id}", couponId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("COUPON_IN_USE"));
    }
}
