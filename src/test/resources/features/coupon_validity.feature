Feature: Coupon validity

  Background:
    Given a product "Widget" with price "20.00" and stock 10

  Scenario: Expired coupon is rejected
    Given a coupon "EXPIRED10" of type PERCENTAGE with value "10" minOrderAmount "0" maxUses 5 expiring in the past
    When I create an order with 1 unit of "Widget" and coupon code "EXPIRED10"
    Then the response status is 422
    And the error code is "COUPON_EXPIRED"

  Scenario: Coupon usage limit reached is rejected
    Given a coupon "MAXED" of type FIXED with value "5" minOrderAmount "0" maxUses 1 expiring in the future
    And the coupon "MAXED" has already been used 1 times
    When I create an order with 1 unit of "Widget" and coupon code "MAXED"
    Then the response status is 422
    And the error code is "COUPON_USAGE_LIMIT_EXCEEDED"

  Scenario: Subtotal below minimum order amount is rejected
    Given a coupon "MIN100" of type FIXED with value "5" minOrderAmount "100.00" maxUses 5 expiring in the future
    When I create an order with 1 unit of "Widget" and coupon code "MIN100"
    Then the response status is 422
    And the error code is "MIN_ORDER_AMOUNT_NOT_MET"

  Scenario: Subtotal exactly equal to minimum order amount is accepted
    Given a coupon "MIN20" of type FIXED with value "5" minOrderAmount "20.00" maxUses 5 expiring in the future
    When I create an order with 1 unit of "Widget" and coupon code "MIN20"
    Then the response status is 201
