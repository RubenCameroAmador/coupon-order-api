Feature: Coupon CRUD rules

  Scenario: PERCENTAGE value above 100 is rejected at creation
    When I attempt to create a coupon "TOOBIG" of type PERCENTAGE with value "150" minOrderAmount "0" maxUses 5 expiring in the future
    Then the response status is 400
    And the error code is "VALIDATION_ERROR"

  Scenario: Duplicate coupon code is rejected
    Given a coupon "DUPTEST" of type FIXED with value "5" minOrderAmount "0" maxUses 5 expiring in the future
    When I attempt to create a coupon "DUPTEST" of type FIXED with value "5" minOrderAmount "0" maxUses 5 expiring in the future
    Then the response status is 409
    And the error code is "DUPLICATE_COUPON_CODE"

  Scenario: Deleting an unused coupon succeeds
    Given a coupon "UNUSED1" of type FIXED with value "5" minOrderAmount "0" maxUses 5 expiring in the future
    When I delete the coupon "UNUSED1"
    Then the response status is 204

  Scenario: Deleting a used coupon is blocked
    Given a product "Widget" with price "20.00" and stock 10
    And a coupon "USED1" of type FIXED with value "5" minOrderAmount "0" maxUses 5 expiring in the future
    And I create an order with 1 unit of "Widget" and coupon code "USED1"
    When I delete the coupon "USED1"
    Then the response status is 409
    And the error code is "COUPON_IN_USE"
