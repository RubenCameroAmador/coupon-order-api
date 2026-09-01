package com.project.coupon_order_api.cucumber;

import com.project.coupon_order_api.dto.request.ProductRequest;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class ProductSteps {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final ScenarioContext context;

    public ProductSteps(MockMvc mockMvc, ObjectMapper objectMapper, ScenarioContext context) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.context = context;
    }

    @Given("a product {string} with price {string} and stock {int}")
    public void aProductWithPriceAndStock(String name, String price, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductRequest(name, new BigDecimal(price), stock))))
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        context.registerProduct(name, id);
    }

    @Then("product {string} still has stock {int}")
    public void productStillHasStock(String name, int expectedStock) throws Exception {
        Long id = context.productId(name);
        MvcResult result = mockMvc.perform(get("/products/{id}", id)).andReturn();
        int actual = objectMapper.readTree(result.getResponse().getContentAsString()).get("stock").asInt();
        assertThat(actual).isEqualTo(expectedStock);
    }
}
