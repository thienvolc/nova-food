# nova-food Sprint Checklist

## Decisions

- Project name: `nova-food`
- Package namespace: `com.nova.food`
- Runtime target: Java 21
- Architecture: modular monolith
- Database: PostgreSQL
- Messaging: Kafka
- Security: simple JWT
- Payment: mock adapter first
- Excluded from initial scope: Redis, WebSocket, real payment gateway, microservices, gateway, service discovery
- CV repo focus decision: reduce product-surface sprawl and prioritize backend depth in core flows (`order`, `payment`, `delivery`, `inventory`, `reporting`, infra readiness)
- Deprioritized/removal candidates for CV scope: `customer profile`, `driver profile`, `review`, `coupon`, `receipt`

## Confirmed Sprint 1 Tradeoffs

- [x] `1B` - Balanced scope: core modules, key validations, transaction table, tests, `.http` demo.
- [x] `2A` - Use UUID references between modules to keep modular boundaries simple.
- [x] `3A` - Use synchronous mock payment API first, then publish Kafka events.
- [x] `4A` - Kafka producer only in Sprint 1; no business consumers/outbox.
- [x] `5A` - Implement only `PENDING_PAYMENT`, `PAID`, `CANCELLED` rules in Sprint 1.
- [x] `6A` - Restaurant/menu read public; mutations require roles; order/payment require customer.
- [x] `7A` - Use H2 + Mockito/service tests; defer Testcontainers.

## Phase 0 - Build Readiness

- [x] Align all Java packages/imports to `com.nova.food`.
- [x] Rename application identity from `pulse-chat` to `nova-food`.
- [x] Rename local database/user/container/volume settings from `pulse` to `nova_food` / `nova-food`.
- [x] Remove Redis from application config, Docker Compose, and dependencies.
- [x] Remove WebSocket config from initial scope.
- [x] Keep Kafka config minimal and food-oriented.
- [x] Add only required dependencies for Phase 1:
  - [x] Spring Web
  - [x] Spring Data JPA
  - [x] Validation
  - [x] Spring Security
  - [x] JWT library
  - [x] PostgreSQL driver
  - [x] Kafka
  - [x] Spring Boot test
- [x] Ensure Maven wrapper is executable or document `sh ./mvnw`.
- [x] Verify Java 21 is available for local build.
- [x] Run `./mvnw test` successfully.

## Sprint 1 - Core Food Ordering Vertical Slice

### 1. Foundation/Auth hardening

- [x] Implement `domain.user`.
  - [x] `UserEntity`
  - [x] `UserRole`
  - [x] `UserRepository`
  - [x] `UserService`
- [x] Make auth endpoints compile.
  - [x] `POST /api/v1/auth/register`
  - [x] `POST /api/v1/auth/login`
- [x] Keep controllers thin and delegate business logic to services.
- [x] Keep JWT/filter/security code in infrastructure.
- [x] Keep response/error format consistent.
- [x] Add food response codes.
- [x] Allow registration for `CUSTOMER` and `RESTAURANT_OWNER`; keep `ADMIN` out of public registration.
- [x] Enable role-based method security.
- [x] Add auth service tests.

### 2. Restaurant module

- [x] Add `domain.restaurant.constant.RestaurantStatus`.
- [x] Add `domain.restaurant.entity.RestaurantEntity`.
- [x] Add request/response DTOs.
- [x] Add explicit mapper.
- [x] Add repository.
- [x] Add service with ownership validation.
- [x] Add REST controller:
  - [x] `POST /api/v1/restaurants`
  - [x] `GET /api/v1/restaurants`
  - [x] `GET /api/v1/restaurants/{id}`
  - [x] `PUT /api/v1/restaurants/{id}`
  - [x] `PATCH /api/v1/restaurants/{id}/status`
- [x] Add service tests.

### 3. Menu module

- [x] Add `domain.menu.entity.MenuItemEntity`.
- [x] Add request/response DTOs.
- [x] Add explicit mapper.
- [x] Add repository.
- [x] Add service with restaurant ownership validation.
- [x] Add REST controller:
  - [x] `POST /api/v1/restaurants/{restaurantId}/menu-items`
  - [x] `GET /api/v1/restaurants/{restaurantId}/menu-items`
  - [x] `PUT /api/v1/menu-items/{id}`
  - [x] `PATCH /api/v1/menu-items/{id}/availability`
- [x] Validate price > 0.
- [x] Add service tests.

### 4. Order module

- [x] Add `domain.order.constant.OrderStatus`.
- [x] Add `OrderEntity` and `OrderItemEntity`.
- [x] Add create/detail/list DTOs.
- [x] Add event DTO `OrderCreatedEvent`.
- [x] Add explicit mapper.
- [x] Add repositories.
- [x] Add service:
  - [x] validate customer
  - [x] validate active restaurant
  - [x] validate all menu items belong to same restaurant
  - [x] validate menu availability
  - [x] snapshot item name and price
  - [x] calculate totals with `BigDecimal`
  - [x] create order with `PENDING_PAYMENT`
- [x] Add REST controller:
  - [x] `POST /api/v1/orders`
  - [x] `GET /api/v1/orders/{id}`
  - [x] `GET /api/v1/orders/my`
- [x] Add service tests.

### 5. Mock payment module

- [x] Add `domain.payment.constant.PaymentStatus`.
- [x] Add `domain.payment.constant.PaymentMethod`.
- [x] Add `PaymentTransactionEntity`.
- [x] Add payment DTOs.
- [x] Add repository.
- [x] Add `infrastructure.adapter.payment.PaymentGateway`.
- [x] Add `infrastructure.adapter.payment.MockPaymentGateway`.
- [x] Add service:
  - [x] validate order exists
  - [x] validate customer owns order
  - [x] validate order is `PENDING_PAYMENT`
  - [x] persist payment transaction
  - [x] success marks order `PAID`
  - [x] failure leaves order `PENDING_PAYMENT`
- [x] Add REST controller:
  - [x] `POST /api/v1/orders/{orderId}/payments/mock`
  - [x] `GET /api/v1/orders/{orderId}/payments`
- [x] Add service tests.

### 6. Kafka producer event foundation

- [x] Add typed event topic properties:
  - [x] `nova.order.created`
  - [x] `nova.payment.completed`
  - [x] `nova.payment.failed`
- [x] Add `PaymentCompletedEvent`.
- [x] Add `PaymentFailedEvent`.
- [x] Add Kafka producer wrapper.
- [x] Publish `OrderCreatedEvent` after order creation.
- [x] Publish payment event after mock payment.
- [x] Keep fallback-on-kafka-error behavior for local demo.
- [x] Do not add business consumers or outbox in Sprint 1.

### 7. Docs/demo and verification

- [x] Add `nova-food/requests/auth.http`.
- [x] Add `nova-food/requests/restaurant.http`.
- [x] Add `nova-food/requests/menu.http`.
- [x] Add `nova-food/requests/order-payment.http`.
- [x] Add/update local demo README.
- [x] Verify `./mvnw test` with Java 21.
- [x] Verify Docker Compose starts PostgreSQL/Kafka.
- [x] Verify basic API demo flow.

## MVP-first direction

- [x] Complete backend business MVP before adding new infrastructure.
- [x] Keep Kafka producer foundation as-is for now; do not add Kafka consumers until MVP flows are stable.
- [x] Keep Redis, WebSocket, Elasticsearch, real payment gateway, gateway/service discovery, and microservices out of MVP scope.
- [x] Add only high-ROI backend features that make the food-delivery product feel complete.
- [x] Keep controllers as feature surfaces and domain services as business-rule owners.
- [x] Avoid technical debt by shipping one focused sprint at a time with tests and `.http` examples.

## Sprint 2 - Order Lifecycle and Basic Delivery

### Sprint 2 scope

- [x] Extend order lifecycle after payment:
  - [x] `PAID`
  - [x] `CONFIRMED`
  - [x] `PREPARING`
  - [x] `READY_FOR_DELIVERY`
  - [x] `DELIVERING`
  - [x] `COMPLETED`
- [x] Add restaurant owner order management:
  - [x] List orders by restaurant.
  - [x] Confirm paid order.
  - [x] Mark confirmed order as preparing.
  - [x] Mark preparing order as ready for delivery.
- [x] Add basic delivery module:
  - [x] `DeliveryEntity`
  - [x] `DeliveryStatus`
  - [x] Delivery DTOs.
  - [x] Delivery mapper.
  - [x] Delivery repository.
  - [x] Delivery service.
  - [x] Delivery controller.
- [x] Add driver assignment:
  - [x] Admin assigns driver to order ready for delivery.
  - [x] Validate assigned user has `DRIVER` role.
  - [x] Prevent duplicate delivery assignment for the same order.
- [x] Add driver delivery workflow:
  - [x] Driver lists assigned deliveries.
  - [x] Driver starts assigned delivery.
  - [x] Driver completes started delivery.
- [x] Add admin user support:
  - [x] Admin creates users for non-public roles such as `DRIVER` and `ADMIN`.
  - [x] Public register remains limited to `CUSTOMER` and `RESTAURANT_OWNER`.
- [x] Add Sprint 2 tests:
  - [x] Owner can progress own restaurant order through restaurant-side statuses.
  - [x] Non-owner cannot mutate another restaurant order.
  - [x] Admin can assign driver only after `READY_FOR_DELIVERY`.
  - [x] Driver can start and complete only own delivery.
  - [x] Invalid state transitions are rejected.
- [x] Update docs and `.http` demo flow.

### Sprint 2 risks and mitigations

- [x] Scope creep: keep delivery to assignment/status transitions only; no realtime location, map, refund, or routing.
- [x] Invalid state transitions: centralize lifecycle validation in services and reject non-next statuses.
- [x] Authorization gaps: owner can mutate only their restaurant orders; driver can mutate only assigned deliveries; admin-only assignment.
- [x] Demo user bootstrap: add admin-only user creation endpoint and document local admin creation path.
- [x] Payment/order conflict: restaurant workflow starts only from `PAID`; delivery assignment starts only from `READY_FOR_DELIVERY`.

## Sprint 3 MVP - Search, Filtering, Pagination, and Profiles

### Sprint 3 goals

- Make the product easier to browse and operate before adding new infrastructure.
- Add high-ROI API features found in reference projects:
  - menu search by name/description/price range
  - restaurant/menu pagination and filters
  - customer and driver profile management
- Keep implementation SQL/JPA-based; do not add Elasticsearch or Redis yet.

### Search/filter/pagination checklist

- [x] Add shared pagination response DTO:
  - [x] page
  - [x] size
  - [x] totalElements
  - [x] totalPages
  - [x] items
- [x] Extend restaurant listing:
  - [x] filter by keyword in name/address
  - [x] filter by status for admin/owner use
  - [x] sort by created date or name
  - [x] support page/size
- [x] Extend menu listing/search:
  - [x] search by item name
  - [x] search by description keyword
  - [x] filter by restaurant id
  - [x] filter by availability
  - [x] filter by price range
  - [x] support page/size
- [x] Add request validation:
  - [x] page >= 0
  - [x] size within safe max
  - [x] minPrice <= maxPrice
- [x] Add controller endpoints without duplicating business rules:
  - [x] `GET /api/v1/restaurants/search`
  - [x] `GET /api/v1/menu-items/search`
- [x] Add indexes for MVP search filters:
  - [x] restaurant name/status
  - [x] menu restaurant id
  - [x] menu availability
  - [x] menu price

### Customer profile checklist

- [x] Add `CustomerProfileEntity`.
- [x] Add customer profile DTOs.
- [x] Add mapper/repository/service.
- [x] Add endpoints:
  - [x] `POST /api/v1/customers/me/profile`
  - [x] `GET /api/v1/customers/me/profile`
  - [x] `PUT /api/v1/customers/me/profile`
- [x] Add rules:
  - [x] customer can access only own profile
  - [ ] admin can view profile by customer id if needed
  - [x] phone/address validation

### Driver profile checklist

- [x] Add `DriverProfileEntity`.
- [x] Add `DriverStatus`:
  - [x] `AVAILABLE`
  - [x] `BUSY`
  - [x] `OFFLINE`
- [x] Add driver profile DTOs.
- [x] Add mapper/repository/service.
- [x] Add endpoints:
  - [x] `POST /api/v1/drivers/me/profile`
  - [x] `GET /api/v1/drivers/me/profile`
  - [x] `PUT /api/v1/drivers/me/profile`
  - [x] `PATCH /api/v1/drivers/me/status`
  - [x] `GET /api/v1/drivers/available`
- [x] Add rules:
  - [x] driver can update only own profile/status
  - [x] admin can list available drivers
  - [ ] driver profile required before availability assignment in later sprint

### Sprint 3 acceptance criteria

- [x] Public users can search restaurants/menu with deterministic pagination metadata.
- [x] Search can return menu items by keyword and price range without Elasticsearch.
- [x] Customer can create/read/update own profile.
- [x] Driver can create/read/update own profile and status.
- [x] Admin can list available drivers.
- [x] Add service tests for search filters and profile authorization.
- [x] Add `.http` examples for search/profile flows.
- [x] Update README with Sprint 3 demo.

### Sprint 3 risk controls

- [x] Avoid query-method explosion by using clear repository queries/specifications.
- [x] Avoid leaking entity objects directly from controllers.
- [x] Keep profile data minimal; no map/location routing yet.
- [x] Cap page size to avoid expensive unbounded queries.

## Sprint 4 MVP - Tracking, Status History, and Cancellation Rules

### Sprint 4 goals

- Add product-grade order tracking and auditable lifecycle history.
- Add cancellation rules before the app has real payment/refund integration.
- Reuse the current order lifecycle; do not introduce Kafka consumers.

### Tracking checklist

- [x] Add `trackingId` to orders.
- [x] Generate stable tracking id during order creation.
- [x] Add endpoint:
  - [x] `GET /api/v1/orders/tracking/{trackingId}`
- [x] Add access rules:
  - [x] customer can track own order
  - [x] restaurant owner/admin can track restaurant order
  - [x] no public tracking unless explicitly approved
- [x] Add tracking response DTO with:
  - [x] trackingId
  - [x] orderId
  - [x] status
  - [x] restaurantId
  - [x] total
  - [x] createdAt
  - [x] updatedAt

### Status history checklist

- [x] Add `OrderStatusHistoryEntity`.
- [x] Record history on every order status change:
  - [x] `PENDING_PAYMENT`
  - [x] `PAID`
  - [x] `CONFIRMED`
  - [x] `PREPARING`
  - [x] `READY_FOR_DELIVERY`
  - [x] `DELIVERING`
  - [x] `COMPLETED`
  - [x] `CANCELLED`
- [x] Store:
  - [x] orderId
  - [x] fromStatus
  - [x] toStatus
  - [x] changedByUserId
  - [x] reason
  - [x] changedAt
- [x] Add endpoint:
  - [x] `GET /api/v1/orders/{orderId}/status-history`
- [x] Keep status transition logic centralized in `OrderService`.

### Cancellation checklist

- [x] Add cancel request DTO with reason.
- [x] Add customer cancellation endpoint:
  - [x] `PATCH /api/v1/orders/{orderId}/cancel`
- [x] Add owner/admin cancellation endpoint if needed:
  - [x] `PATCH /api/v1/orders/{orderId}/restaurant-cancel`
- [x] Add rules:
  - [x] customer can cancel own order while `PENDING_PAYMENT`
  - [x] customer can cancel paid order before `CONFIRMED`
  - [x] owner/admin can cancel before `READY_FOR_DELIVERY`
  - [x] cannot cancel `DELIVERING` or `COMPLETED`
  - [x] paid cancellation marks refund as deferred placeholder, not real refund
- [x] Add response codes for invalid cancellation windows.

### Sprint 4 acceptance criteria

- [x] Order detail includes tracking id.
- [x] Tracking endpoint returns the correct order status.
- [x] Status history records each lifecycle transition exactly once.
- [x] Valid cancellation changes order to `CANCELLED` and writes history.
- [x] Invalid cancellation returns a domain error and does not mutate order.
- [x] Add tests for tracking, history, valid cancellation, invalid cancellation.
- [x] Add `.http` examples and README demo update.

### Sprint 4 risk controls

- [x] Do not scatter status-history writes across controllers.
- [x] Do not implement real refund until payment provider exists.
- [x] Keep cancellation reason optional but bounded in length.
- [x] Avoid public tracking links unless security design changes.

## Sprint 5 MVP - Inventory and Driver Availability Assignment

### Sprint 5 goals

- Add stock control so menu/order behavior feels realistic.
- Improve delivery assignment using driver availability without map/realtime routing.
- Keep concurrency simple but safe.

### Inventory checklist

- [x] Add inventory fields to menu items:
  - [x] stockQuantity
  - [x] trackStock
  - [x] lowStockThreshold
- [x] Add owner/admin inventory endpoints:
  - [x] `PATCH /api/v1/menu-items/{menuItemId}/stock`
  - [x] `POST /api/v1/menu-items/{menuItemId}/stock-adjustments`
- [x] Add `StockAdjustmentEntity` for audit:
  - [x] menuItemId
  - [x] quantityDelta
  - [x] reason
  - [x] changedByUserId
  - [x] createdAt
- [x] Add order validation:
  - [x] reject out-of-stock items
  - [x] reject quantity greater than available stock
  - [x] skip stock checks when `trackStock=false`
- [x] Add stock mutation rule:
  - [x] decrement stock once order is paid or confirmed
  - [x] avoid double decrement on repeated transitions
  - [x] restore stock on valid cancellation before preparation if stock was decremented

### Driver availability assignment checklist

- [x] Require driver status `AVAILABLE` before assignment.
- [x] On assignment:
  - [x] delivery status becomes `ASSIGNED`
  - [x] driver status becomes `BUSY`
- [x] On delivery completion:
  - [x] delivery status becomes `COMPLETED`
  - [x] driver status becomes `AVAILABLE`
- [x] Add admin endpoint:
  - [x] `GET /api/v1/drivers/available`
- [x] Prevent assigning one busy driver to multiple active deliveries.

### Sprint 5 acceptance criteria

- [x] Order creation/payment rejects insufficient stock with a domain error.
- [x] Paid/confirmed order decrements stock exactly once.
- [x] Cancellation restores stock only when rules allow.
- [x] Admin can assign only available drivers.
- [x] Driver becomes busy after assignment and available after completion.
- [x] Add tests for stock decrement, out-of-stock, cancellation restore, busy-driver rejection.
- [x] Add `.http` examples and README demo update.

### Sprint 5 risk controls

- [x] Use transaction boundaries around stock mutation and order status changes.
- [x] Prefer repository-level locking or safe update query if oversell appears in tests.
- [x] Keep location/distance routing out of scope.
- [x] Do not add Redis locks in MVP.

## Sprint 6 MVP - Reports and Receipt/Invoice JSON

### Sprint 6 goals

- Add backend reporting/query features that are strong for CV interviews.
- Add a receipt endpoint after the order/payment/delivery lifecycle is complete.
- Keep output JSON-first; PDF generation is post-MVP.

### Reports checklist

- [x] Add report DTOs.
- [x] Add admin report endpoints:
  - [x] `GET /api/v1/admin/reports/revenue`
  - [x] `GET /api/v1/admin/reports/orders-by-status`
  - [x] `GET /api/v1/admin/reports/top-menu-items`
- [x] Add restaurant owner report endpoints:
  - [x] `GET /api/v1/restaurants/{restaurantId}/reports/revenue`
  - [x] `GET /api/v1/restaurants/{restaurantId}/reports/orders-by-status`
  - [x] `GET /api/v1/restaurants/{restaurantId}/reports/top-menu-items`
- [x] Add filters:
  - [x] fromDate
  - [x] toDate
  - [x] restaurantId where applicable
  - [x] status where applicable
- [x] Add repository aggregation queries:
  - [x] revenue by date range
  - [x] completed order count
  - [x] paid/completed amount total
  - [x] top-selling menu items by quantity and amount

### Receipt/invoice checklist

- [x] Add receipt response DTO:
  - [x] order id
  - [x] tracking id
  - [x] customer id
  - [x] restaurant id
  - [x] item snapshots
  - [x] subtotal
  - [x] total
  - [x] payment status/method
  - [x] delivery status
  - [x] completedAt
- [x] Add endpoint:
  - [x] `GET /api/v1/orders/{orderId}/receipt`
- [x] Add access rules:
  - [x] customer can get own receipt
  - [x] restaurant owner can get receipt for own restaurant order
  - [x] admin can get any receipt
- [x] Only generate receipt for paid/completed orders.

### Query/index checklist

- [x] Add indexes for common filters:
  - [x] order restaurant id
  - [x] order customer id
  - [x] order status
  - [x] order created at
  - [x] order tracking id
  - [x] payment order id
  - [x] delivery order id
  - [x] menu item restaurant id
- [x] Keep report queries deterministic for tests.

### Sprint 6 acceptance criteria

- [x] Admin can query revenue/date range across all restaurants.
- [x] Restaurant owner can query revenue only for own restaurant.
- [x] Reports return correct totals with deterministic fixture data.
- [x] Receipt endpoint returns item price snapshots, not current menu prices.
- [x] Unauthorized report/receipt access returns `ACCESS_DENIED`.
- [x] Add tests for admin reports, owner reports, receipt access, snapshot correctness.
- [x] Add `.http` examples and README demo update.

### Sprint 6 risk controls

- [x] Avoid putting aggregation logic in controllers.
- [x] Prefer JPQL/projection queries before native SQL unless needed.
- [x] Keep PDF generation out of MVP to avoid binary/file complexity.
- [x] Add date range validation to avoid unbounded heavy reports.

## Sprint 7 MVP Polish - Error Contract and Pagination

### Sprint 7 goals

- Standardize validation/error responses for all APIs.
- Add pagination to order/payment/delivery list endpoints that still return unpaged lists.
- Do not add new business modules in Sprint 7.

### Error response checklist

- [x] Review current global exception handling and response wrapper.
- [x] Standardize `BusinessException` response format:
  - [x] use `ResponseCode`
  - [x] use HTTP status from `ResponseCode`
  - [x] keep the same response wrapper across APIs
- [x] Standardize bean validation errors:
  - [x] `@Valid` request body failures
  - [x] invalid field values
  - [x] field-level error details
- [x] Standardize request parsing errors:
  - [x] invalid UUID path/query values
  - [x] invalid enum query values
  - [x] invalid date/time query values
  - [x] malformed JSON body
  - [x] missing required query parameters
- [x] Standardize security errors:
  - [x] unauthenticated requests return consistent `401`
  - [x] forbidden role/ownership requests return consistent `403`
- [x] Add only necessary shared `ResponseCode` values, avoid code explosion.

### Pagination checklist

- [x] Add pagination to customer order list:
  - [x] `GET /api/v1/orders/my`
- [x] Add pagination to restaurant order list:
  - [x] `GET /api/v1/orders/restaurants/{restaurantId}`
- [x] Add pagination to payment transaction list:
  - [x] `GET /api/v1/orders/{orderId}/payments`
- [x] Add pagination to driver delivery list:
  - [x] `GET /api/v1/deliveries/my`
- [x] Keep existing non-list detail endpoints unchanged.
- [x] Reuse existing `PageResponse` pattern.
- [x] Validate `page` and `size` consistently.

### Sprint 7 acceptance criteria

- [x] Validation errors return a deterministic API response body.
- [x] Business errors still return the intended domain `ResponseCode`.
- [x] Unauthorized and forbidden requests do not return default framework bodies.
- [x] Invalid UUID/enum/date/malformed JSON are mapped consistently.
- [x] Orders, payments, and deliveries list endpoints support pagination.
- [x] Existing API behavior remains backward-compatible except list response shape where pagination is introduced.
- [x] Add focused tests for validation, parsing, security, and paginated list behavior.
- [x] Update README and `.http` examples.

### Sprint 7 risk controls

- [x] Do not move business validation out of services.
- [x] Do not add feature behavior unrelated to error contract or pagination.
- [x] Keep controllers thin; controllers only bind pagination/query params and delegate.
- [x] Avoid broad refactors of existing exception/security config.

## Sprint 8 MVP Feature Extension - Promotion and Review

### Sprint 8 goals

- Add small, high-ROI product features after Sprint 7 polish.
- Keep implementation database-first and synchronous.
- Do not add Kafka/Redis/WebSocket/PDF/real payment in Sprint 8.

### Promotion/coupon checklist

- [x] Add coupon entity:
  - [x] code
  - [x] type: fixed amount or percentage
  - [x] value
  - [x] minimum order amount
  - [x] active window
  - [x] usage limit
  - [x] status
- [x] Add owner/admin coupon management endpoints.
- [x] Allow customer to apply coupon during order creation.
- [x] Snapshot discount amount on order.
- [x] Validate coupon status, active window, minimum order amount, and usage limit.
- [x] Keep coupon engine simple; no stacking and no complex campaign rules.

### Review/rating checklist

- [x] Add restaurant review entity:
  - [x] customer id
  - [x] restaurant id
  - [x] order id
  - [x] rating
  - [x] comment
  - [x] createdAt/updatedAt
- [x] Add customer endpoint to create/update own review after completed order.
- [x] Add public endpoint to list restaurant reviews.
- [x] Add restaurant average rating/count to restaurant response or detail response.
- [x] Prevent duplicate reviews for the same order.
- [x] Ensure only customer with completed order can review.

### Sprint 8 acceptance criteria

- [x] Customer can apply a valid coupon and order total reflects snapshot discount.
- [x] Invalid/expired/over-limit coupon is rejected with a domain `ResponseCode`.
- [x] Customer can review only after completed delivery/order.
- [x] Public restaurant review list returns deterministic pagination.
- [x] Restaurant rating aggregate is correct for deterministic fixture data.
- [x] Add tests and `.http` examples.

### Sprint 8 risk controls

- [x] Avoid overbuilding coupon rules.
- [x] Do not introduce async promotion usage tracking.
- [x] Do not recalculate historical order totals after coupon changes.
- [x] Keep rating aggregation simple and query-backed.

## Post-MVP Technology Backlog

## CV Scope Reduction Checklist

### Goal

- Reduce low-signal CRUD/product modules that dilute backend CV value.
- Keep the repo centered on backend-heavy concerns:
  - order lifecycle
  - payment flow
  - delivery assignment
  - inventory consistency
  - reporting
  - PostgreSQL/Testcontainers
  - Kafka/Redis/idempotency in later phases

### Modules to remove from active scope

- [x] Remove `customer` profile module from active roadmap and API surface.
- [x] Remove `driver` profile module except only the minimum driver availability data needed by delivery assignment.
- [x] Remove `review` module and rating aggregate from active scope.
- [x] Remove `coupon` module from active scope.
- [x] Remove `receipt` module from active scope.

### Cleanup checklist

- [x] Remove controllers/endpoints for the reduced modules.
- [x] Remove services/repositories/entities/mappers/DTOs for the reduced modules.
- [x] Remove tests tied only to the reduced modules.
- [ ] Remove `.http` examples and README sections tied only to the reduced modules.
- [ ] Remove response codes that become unused after module removal.
- [x] Re-check `OrderService`, `RestaurantService`, and `DeliveryService` for dependencies on removed modules.
- [x] Keep diffs focused; do not mix this cleanup with Kafka/Redis/PostgreSQL upgrades.

### Guardrails

- [ ] Do not remove data needed by the core flow: driver assignment/availability must still work after profile simplification.
- [ ] Do not weaken authorization boundaries while shrinking modules.
- [ ] Do not introduce new product features during this cleanup.
- [x] Finish cleanup before major Post-MVP distributed systems work to keep the baseline simpler.

Post-MVP work should be implemented after MVP business flows are stable and after Sprint 7 issues are cleaned up. Prefer one PR per technology area. Do not combine Kafka, Redis, CI, and realtime changes in one PR.

### CV-focused completion direction

#### Goal

- Keep `nova-food` distinct from `pulse-chat` by centering this repo on workflow consistency, not chat-style CQRS/realtime concerns.
- Reuse only the high-ROI implementation discipline learned from `pulse-chat`:
  - policy-oriented services
  - idempotency by design
  - outbox replay/ops thinking
  - typed config and internal ops endpoints
- Reuse the distributed workflow direction from `_3`:
  - outbox-first event publishing
  - order/payment/delivery event boundaries
  - compensation/retry thinking without forcing a full microservice split

#### Repository identity guardrails

- [ ] Keep `pulse-chat` as the repo that demonstrates realtime interaction, projections, replay, and CQRS-style read concerns.
- [ ] Keep `nova-food` as the repo that demonstrates workflow orchestration, inventory consistency, payment ordering, and fulfillment recovery.
- [ ] Adopt implementation patterns from `pulse-chat` only when they support `nova-food`'s order/payment/delivery story.
- [ ] Do not mirror `pulse-chat` package names or internal API shapes one-to-one.
- [ ] Prefer `_3`-style event boundaries over `_2`-style breadth expansion.

### ROI-first implementation order

1. [x] Sprint 9A - Core workflow hardening
   - [x] extract `OrderPolicyService` for lifecycle invariants and transition guards
   - [x] extract `InventoryPolicyService` for stock decrement/restore rules
   - [x] extract minimal `DeliveryPolicyService` for assignment/completion guards
   - [x] review database constraints/indexes for tracking id, order ownership, restaurant order listing, and active delivery checks
   - [x] add focused tests for:
     - [x] invalid order transitions
     - [x] duplicate or conflicting payment attempts
     - [x] duplicate delivery assignment
     - [x] stock restore/decrement edge cases
2. [x] Sprint 9B - Outbox-backed domain events
   - [x] add shared outbox entity/repository/service for workflow events
   - [x] append outbox records in the same transaction as key workflow changes
   - [x] first event set:
     - [x] `OrderPaid`
     - [x] `OrderReadyForDelivery`
     - [x] `DeliveryCompleted`
   - [x] add worker to publish pending outbox records
   - [x] keep publisher mode switchable:
     - [x] `local`
     - [x] `kafka`
     - [x] optional `hybrid`
3. [ ] Sprint 10 - Idempotency and retry safety
   - [ ] add idempotency key support for high-risk mutations:
     - [x] mock payment
     - [x] delivery assignment
     - [ ] optional order confirm
   - [x] add lock abstraction with local implementation first
   - [x] add unique-key-based recovery path after race conditions where practical
   - [x] define retry/backoff policy for failed outbox publishing
4. [x] Sprint 11A - Order idempotency hardening
   - [x] add `Idempotency-Key` support for `POST /api/v1/orders`
   - [x] persist normalized idempotency key on order aggregate or dedicated request record
   - [x] add unique-key-based recovery path for duplicate order submission
   - [x] reject reused idempotency key when payload meaningfully differs
   - [x] add focused tests for:
     - [x] repeated submit with same key returns same order
     - [x] repeated submit without key still creates distinct orders
     - [x] same key with different payload is rejected
5. [x] Sprint 11B - Outbox ops and replay visibility
   - [x] add internal ops endpoints for outbox metrics and failed replay
   - [x] expose workflow-oriented status snapshot:
     - [x] pending outbox count
     - [x] failed outbox count
     - [x] latest paid order time
     - [x] latest completed delivery time
   - [x] add replay endpoint for failed outbox events with safe validation
   - [x] add structured logs around order id, payment id, delivery id, event key, and replay attempts
   - [x] add focused tests for outbox metrics and replay behavior
6. [x] Sprint 11C - PostgreSQL verification and CV readiness docs
   - [x] run critical workflow tests against PostgreSQL
   - [x] verify UUID, enum, timestamp, unique constraint, and index-sensitive paths
   - [x] verify idempotency and outbox behavior on PostgreSQL rather than H2 only
   - [x] document architecture decisions and tradeoffs in README or sprint docs
   - [x] document why the repo intentionally stops at modular monolith + workflow reliability
7. [x] Sprint 12 - CV case study polish
   - [x] align `README.md` with final CV scope and repo story
   - [x] add concise CV definition of done and evidence snapshot
   - [x] remove stale `.http` files tied to removed domains
   - [x] keep only request demos that match active modules and workflow
   - [x] add dedicated request demos for search/reporting and outbox/idempotency
   - [x] lock the repo as CV-ready without expanding new product domains
8. [ ] Deferred only after the above are stable
   - [ ] CI and Docker polish
   - [ ] Redis cache/lock upgrade
   - [ ] realtime delivery tracking
   - [ ] real payment provider
   - [ ] search/indexing split
   - [ ] microservice extraction

### CV completion gate

- [x] Keep `nova-food` CV scope fixed around `auth`, `menu`, `order`, `payment`, `delivery`, `inventory`, `outbox`, and `idempotency`.
- [x] Do not add new business domains just to increase surface area.
- [x] Prioritize backend depth over product breadth.
- [x] Stop feature expansion once the repo clearly demonstrates:
  - [x] transaction boundaries and workflow state transitions
  - [x] async reliability via outbox
  - [x] retry/idempotency on high-risk writes
  - [x] database constraints, indexes, and persistence correctness
  - [x] testability on realistic flows
- [x] Finish only the minimum remaining high-ROI work before calling the repo CV-ready:
  - [x] Sprint 11A - order creation idempotency
  - [x] Sprint 11B - outbox ops/replay visibility
  - [x] Sprint 11C - PostgreSQL verification and architecture/tradeoff notes
  - [x] Sprint 12 - request/demo cleanup and case-study polish
- [x] Treat the repo as complete for CV once the above backend maturity points are done.
- [x] Reject low-ROI additions unless they strengthen the core backend story directly.

### Post-MVP guardrails

- [ ] Do not add new product domains before the workflow and outbox milestones are complete.
- [ ] Do not optimize reads with Redis or search infrastructure before write-side consistency is proven.
- [ ] Do not add gateway/discovery/microservice split just to imitate the reference repos.
- [ ] Keep each PR focused on one technology or one capability.
- [ ] Add architecture notes before introducing infrastructure dependencies.
- [ ] Add rollback/failure behavior for every async or external integration.
- [ ] Add integration tests for infrastructure behavior before relying on it.
- [ ] Keep core business state changes synchronous unless outbox/idempotency exists.
- [ ] Do not introduce Redis cache without invalidation rules.
- [ ] Do not introduce WebSocket without persisted tracking history.
- [ ] Do not split microservices before module boundaries and contracts are stable.

### Sprint 9 - Production Readiness Baseline

#### Goals

- Make the repo easy to build, test, and review by another backend team.
- Add CI and real-database integration coverage before adding more infrastructure.
- Keep business behavior unchanged.

#### Implementation checklist

- [ ] Add GitHub Actions workflow:
  - [ ] checkout
  - [ ] setup Java 21
  - [ ] Maven dependency cache
  - [ ] run `./mvnw test`
  - [ ] upload surefire reports on failure
- [ ] Add Testcontainers PostgreSQL integration test profile:
  - [ ] keep current H2 tests for fast feedback
  - [ ] add focused PostgreSQL repository/service integration tests
  - [ ] verify UUID, BigDecimal, enum, and timestamp mappings
- [ ] Add Dockerfile for backend:
  - [ ] multi-stage Maven build
  - [ ] Java 21 runtime image
  - [ ] non-root user if practical
  - [ ] expose app port
- [ ] Add environment documentation:
  - [ ] required env vars
  - [ ] local profile
  - [ ] test profile
  - [ ] PostgreSQL/Kafka config
- [ ] Add actuator health/info:
  - [ ] expose health endpoint
  - [ ] include db health
  - [ ] keep sensitive endpoints disabled
- [ ] Add request id logging:
  - [ ] request id filter
  - [ ] include request id in logs
  - [ ] return request id in response header

#### Acceptance criteria

- [ ] CI runs on pull request and fails on test failure.
- [ ] Docker image can start the backend with documented env vars.
- [ ] PostgreSQL integration tests cover critical persistence mappings.
- [ ] Health endpoint works without exposing sensitive internals.
- [ ] README includes architecture and local/prod environment instructions.

#### Risk controls

- [ ] Do not change business rules in this sprint.
- [ ] Keep Testcontainers tests focused to avoid slow CI.
- [ ] Do not expose actuator env/beans/configprops publicly.

### Kafka consumers and audit/notification

### Sprint 10 - Kafka Audit/Notification Consumers

#### Goals

- Turn existing producer events into a real async read-side capability.
- Keep consumers side-effect-light before implementing transactional outbox.
- Avoid mutating core order/payment state from Kafka consumers.

#### Implementation checklist

- [ ] Add audit/notification domain:
  - [ ] `AuditLogEntity` or `NotificationEntity`
  - [ ] Repository/service/mapper/DTO
- [ ] Add Kafka consumers:
  - [ ] Consume `nova.order.created`
  - [ ] Consume `nova.payment.completed`
  - [ ] Consume `nova.payment.failed`
- [ ] Keep consumers side-effect-light first:
  - [ ] Write audit log or notification records
  - [ ] Do not mutate core order/payment state through consumers until outbox exists
- [ ] Add idempotency for consumed events:
  - [ ] event id or deterministic event key
  - [ ] unique constraint to prevent duplicate audit records
- [ ] Configure retry/DLT:
  - [ ] finite retry attempts
  - [ ] dead-letter topic naming convention
  - [ ] log enough context for replay
- [ ] Add admin/read endpoints if useful:
  - [ ] list audit logs by order id
  - [ ] list notifications by user id
- [ ] Add Testcontainers Kafka integration tests.

#### Acceptance criteria

- [ ] Creating an order writes an audit/notification record through Kafka consumer.
- [ ] Completed/failed payment writes an audit/notification record through Kafka consumer.
- [ ] Duplicate Kafka delivery does not duplicate audit records.
- [ ] DLT behavior is documented and covered by at least one test or smoke scenario.

#### Risk controls

- [ ] Do not update order/payment state from consumers before outbox exists.
- [ ] Do not block HTTP request success on consumer processing.
- [ ] Keep consumer payload mapping explicit and version-tolerant.

### Redis

### Sprint 11 - Redis Idempotency and Rate Limit

#### Goals

- Add Redis for correctness and abuse protection before adding cache.
- Start with idempotency keys for risky write endpoints.
- Add lightweight rate limiting for auth/payment endpoints.

#### Implementation checklist

- [ ] Add Redis only after MVP query/report behavior is stable.
- [ ] Candidate use cases:
  - [ ] idempotency keys for payment/order submission
  - [ ] rate limiting
  - [ ] restaurant/menu search cache
- [ ] Define cache invalidation rules before implementation.
- [ ] Add Redis docker-compose service.
- [ ] Add Redis properties and local/test profile docs.
- [ ] Implement idempotency:
  - [ ] require optional `Idempotency-Key` header for order/payment writes
  - [ ] store request fingerprint and response summary
  - [ ] reject key reuse with different payload
  - [ ] define TTL
- [ ] Implement rate limiting:
  - [ ] login/register endpoints
  - [ ] payment endpoint
  - [ ] IP/user based keys
  - [ ] deterministic error response when limited
- [ ] Add Redis integration tests with Testcontainers.

#### Acceptance criteria

- [ ] Repeating the same idempotency key returns the same safe outcome.
- [ ] Reusing the same idempotency key with different payload is rejected.
- [ ] Rate-limited endpoint returns a consistent error response.
- [ ] App still starts without Redis in test profile unless Redis tests are selected.

#### Risk controls

- [ ] Do not cache mutable restaurant/menu data until invalidation rules are implemented.
- [ ] Do not make every endpoint depend on Redis availability.
- [ ] Keep idempotency scope limited to order creation and payment first.

### Deferred backlog - Search Cache

#### Goals

- Improve read performance for restaurant/menu discovery.
- Keep cache optional and safe to invalidate.

#### Implementation checklist

- [ ] Cache restaurant search responses by normalized query params.
- [ ] Cache menu search responses by normalized query params.
- [ ] Invalidate restaurant cache on restaurant update/status change.
- [ ] Invalidate menu cache on menu create/update/availability/stock changes.
- [ ] Add cache metrics/logging for hit/miss if practical.
- [ ] Document TTL and invalidation behavior.

#### Acceptance criteria

- [ ] Search endpoints still return correct data after create/update/status changes.
- [ ] Cache keys are deterministic and bounded.
- [ ] Cache can be disabled by config.

#### Risk controls

- [ ] Do not cache authenticated/role-sensitive responses unless key includes role scope.
- [ ] Prefer short TTL over complex invalidation if uncertain.

### Advanced deferred features

### Sprint 13 - Realtime Delivery Tracking

#### Implementation checklist

- [ ] Add delivery tracking update entity:
  - [ ] delivery id
  - [ ] driver id
  - [ ] latitude/longitude
  - [ ] status
  - [ ] createdAt
- [ ] Add driver endpoint to post tracking update.
- [ ] Add customer endpoint to read latest tracking state.
- [ ] Add WebSocket or SSE channel after DB tracking works.
- [ ] Authorize customer/driver/admin access.
- [ ] Add retention rule for tracking updates.

#### Risk controls

- [ ] Persist tracking updates before broadcasting.
- [ ] Do not rely on WebSocket as source of truth.
- [ ] Rate-limit tracking updates before production use.

### Sprint 14 - Real Payment and Refund

#### Implementation checklist

- [ ] Keep `PaymentGateway` adapter abstraction.
- [ ] Add provider-specific sandbox adapter.
- [ ] Add payment initiate endpoint if provider requires redirect/session.
- [ ] Add callback/webhook endpoint with signature verification.
- [ ] Add payment status reconciliation job or manual endpoint.
- [ ] Add refund workflow:
  - [ ] refund requested
  - [ ] refund processing
  - [ ] refunded
  - [ ] refund failed
- [ ] Store provider transaction ids and callback payload metadata safely.

#### Risk controls

- [ ] Never trust callback payload without signature verification.
- [ ] Make callbacks idempotent.
- [ ] Do not mark order paid until provider status is verified.
- [ ] Do not log sensitive payment payloads.

### Sprint 15 - OpenAPI and PDF Invoice

#### Implementation checklist

- [ ] Add OpenAPI/Swagger only if accepted for project scope.
- [ ] Annotate public DTOs/endpoints minimally.
- [ ] Generate API docs in CI if practical.
- [ ] Add PDF invoice generation after receipt JSON is stable:
  - [ ] render from receipt DTO
  - [ ] no business logic in PDF layer
  - [ ] store or stream PDF based on product decision

#### Risk controls

- [ ] Keep receipt JSON as source of truth.
- [ ] Do not duplicate invoice calculation logic in PDF generation.

### Sprint 16 - Elasticsearch and Microservices Split

#### Elasticsearch checklist

- [ ] Define search documents for restaurant/menu.
- [ ] Build indexer from database state.
- [ ] Add reindex command/job.
- [ ] Keep SQL search fallback.
- [ ] Document eventual consistency.

#### Microservices checklist

- [ ] Split only after module boundaries are stable.
- [ ] Define API/event contracts first.
- [ ] Add contract tests before extraction.
- [ ] Candidate split order:
  - [ ] auth/user
  - [ ] catalog restaurant/menu
  - [ ] order/payment
  - [ ] delivery
  - [ ] reporting
- [ ] Add API gateway/service discovery only after at least two services exist.

#### Risk controls

- [ ] Do not split just for architecture aesthetics.
- [ ] Do not introduce distributed transactions.
- [ ] Prefer modular monolith until operational complexity is justified.
