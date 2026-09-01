# Coupon Order Pricing — Specification

## Overview
A REST API for managing Products and Coupons, and for creating Orders that apply an optional coupon discount to a computed subtotal. Products carry price/stock; Coupons are PERCENTAGE or FIXED discounts with expiry, a minimum order amount, and a usage cap; Orders are created atomically — either the full order succeeds (stock decremented, coupon usage incremented) or nothing changes.

## Entities

### Product
| Field | Type | Constraints |
|---|---|---|
| id | Long | server-generated, read-only |
| name | String | required, not blank |
| price | BigDecimal(19,2) | required, > 0 |
| stock | Integer | required, >= 0 |

### Coupon
| Field | Type | Constraints |
|---|---|---|
| id | Long | server-generated, read-only |
| code | String | required, not blank, unique, case-sensitive |
| type | enum: `PERCENTAGE` \| `FIXED` | required |
| value | BigDecimal(19,2) | required, > 0; if `type == PERCENTAGE` then also `<= 100` |
| minOrderAmount | BigDecimal(19,2) | required, >= 0 |
| expirationDate | ISO-8601 datetime | required, must be strictly after "now" at creation/update time |
| maxUses | Integer | required, > 0 |
| timesUsed | Integer | server-managed; starts at 0; not accepted in create/update requests; read-only in responses |

### Order (created only — no update/patch endpoint)
| Field | Type | Notes |
|---|---|---|
| id | Long | server-generated |
| items | list of OrderLine, min 1 | request: `{productId, quantity>0}`; response adds `productName`, `unitPrice`, `lineSubtotal` (snapshots taken at order time — later product price changes never affect existing orders) |
| couponCode | String, nullable | request: optional; response: the applied code, or `null` if none |
| subtotal | BigDecimal(19,2) | computed, response-only — Σ(unitPrice × quantity) |
| discountAmount | BigDecimal(19,2) | computed, response-only — 0 if no coupon applied |
| totalAmount | BigDecimal(19,2) | computed, response-only — `max(subtotal - discountAmount, 0)` |
| createdAt | ISO-8601 datetime | server-generated |

### Standard error envelope (all error responses)
```json
{
  "timestamp": "2026-09-01T10:15:00",
  "status": 404,
  "error": "Not Found",
  "errorCode": "COUPON_NOT_FOUND",
  "message": "Coupon with code 'SAVE10' does not exist",
  "path": "/orders",
  "fieldErrors": null
}
```
`fieldErrors` is a `{field: message}` map, populated only for 400 request-validation failures. No response body ever includes a stack trace.

## Endpoints

### Products
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /products | `{name, price, stock}` | 201 Product | 400 validation |
| GET | /products/{id} | — | 200 Product | 404 not found |
| GET | /products | — | 200 Product[] | — |
| PUT | /products/{id} | `{name, price, stock}` (full replace) | 200 Product | 400 validation, 404 not found |
| DELETE | /products/{id} | — | 204 | 404 not found |

### Coupons
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /coupons | `{code, type, value, minOrderAmount, expirationDate, maxUses}` | 201 Coupon (timesUsed=0) | 400 validation, 409 duplicate code |
| GET | /coupons/{id} | — | 200 Coupon | 404 not found |
| GET | /coupons | — | 200 Coupon[] | — |
| PUT | /coupons/{id} | same shape as create (timesUsed preserved, not settable) | 200 Coupon | 400 validation, 404 not found, 409 code collision with another coupon |
| DELETE | /coupons/{id} | — | 204 | 404 not found, 409 if `timesUsed > 0` |

### Orders
| Method | Path | Request | Response | Errors |
|---|---|---|---|---|
| POST | /orders | `{items: [{productId, quantity}], couponCode?}` | 201 Order (full breakdown) | 400 (empty items / bad quantity / malformed body), 404 (unknown productId or couponCode), 422 (insufficient stock, coupon expired, usage limit reached, min order amount not met) |
| GET | /orders/{id} | — | 200 Order | 404 not found |
| GET | /orders | — | 200 Order[] | — |

## Business rules (Given/When/Then)

### Coupon validity

1. **Expired coupon rejected**
   Given a coupon whose `expirationDate` is before now
   When an order is created referencing that coupon's code
   Then the order is rejected with 422 and a message identifying the coupon as expired, and no product stock or `timesUsed` changes occur.

2. **Expiration boundary — equal instant still valid**
   Given a coupon whose `expirationDate` equals the current instant exactly
   When an order is created referencing it
   Then the coupon is treated as still valid (only `now > expirationDate` triggers rejection).

3. **Coupon usage limit reached**
   Given a coupon where `timesUsed >= maxUses`
   When an order is created referencing that coupon's code
   Then the order is rejected with 422 and a usage-limit message, and no side effects occur.

4. **Minimum order amount not met**
   Given a coupon with `minOrderAmount = X`
   When an order is created whose computed subtotal is less than X, referencing that coupon
   Then the order is rejected with 422, and the message states both the required minimum and the actual subtotal.

5. **Minimum order amount boundary — exact match passes**
   Given a coupon with `minOrderAmount = X`
   When an order's subtotal equals X exactly
   Then the coupon is accepted (not rejected).

### Discount calculation

6. **PERCENTAGE discount calculation**
   Given a valid, applicable coupon of type PERCENTAGE with `value = V` (0 < V ≤ 100)
   When an order is created with subtotal S and that coupon
   Then `discountAmount = round(S × V/100, 2, HALF_UP)` and `totalAmount = S - discountAmount`, which is always ≥ 0 since V ≤ 100.

7. **FIXED discount calculation**
   Given a valid, applicable coupon of type FIXED with `value = V`
   When an order is created with subtotal S and that coupon
   Then `discountAmount = min(V, S)` and `totalAmount = S - discountAmount`, always ≥ 0.

8. **FIXED discount greater than subtotal — capped, never negative**
   Given a FIXED coupon with `value = 100` and an order whose subtotal is `40.00`
   When the order is created with that coupon
   Then `discountAmount = 40.00` (capped to subtotal, not 100) and `totalAmount = 0.00`.

### Stock and order integrity

9. **Insufficient stock rejects the entire order**
   Given an order request with multiple lines, where at least one line's `quantity` exceeds that product's current `stock`
   When POST /orders is submitted
   Then the entire order is rejected with 422, **no product's stock is modified** (including lines that individually had enough stock), and no order is persisted.

10. **Non-existent coupon code**
    Given a `couponCode` that matches no existing coupon
    When an order is created referencing it
    Then the order is rejected with 404 and a clear message — never a 500.

11. **Non-existent product in an order line**
    Given a line item whose `productId` does not exist
    When POST /orders is submitted
    Then the order is rejected with 404 and a clear message, no side effects — never a 500.

12. **Successful coupon application increments usage**
    Given a valid, applicable coupon
    When an order is successfully created using it
    Then `timesUsed` is incremented by exactly 1; a rejected order attempt never increments it.

13. **No coupon provided**
    Given an order request with no `couponCode`
    When the order is created
    Then `discountAmount = 0` and `totalAmount = subtotal`.

14. **Happy path — correct end-to-end calculation**
    Given two products with known price/stock and a valid PERCENTAGE coupon meeting its `minOrderAmount`
    When an order is created with several lines and that coupon
    Then the response's `subtotal`, `discountAmount`, and `totalAmount` match the expected arithmetic exactly, each product's `stock` is decremented by its ordered quantity, and the coupon's `timesUsed` is incremented by 1.

15. **Deterministic validation order under multiple simultaneous failures**
    Given an order request where a product line has insufficient stock **and** the referenced coupon is also expired
    When POST /orders is submitted
    Then the stock failure is returned (product/line checks run before coupon checks), not the coupon failure.

### Coupon CRUD rules

16. **PERCENTAGE value capped at creation time**
    Given a request to create or update a coupon with `type = PERCENTAGE` and `value > 100`
    When POST /coupons or PUT /coupons/{id} is submitted
    Then it is rejected with 400 and a field-validation error; no coupon is created/updated.

17. **Duplicate coupon code rejected**
    Given an existing coupon with `code = "SAVE10"`
    When POST /coupons is submitted with the same code
    Then it is rejected with 409.

18. **Deleting an unused coupon succeeds**
    Given a coupon with `timesUsed == 0`
    When DELETE /coupons/{id} is submitted
    Then it succeeds with 204.

19. **Deleting a used coupon is blocked**
    Given a coupon with `timesUsed > 0`
    When DELETE /coupons/{id} is submitted
    Then it is rejected with 409, and the coupon is not deleted.

### Concurrency safety

Both the coupon row and every referenced product row are pessimistic-locked (`SELECT ... FOR UPDATE`) for the duration of order validation and commit, so `timesUsed` can never exceed `maxUses` and `stock` can never go negative under concurrent orders. A losing concurrent request receives the normal 422 (usage-limit-reached / insufficient-stock) as if it had arrived strictly after the winner — no new error code is introduced for this.

20. **Concurrent coupon redemption never exceeds maxUses**
    Given a coupon with `maxUses=2` and `timesUsed=1` (exactly one redemption slot remaining)
    When two orders both referencing that coupon's code are submitted concurrently
    Then exactly one succeeds and increments `timesUsed` to 2; the other is rejected with 422 (usage limit reached), as if it had arrived strictly after the first. `timesUsed` never exceeds `maxUses`.

21. **Concurrent orders never oversell stock**
    Given a product with `stock=1`
    When two orders, each requesting 1 unit of that product, are submitted concurrently
    Then exactly one succeeds and decrements stock to 0; the other is rejected with 422 (insufficient stock). Stock never goes negative.

## Out of scope
- Authentication, authorization, and user accounts.
- Stacking multiple coupons on one order (only a single optional `couponCode`).
- Case-insensitive or normalized coupon code matching.
- Unlimited-use coupons (`maxUses` is always required and finite).
- Editing, cancelling, or refunding an existing order; restocking on cancellation.
- Payment processing.
- Multi-currency support (a single implicit currency throughout).
- Scalability/performance under heavy write contention on a single hot coupon or product (the pessimistic lock guarantees correctness, not throughput, under concurrent redemption of the same coupon or purchase of the same product).
- Partial/PATCH updates — Product and Coupon updates are full-replace PUT only.
- Pagination, filtering, or sorting on list endpoints.
- Soft-delete or audit history for Products/Coupons.

## End-to-end verification checklist
1. `POST /products` twice → 201s with generated ids; prices/stock as sent.
2. `POST /coupons` with a PERCENTAGE coupon (`value=10`, `minOrderAmount=0`, future `expirationDate`, `maxUses=5`) → 201, `timesUsed=0`.
3. `POST /coupons` with a FIXED coupon (`value=100`) → 201.
4. `POST /orders` with valid items, no coupon → 201, `discountAmount=0`, `totalAmount=subtotal`; `GET` each product afterward confirms stock reduced by the ordered quantity.
5. `POST /orders` with valid items + the PERCENTAGE coupon → 201, discount/total match hand-computed values; `GET /coupons/{id}` shows `timesUsed` incremented by 1.
6. `POST /orders` with an order whose subtotal is well below the FIXED coupon's value → discount capped at subtotal, `totalAmount=0`.
7. `POST /orders` referencing an already-expired coupon → 422 with a clear expired message; re-`GET` the coupon confirms `timesUsed` unchanged.
8. Exhaust a coupon's `maxUses` via repeated successful orders, then attempt one more → 422 usage-limit message.
9. `POST /orders` where subtotal is below a coupon's `minOrderAmount` → 422; retry at exactly `minOrderAmount` → succeeds.
10. `POST /orders` with two lines where only the second line has insufficient stock → 422; `GET` **both** products afterward confirms neither's stock changed.
11. `POST /orders` with an unknown `couponCode` → 404, no side effects, no 500.
12. `POST /orders` with an unknown `productId` in a line → 404, no side effects, no 500.
13. `POST /coupons` with a duplicate `code` → 409.
14. `POST /coupons` with `type=PERCENTAGE, value=150` → 400 validation error.
15. `POST /coupons` with `expirationDate` in the past → 400 validation error.
16. `DELETE /coupons/{id}` on the used coupon from step 5 → 409; on a still-unused coupon → 204.
17. `GET /orders/{id}` after creation returns the same full breakdown the `POST` returned.
18. Submit a malformed JSON body / invalid enum literal → 400 with the standard error envelope; confirm no response anywhere in this checklist ever includes a stack trace.
19. Fire two concurrent `POST /orders` requests both referencing a coupon with exactly one redemption slot remaining (`maxUses - timesUsed == 1`) → exactly one returns 201 and one returns 422 (usage limit); confirm final `timesUsed == maxUses`, never more.
20. Fire two concurrent `POST /orders` requests that each request the last unit of a product with `stock=1` → exactly one returns 201 and one returns 422 (insufficient stock); confirm final stock is 0, never negative.
