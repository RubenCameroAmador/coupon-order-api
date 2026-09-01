package com.project.coupon_order_api.cucumber;

import com.project.coupon_order_api.dto.request.CouponRequest;
import com.project.coupon_order_api.entity.Coupon;
import com.project.coupon_order_api.entity.CouponType;
import com.project.coupon_order_api.repository.CouponRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class CouponSteps {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final ScenarioContext context;
    private final CouponRepository couponRepository;

    public CouponSteps(MockMvc mockMvc, ObjectMapper objectMapper, ScenarioContext context,
                        CouponRepository couponRepository) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.context = context;
        this.couponRepository = couponRepository;
    }

    @Given("a coupon {string} of type {word} with value {string} minOrderAmount {string} maxUses {int} expiring in the future")
    public void aCouponExpiringInTheFuture(String code, String type, String value, String minOrderAmount, int maxUses)
            throws Exception {
        createCoupon(code, type, value, minOrderAmount, maxUses, LocalDateTime.now().plusDays(1));
    }

    @Given("a coupon {string} of type {word} with value {string} minOrderAmount {string} maxUses {int} expiring in the past")
    public void aCouponExpiringInThePast(String code, String type, String value, String minOrderAmount, int maxUses)
            throws Exception {
        long id = createCoupon(code, type, value, minOrderAmount, maxUses, LocalDateTime.now().plusDays(1));
        Coupon coupon = couponRepository.findById(id).orElseThrow();
        coupon.setExpirationDate(LocalDateTime.now().minusDays(1));
        couponRepository.save(coupon);
    }

    @Given("the coupon {string} has already been used {int} times")
    public void theCouponHasAlreadyBeenUsedTimes(String code, int timesUsed) {
        Long id = context.couponId(code);
        Coupon coupon = couponRepository.findById(id).orElseThrow();
        coupon.setTimesUsed(timesUsed);
        couponRepository.save(coupon);
    }

    @When("I attempt to create a coupon {string} of type {word} with value {string} minOrderAmount {string} maxUses {int} expiring in the future")
    public void iAttemptToCreateACoupon(String code, String type, String value, String minOrderAmount, int maxUses)
            throws Exception {
        CouponRequest request = new CouponRequest(code, CouponType.valueOf(type), new BigDecimal(value),
                new BigDecimal(minOrderAmount), LocalDateTime.now().plusDays(1), maxUses);
        MvcResult result = mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        context.setLastResult(result);
    }

    @When("I delete the coupon {string}")
    public void iDeleteTheCoupon(String code) throws Exception {
        Long id = context.couponId(code);
        MvcResult result = mockMvc.perform(delete("/coupons/{id}", id)).andReturn();
        context.setLastResult(result);
    }

    private long createCoupon(String code, String type, String value, String minOrderAmount, int maxUses,
                               LocalDateTime expirationDate) throws Exception {
        CouponRequest request = new CouponRequest(code, CouponType.valueOf(type), new BigDecimal(value),
                new BigDecimal(minOrderAmount), expirationDate, maxUses);
        MvcResult result = mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        context.registerCoupon(code, id);
        return id;
    }
}
