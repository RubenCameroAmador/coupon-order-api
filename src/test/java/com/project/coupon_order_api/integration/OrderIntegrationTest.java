package com.project.coupon_order_api.integration;

import com.project.coupon_order_api.dto.request.CouponRequest;
import com.project.coupon_order_api.dto.request.OrderItemRequest;
import com.project.coupon_order_api.dto.request.OrderRequest;
import com.project.coupon_order_api.dto.request.ProductRequest;
import com.project.coupon_order_api.entity.Coupon;
import com.project.coupon_order_api.entity.CouponType;
import com.project.coupon_order_api.repository.CouponRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponRepository couponRepository;

    @Test
    void createOrder_withCoupon_decrementsStockAndIncrementsUsage() throws Exception {
        long productId = createProduct("Widget", "20.00", 10);
        String couponCode = "SAVE10-" + System.nanoTime();
        long couponId = createCoupon(couponCode, CouponType.PERCENTAGE, "10", "0", 5);

        OrderRequest orderRequest = new OrderRequest(List.of(new OrderItemRequest(productId, 2)), couponCode);

        String orderResponse = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(40.00))
                .andExpect(jsonPath("$.discountAmount").value(4.00))
                .andExpect(jsonPath("$.totalAmount").value(36.00))
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(orderResponse).get("id").asLong();

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(productId));

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(8));

        mockMvc.perform(get("/coupons/{id}", couponId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timesUsed").value(1));
    }

    @Test
    void createOrder_insufficientStockOnOneLine_rejectsWholeOrder_stockUnchanged() throws Exception {
        long widgetId = createProduct("Widget", "20.00", 5);
        long gadgetId = createProduct("Gadget", "15.00", 1);

        OrderRequest orderRequest = new OrderRequest(
                List.of(new OrderItemRequest(widgetId, 2), new OrderItemRequest(gadgetId, 3)), null);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));

        mockMvc.perform(get("/products/{id}", widgetId))
                .andExpect(jsonPath("$.stock").value(5));
        mockMvc.perform(get("/products/{id}", gadgetId))
                .andExpect(jsonPath("$.stock").value(1));
    }

    @Test
    void createOrder_expiredCoupon_returns422_noSideEffects() throws Exception {
        long productId = createProduct("Widget", "20.00", 10);
        String couponCode = "EXPIRED-" + System.nanoTime();
        long couponId = createCoupon(couponCode, CouponType.PERCENTAGE, "10", "0", 5);

        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        coupon.setExpirationDate(LocalDateTime.now().minusDays(1));
        couponRepository.save(coupon);

        OrderRequest orderRequest = new OrderRequest(List.of(new OrderItemRequest(productId, 1)), couponCode);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("COUPON_EXPIRED"));

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void createOrder_unknownCoupon_returns404() throws Exception {
        long productId = createProduct("Widget", "20.00", 10);

        OrderRequest orderRequest = new OrderRequest(List.of(new OrderItemRequest(productId, 1)), "DOES-NOT-EXIST");

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COUPON_NOT_FOUND"));
    }

    @Test
    void createOrder_unknownProduct_returns404() throws Exception {
        OrderRequest orderRequest = new OrderRequest(List.of(new OrderItemRequest(999999L, 1)), null);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    private long createProduct(String name, String price, int stock) throws Exception {
        String response = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductRequest(name, new BigDecimal(price), stock))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createCoupon(String code, CouponType type, String value, String minOrderAmount, int maxUses)
            throws Exception {
        CouponRequest request = new CouponRequest(code, type, new BigDecimal(value), new BigDecimal(minOrderAmount),
                LocalDateTime.now().plusDays(1), maxUses);
        String response = mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }
}
