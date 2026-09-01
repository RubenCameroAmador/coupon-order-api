Feature: Discount calculation

  Background:
    Given a product "Widget" with price "10.00" and stock 100

  Scenario: PERCENTAGE discount is computed and rounded half-up
    Given a coupon "SAVE33" of type PERCENTAGE with value "33.33" minOrderAmount "0" maxUses 5 expiring in the future
    When I create an order with 3 units of "Widget" and coupon code "SAVE33"
    Then the response status is 201
    And the discount amount is "10.00"
    And the total amount is "20.00"

  Scenario: FIXED discount is computed
    Given a coupon "FIXED5" of type FIXED with value "5.00" minOrderAmount "0" maxUses 5 expiring in the future
    When I create an order with 2 units of "Widget" and coupon code "FIXED5"
    Then the response status is 201
    And the discount amount is "5.00"
    And the total amount is "15.00"

  Scenario: FIXED discount greater than subtotal is capped and total floors at zero
    Given a coupon "BIG100" of type FIXED with value "100.00" minOrderAmount "0" maxUses 5 expiring in the future
    When I create an order with 1 unit of "Widget" and coupon code "BIG100"
    Then the response status is 201
    And the discount amount is "10.00"
    And the total amount is "0.00"
