# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Spring Boot REST API (Maven, Java 21, package root `com.project.coupon_order_api`) implementing `specs/001-coupon-order-pricing/spec.md`: Product/Coupon CRUD plus Order creation with coupon-based discount pricing.

## Architecture

Layered: `controller` → `service` → `repository`, with `entity` (JPA), `dto/request` + `dto/response` (validated records, never exposing entities over the API), `mapper` (manual entity⇄DTO mapping, no MapStruct), and `exception` (`ApiException` subtypes + one `GlobalExceptionHandler` `@RestControllerAdvice` mapping every business rule to its HTTP status per spec.md).

`OrderService.createOrder` is the core of the system — it implements every business rule in spec.md (1–21) in one `@Transactional` method: resolve+lock every referenced product (`ProductRepository.findByIdForUpdate`, `PESSIMISTIC_WRITE`, locked in ascending-id order to avoid cross-order deadlocks) → check all lines have sufficient stock *before any mutation* → resolve+lock the coupon (`CouponRepository.findByCodeForUpdate`) → validate expiry/usage-limit/min-order-amount → compute discount → only then commit stock decrements, `timesUsed` increment, and the `Order` write. Order lines duplicating the same `productId` are merged (quantities summed) before any of this, so a request can never double-decrement the same locked row.

`OrderLine` denormalizes `productId`/`productName`/`unitPrice` as a snapshot rather than a `@ManyToOne` to `Product` — required so a later price change or product deletion never alters historical orders.

Tests, by layer: `service/*Test` (Mockito unit tests, one method per business rule), `controller/*Test` (`@WebMvcTest` + `MockMvc`, service mocked), `integration/*Test` (`@SpringBootTest` + `MockMvc`, real H2, nothing mocked), `service/OrderConcurrencyIntegrationTest` (real threads against real H2 — the only way to actually exercise the pessimistic locks), and `cucumber/` (Gherkin features under `src/test/resources/features/`, executable form of spec.md's Given/When/Then rules).

## Commands

Use the Maven wrapper, not a system `mvn`.

```bash
./mvnw compile                          # compile
./mvnw test                             # run all tests
./mvnw test -Dtest=ClassName            # run a single test class
./mvnw test -Dtest=ClassName#methodName # run a single test method
./mvnw spring-boot:run                  # run the app locally
./mvnw clean package                    # build the jar
```

## Dependency stack quirk

`pom.xml` pins `spring-boot-starter-parent` **4.1.1** (Spring Framework 7 line), not the 3.x line most Spring Boot docs/examples assume. This has real, confirmed API differences from 3.x — don't assume 3.x tutorials/snippets apply as-is:

- Starter artifact IDs are renamed: `spring-boot-starter-webmvc` (not `spring-boot-starter-web`), `spring-boot-h2console`, and test-scope starters `spring-boot-starter-data-jpa-test` / `spring-boot-starter-validation-test` / `spring-boot-starter-webmvc-test`. Don't "fix" these back to 3.x names.
- **Jackson 3, not 2**: `ObjectMapper`/`JsonNode`/etc. live under `tools.jackson.databind.*` (groupId `tools.jackson.core`), not `com.fasterxml.jackson.databind.*`. `JacksonException` is unchecked (extends `RuntimeException`) in Jackson 3.
- **`@WebMvcTest` / `@AutoConfigureMockMvc` moved** to `org.springframework.boot.webmvc.test.autoconfigure` (not `org.springframework.boot.test.autoconfigure.web.servlet`). `@SpringBootTest` itself is unchanged (`org.springframework.boot.test.context`).
- **`@MockBean` is gone.** Use `@MockitoBean` from `org.springframework.test.context.bean.override.mockito.MockitoBean` (spring-test, not spring-boot-test) in `@WebMvcTest`/`@SpringBootTest` classes.
- `H2` reserves the identifier `value` — an entity field literally named `value` (e.g. `Coupon.value`) needs an explicit `@Column(name = "...")` override or table creation fails at Hibernate DDL time.
- JUnit is on the **6.0.3** line (Jupiter and Platform share one major version number now) — this is newer than most third-party JUnit-5-era libraries expect. Before adding one (Cucumber, etc.), run `./mvnw dependency:tree` after adding it: Maven's dependency mediation will pull `junit-platform-*` up to the version pinned by Boot's BOM regardless of what the library's own POM asks for, which is usually fine, but confirm no version conflict/warning appears.
- Cucumber Expressions: `/` in step text is alternative-syntax and must be escaped as `\/` (e.g. `@When("I POST \\/orders with items:")`) or the whole feature file's glue fails to register with a confusing cascade of "undefined step" errors.

When adding any new Spring-ecosystem or third-party dependency, verify its version explicitly supports Spring Boot 4.x / Spring Framework 7 (or, per the JUnit note above, at least resolves cleanly via `dependency:tree`) before assuming it works — many libraries' stable releases still only target the 3.x line.

Lombok is on the classpath (`optional` scope) with the annotation processor already wired into `maven-compiler-plugin` for both `compile` and `test-compile`.

Data stack: Spring Data JPA + H2 (in-memory, `create-drop`) + Bean Validation, configured in `src/main/resources/application.properties`. H2 console at `/h2-console`.
