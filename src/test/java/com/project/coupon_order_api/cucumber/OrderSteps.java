package com.project.coupon_order_api.cucumber;

import com.project.coupon_order_api.dto.request.OrderItemRequest;
import com.project.coupon_order_api.dto.request.OrderRequest;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class OrderSteps {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final ScenarioContext context;

    public OrderSteps(MockMvc mockMvc, ObjectMapper objectMapper, ScenarioContext context) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.context = context;
    }

    @When("I create an order with {int} unit(s) of {string} and coupon code {string}")
    public void iCreateAnOrderWithUnitOfAndCouponCode(int quantity, String productName, String couponCode)
            throws Exception {
        submitOrder(List.of(new Entry(productName, quantity)), couponCode);
    }

    @When("I create an order with {int} unit(s) of {string}")
    public void iCreateAnOrderWithUnitOf(int quantity, String productName) throws Exception {
        submitOrder(List.of(new Entry(productName, quantity)), null);
    }

    @When("I create an order with {int} unit(s) of product id {int}")
    public void iCreateAnOrderWithUnitOfProductId(int quantity, int productId) throws Exception {
        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest((long) productId, quantity)), null);
        MvcResult result = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        context.setLastResult(result);
    }

    @When("I POST \\/orders with items:")
    public void iPostOrdersWithItems(DataTable dataTable) throws Exception {
        List<Entry> entries = dataTable.asMaps(String.class, String.class).stream()
                .map(row -> new Entry(row.get("product"), Integer.parseInt(row.get("quantity"))))
                .toList();
        submitOrder(entries, null);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        assertThat(context.getLastResult().getResponse().getStatus()).isEqualTo(expectedStatus);
    }

    @Then("the error code is {string}")
    public void theErrorCodeIs(String expectedCode) throws Exception {
        String body = context.getLastResult().getResponse().getContentAsString();
        String actual = objectMapper.readTree(body).get("errorCode").asText();
        assertThat(actual).isEqualTo(expectedCode);
    }

    @Then("the discount amount is {string}")
    public void theDiscountAmountIs(String expected) throws Exception {
        String body = context.getLastResult().getResponse().getContentAsString();
        String actual = objectMapper.readTree(body).get("discountAmount").asText();
        assertThat(new BigDecimal(actual)).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Then("the total amount is {string}")
    public void theTotalAmountIs(String expected) throws Exception {
        String body = context.getLastResult().getResponse().getContentAsString();
        String actual = objectMapper.readTree(body).get("totalAmount").asText();
        assertThat(new BigDecimal(actual)).isEqualByComparingTo(new BigDecimal(expected));
    }

    private void submitOrder(List<Entry> entries, String couponCode) throws Exception {
        List<OrderItemRequest> items = entries.stream()
                .map(e -> new OrderItemRequest(context.productId(e.productName()), e.quantity()))
                .toList();
        OrderRequest request = new OrderRequest(items, couponCode);
        MvcResult result = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        context.setLastResult(result);
    }

    private record Entry(String productName, int quantity) {
    }
}
