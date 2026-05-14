# Techora Ecommerce MVP Sprint Checklist

## Approved MVP Direction

Build a production-like Spring Boot Ecommerce backend using the fixed layered architecture:

- `api` for REST controllers only.
- `app` for shared response, exception, and application helpers.
- `domain/<feature>` for entities, DTOs, repositories, services, mappers, and constants.
- `infrastructure` for framework wiring, security, filters, typed properties, and external adapters.

Prioritize internal business-flow upgrades before external platform tooling. Microservices, Kubernetes,
Prometheus/Grafana, tracing, and CI/CD should come after the core ecommerce flow demonstrates
transactional consistency, domain events, idempotency, compensation, and read projections.

## Sprint 1 — Foundation + Auth Hardening

Estimated time: 2–4 hours

- [x] Normalize package naming/component scanning to the Techora backend package.
- [x] Fix missing Maven dependencies required by existing code.
- [x] Rename app/config leftovers from `pulse-chat` to `techora`.
- [x] Remove or isolate stale chat-only infrastructure that blocks compilation.
- [x] Ensure backend compiles and tests can run.
- [x] Harden authentication endpoints.
- [x] Add current-user endpoint.
- [x] Establish role-based access control foundation for upcoming admin APIs.

## Sprint 2 — Catalog MVP

Estimated time: 2–4 hours

- [x] Add category entity, DTOs, repository, mapper, and service.
- [x] Add product entity, DTOs, repository, mapper, and service.
- [x] Add public product list/detail/search endpoints.
- [x] Add admin create/update/delete product/category endpoints.
- [x] Add pagination/filtering by category/status/keyword where reasonable.

## Sprint 3 — Cart MVP

Estimated time: 2–4 hours

- [x] Add cart and cart-item aggregate.
- [x] Add view/add/update/remove/clear cart endpoints.
- [x] Calculate item subtotal and cart total server-side.
- [x] Prevent cart mutation for inactive or out-of-stock products.
- [x] Ensure cart ownership is enforced from the authenticated user.

## Sprint 4 — Checkout + Order Lifecycle

Estimated time: 3–4 hours

- [x] Add order and order-item entities.
- [x] Add order status enum and status transition rules.
- [x] Checkout authenticated user's cart into an order.
- [x] Validate product availability.
- [x] Reduce stock transactionally.
- [x] Snapshot product name, SKU, price, quantity, and subtotal.
- [x] Add user order history endpoint.
- [x] Add admin order status update endpoint.

## Sprint 5 — Payment Simulation + Admin Polish

Estimated time: 2–4 hours

- [x] Add payment entity, status enum, DTOs, repository, mapper, and service.
- [x] Add mock payment confirmation/failure endpoints.
- [x] Integrate payment result with order status transitions.
- [x] Add admin low-stock endpoint if time permits.
- [x] Add admin order status summary if time permits.

## Sprint 6 — Verification + Portfolio Documentation

Estimated time: 2–3 hours

- [x] Add service tests for auth, product, cart, checkout, and payment rules.
- [x] Add API smoke tests where feasible.
- [x] Add manual API test checklist for golden path.
- [x] Evaluate seed/demo data; keep manual setup through admin APIs for now.
- [x] Verify backend build/test commands.
- [x] Prepare PR summary with verification results.

## Sprint 7 — Domain Events + Outbox Pattern

Estimated time: 3–4 hours

- [x] Add `domain/outbox` module following the fixed layered architecture.
- [x] Add outbox event entity with aggregate type, aggregate id, event type, payload, status, retry count, timestamps, and error fields.
- [x] Define business events for order placed, stock reduced, payment confirmed, payment failed, and order cancelled.
- [x] Write outbox events inside the same transaction as checkout and payment state changes.
- [x] Add outbox publisher service for `PENDING` to `PUBLISHED` or `FAILED` processing without Kafka dependency first.
- [x] Add retry-ready fields and status transitions without adding external broker complexity yet.
- [x] Add tests proving business state and outbox records are persisted atomically.

## Sprint 8 — Order Audit Timeline

Estimated time: 2–4 hours

- [x] Add order event/audit entity with order id, event type, old status, new status, reason, metadata, actor, and timestamp.
- [x] Record audit entries for checkout, payment confirm, payment fail, admin status update, and order cancellation.
- [x] Keep audit timeline separate from outbox events: audit is query/history, outbox is integration/event delivery.
- [x] Add user-safe order event timeline endpoint.
- [x] Add admin order event timeline endpoint with richer metadata when needed.
- [x] Add tests for event ordering, ownership checks, and expected audit entries.

## Sprint 9 — Idempotency for Checkout and Payment

Estimated time: 3–4 hours

- [x] Add idempotency key entity with user id, key, request hash, response payload, status, expiration, and timestamps.
- [x] Support `Idempotency-Key` header for checkout.
- [x] Support `Idempotency-Key` header for payment confirm/fail operations.
- [x] Return cached response for the same user, key, and request hash.
- [x] Reject reused keys when request payload or operation differs.
- [x] Ensure double-submit checkout does not create duplicate orders.
- [x] Ensure repeated payment confirmation/failure is retry-safe.
- [x] Add tests for same-key replay, key conflict, and concurrent retry behavior.

## Sprint 10 — Inventory Reservation Flow

Estimated time: 3–5 hours

- [x] Add inventory reservation entity with product id, order id, quantity, status, expiration, and timestamps.
- [x] Change checkout from direct stock reduction to stock reservation where appropriate.
- [x] Confirm reservation and commit stock when payment succeeds.
- [x] Release reservation and restore available stock when payment fails or order is cancelled.
- [x] Add reservation expiration service for abandoned orders.
- [x] Separate stock availability checks from reservation commit/release operations.
- [x] Add tests for reservation success, insufficient stock, release on payment failure, and concurrent checkout.

## Sprint 11 — Saga-Lite Order Workflow

Estimated time: 4–6 hours

- [x] Expand order workflow states to represent created, stock reserved, payment pending, paid, payment failed, cancelled, fulfilling, shipped, and delivered.
- [x] Add order workflow service to orchestrate state transitions.
- [x] Move transition rules out of CRUD/update methods into explicit workflow policy/service methods.
- [x] Trigger compensation when payment fails by releasing reservation and cancelling the order.
- [x] Emit audit entries and outbox events for every workflow transition.
- [x] Add tests for happy path, payment failure rollback, invalid transition, and compensation failure handling.

## Sprint 12 — Internal CQRS Read Projections

Estimated time: 4–6 hours

- [ ] Add read model/projection tables for order summary, product inventory, and daily revenue.
- [ ] Add internal projection handlers that consume outbox events and update read models.
- [ ] Move admin dashboard-style reads to projection queries instead of direct aggregation over write models.
- [ ] Add projection rebuild service for development/demo recovery.
- [ ] Track projection processing status to avoid duplicate application.
- [ ] Add tests for projection updates after checkout, payment confirmation, payment failure, and cancellation.

## Sprint 13 — Event Processing Reliability

Estimated time: 3–5 hours

- [ ] Extend outbox processing statuses to pending, processing, published, failed, and dead letter.
- [ ] Add next attempt timestamp, retry count, max retries, and last error fields.
- [ ] Add retry/backoff policy for failed outbox events.
- [ ] Add dead-letter query endpoint for admin diagnostics.
- [ ] Add consumer idempotency so each event id is processed once.
- [ ] Add tests for duplicate event handling, retry transitions, and dead-letter escalation.

## Sprint 14 — Modular Boundary + Architecture Tests

Estimated time: 3–5 hours

- [ ] Define clear module boundaries for catalog, cart, order, inventory, payment, reporting, outbox, and audit.
- [ ] Replace unnecessary cross-domain repository access with service contracts or internal DTOs.
- [ ] Add architecture tests to prevent controllers from using repositories directly.
- [ ] Add architecture tests to prevent domain code from depending on `api`.
- [ ] Add architecture tests for package placement conventions from `SPRING_CODING_GUIDE.md`.
- [ ] Document module boundaries and allowed dependency directions.

## Sprint 15 — External Platform Tooling Last

Estimated time: 3–6 hours

- [ ] Add backend Dockerfile and production-like runtime profile.
- [ ] Add Docker Compose profile for app, PostgreSQL, Redis, and Kafka.
- [ ] Add OpenAPI/Swagger documentation for API discoverability.
- [ ] Add Prometheus/Grafana only after meaningful business metrics exist.
- [ ] Add tracing only after async event/outbox flow exists.
- [ ] Add GitHub Actions CI/CD for test, package, and optional Docker build.
- [ ] Add Kubernetes manifests only after local Docker runtime is stable.

## P0 Must-Have Feature Checklist

- [x] Stable backend foundation.
- [x] Authentication and role-based authorization.
- [x] Product catalog and category management.
- [x] Shopping cart.
- [x] Checkout and order lifecycle.
- [x] Inventory stock validation/reduction.
- [x] Payment simulation.

## P1 Should-Have Feature Checklist

- [ ] Inventory/admin stock adjustment and low-stock visibility.
- [ ] Product reviews and ratings.
- [ ] Address book and order shipping snapshot.
- [ ] Order events/outbox-lite audit.

## P2 Nice-To-Have Feature Checklist

- [ ] Wishlist/favorites.
- [ ] Coupons/discounts.
- [ ] Admin dashboard summary endpoints.
- [ ] Notification simulation.

## Sprint 1 Risks To Resolve First

- [x] Package mismatch: source path is `com/techora`, while most files declare `com.pulse.chat`.
- [x] Missing dependencies for existing imports: web, validation, JWT, Kafka, WebSocket, actuator.
- [x] Chat-project leftovers in config and infrastructure.
- [x] `mvnw` is present but not executable in the current checkout.
