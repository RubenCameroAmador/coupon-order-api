package com.project.coupon_order_api.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ScenarioContext {

    private final Map<String, Long> productIdsByName = new HashMap<>();
    private final Map<String, Long> couponIdsByCode = new HashMap<>();
    private MvcResult lastResult;

    public void registerProduct(String name, Long id) {
        productIdsByName.put(name, id);
    }

    public Long productId(String name) {
        return productIdsByName.get(name);
    }

    public void registerCoupon(String code, Long id) {
        couponIdsByCode.put(code, id);
    }

    public Long couponId(String code) {
        return couponIdsByCode.get(code);
    }

    public void setLastResult(MvcResult lastResult) {
        this.lastResult = lastResult;
    }

    public MvcResult getLastResult() {
        return lastResult;
    }
}
