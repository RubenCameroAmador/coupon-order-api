Feature: Order integrity

  Background:
    Given a product "Widget" with price "20.00" and stock 5
    And a product "Gadget" with price "15.00" and stock 1

  Scenario: Insufficient stock on any line rejects the entire order
    When I POST /orders with items:
      | product | quantity |
      | Widget  | 2        |
      | Gadget  | 3        |
    Then the response status is 422
    And the error code is "INSUFFICIENT_STOCK"
    And product "Widget" still has stock 5
    And product "Gadget" still has stock 1

  Scenario: Unknown coupon code returns 404
    When I create an order with 1 unit of "Widget" and coupon code "DOES-NOT-EXIST"
    Then the response status is 404
    And the error code is "COUPON_NOT_FOUND"

  Scenario: Unknown product in an order line returns 404
    When I create an order with 1 unit of product id 999999
    Then the response status is 404
    And the error code is "PRODUCT_NOT_FOUND"

  Scenario: No coupon provided results in zero discount
    When I create an order with 2 units of "Widget"
    Then the response status is 201
    And the discount amount is "0"
    And the total amount is "40.00"

  Scenario: Happy path - correct totals stock and usage
    Given a coupon "SAVE10" of type PERCENTAGE with value "10" minOrderAmount "0" maxUses 5 expiring in the future
    When I create an order with 2 units of "Widget" and coupon code "SAVE10"
    Then the response status is 201
    And the discount amount is "4.00"
    And the total amount is "36.00"
    And product "Widget" still has stock 3

  Scenario: Stock is checked before coupon validity
    Given a product "Scarce" with price "20.00" and stock 1
    And a coupon "EXPIREDRULE15" of type PERCENTAGE with value "10" minOrderAmount "0" maxUses 5 expiring in the past
    When I create an order with 5 units of "Scarce" and coupon code "EXPIREDRULE15"
    Then the response status is 422
    And the error code is "INSUFFICIENT_STOCK"
