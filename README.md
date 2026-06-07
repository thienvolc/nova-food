# ⭐ 100% Vibe Coded ⭐

# nova-food

`nova-food` is a Java 21 Spring Boot modular monolith for food ordering workflow reliability.

The repo is intentionally narrow. It focuses on backend depth in `order`, `payment`, `delivery`, `inventory`, `outbox`, and `idempotency` rather than broad product surface.

## Current scope

- Auth with JWT
- Restaurant and menu management
- Order lifecycle
- Mock payment flow
- Delivery assignment and completion
- Inventory decrement and restore
- Outbox-backed workflow events
- Idempotency for order creation, payment, and delivery assignment
- Admin outbox ops and replay

Removed from active CV scope:

- customer profile
- driver profile
- review
- coupon
- receipt

## Stack

- Java 21
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Spring Security + JWT
- PostgreSQL
- Kafka-ready event publishing
- H2 for fast default tests
- PostgreSQL verification profile for critical workflow checks

## Local setup

Start infrastructure:

```bash
docker compose -f nova-food/infrastructure/docker-compose.yaml up -d
```

Important ports:

- App: `8080`
- PostgreSQL local/demo port: `15432`
- Kafka: `9092`

For local demos, create an admin account after the app has started once and created tables:

```bash
docker exec -i nova-food-postgres psql -U nova_food -d nova_food <<'SQL'
INSERT INTO users (id, username, password_hash, role, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    '$2a$10$vgZrZcz62vxSbMR2Wo2SZeg9mMtdACeIIx1J3gDUPgM0qIsbNPDsa',
    'ADMIN',
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;
SQL
```

Windows-friendly helper:

```powershell
.\nova-food\scripts\seed-local-admin.ps1
```

The seeded local password is `Password123!`.

Run the app:

```bash
cd nova-food/food
./mvnw spring-boot:run
```

Base URL:

```text
http://localhost:8080
```

## Test strategy

Fast default suite:

```bash
cd nova-food/food
./mvnw test
```

Critical PostgreSQL verification:

```bash
cd nova-food/food
./mvnw "-Dpostgres.it=true" "-Dtest=Sprint11PostgresVerificationTests" test
```

The PostgreSQL verification profile also uses local port `15432`, so the same Docker Compose database is used for both app demo and focused persistence checks.

What the PostgreSQL verification covers:

- UUID mapping
- enum persistence
- timestamp persistence
- unique constraints for idempotency keys
- order creation idempotency
- payment idempotency
- delivery assignment idempotency
- outbox replay flow on PostgreSQL

## Core workflow demo

Use the request examples in `nova-food/requests`.

Recommended file order:

- `auth.http`
- `restaurant.http`
- `menu.http`
- `order-payment.http`
- `delivery.http`
- `tracking-cancellation.http`
- `search-reports.http`
- `outbox-idempotency.http`

Suggested flow:

1. Register/login a restaurant owner and a customer.
2. Admin creates a driver account.
3. Owner creates a restaurant.
4. Owner creates a menu item.
5. Customer creates an order with optional `Idempotency-Key`.
6. Customer pays with mock payment with optional `Idempotency-Key`.
7. Owner moves the order through `CONFIRMED`, `PREPARING`, and `READY_FOR_DELIVERY`.
8. Admin assigns a driver with optional `Idempotency-Key`.
9. Driver starts and completes delivery.
10. Admin checks outbox summary, failed events, and replay endpoint if needed.

Automated smoke demo helper:

```powershell
.\nova-food\scripts\run-smoke-demo.ps1
```

The script exercises the same CV flow and prints a JSON summary with:

- created restaurant, menu item, order, and delivery ids
- idempotency results for order, payment, and delivery assignment
- final order status and status-history count
- report totals
- outbox pending count before and after worker drain

## CV definition of done

- Scope stays fixed on workflow-heavy backend modules.
- Demo requests match the codebase and avoid removed domains.
- Fast H2 tests and focused PostgreSQL verification both pass.
- README explains the design tradeoffs and why the repo stops here.
- Interviewer can see one coherent backend story: synchronous workflow consistency plus async reliability.

## Evidence snapshot

- `3` idempotent write paths: order creation, mock payment, delivery assignment
- `3` outbox ops endpoints: summary, failed list, replay
- `2` verification modes: fast H2 suite and focused PostgreSQL workflow verification
- `14` automated test classes, including workflow, idempotency, outbox, pagination, and PostgreSQL coverage
- request demos cleaned to current CV scope only

## Key test suites

- `Sprint1CoreFlowTests`
- `Sprint2LifecycleDeliveryTests`
- `Sprint4TrackingCancellationTests`
- `Sprint5InventoryDriverAvailabilityTests`
- `Sprint9WorkflowHardeningTests`
- `Sprint9OutboxWorkflowTests`
- `Sprint10IdempotencyTests`
- `Sprint11OrderIdempotencyTests`
- `Sprint11OutboxOpsTests`
- `Sprint11PostgresVerificationTests`

## Architecture decisions

### Why modular monolith

- The repo optimizes for workflow consistency, not deployment topology.
- `order`, `payment`, `delivery`, and `inventory` still need strong synchronous coordination.
- A modular monolith keeps transactional boundaries explicit without premature distributed complexity.

### Why outbox instead of direct async business mutation

- Workflow state changes stay synchronous in the primary transaction.
- Asynchronous publication is isolated behind outbox records.
- Failed publication can be retried or replayed without mutating business state twice.

### Why local lock plus database constraint for idempotency

- Local lock reduces same-instance duplicate processing.
- Database unique constraints remain the final safety boundary.
- Recovery paths convert race-condition inserts into safe repeated outcomes.

### Why the repo stops here for CV

- The goal is to demonstrate backend engineering judgment, not feature breadth.
- This repo already shows:
  - transaction boundaries
  - lifecycle/state transition control
  - async reliability via outbox
  - idempotency on risky writes
  - PostgreSQL persistence verification
- The request/demo set is intentionally kept narrow so the repo remains easy to review and demo in an interview.
- Adding more product domains would dilute the signal.

## API summary

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

### Restaurant

- `POST /api/v1/restaurants`
- `GET /api/v1/restaurants`
- `GET /api/v1/restaurants/search`
- `GET /api/v1/restaurants/{restaurantId}`
- `PUT /api/v1/restaurants/{restaurantId}`
- `PATCH /api/v1/restaurants/{restaurantId}/status`

### Menu

- `POST /api/v1/restaurants/{restaurantId}/menu-items`
- `GET /api/v1/restaurants/{restaurantId}/menu-items`
- `GET /api/v1/menu-items/search`
- `PUT /api/v1/menu-items/{menuItemId}`
- `PATCH /api/v1/menu-items/{menuItemId}/availability`
- `PATCH /api/v1/menu-items/{menuItemId}/stock`
- `POST /api/v1/menu-items/{menuItemId}/stock-adjustments`

### Order

- `POST /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `GET /api/v1/orders/tracking/{trackingId}`
- `GET /api/v1/orders/my?page=0&size=20`
- `GET /api/v1/orders/restaurants/{restaurantId}?page=0&size=20`
- `GET /api/v1/orders/{orderId}/status-history`
- `PATCH /api/v1/orders/{orderId}/confirm`
- `PATCH /api/v1/orders/{orderId}/preparing`
- `PATCH /api/v1/orders/{orderId}/ready-for-delivery`
- `PATCH /api/v1/orders/{orderId}/cancel`
- `PATCH /api/v1/orders/{orderId}/restaurant-cancel`

`POST /api/v1/orders` accepts optional `Idempotency-Key`.

### Payment

- `POST /api/v1/orders/{orderId}/payments/mock`
- `GET /api/v1/orders/{orderId}/payments?page=0&size=20`

`POST /api/v1/orders/{orderId}/payments/mock` accepts optional `Idempotency-Key`.

### User admin

- `POST /api/v1/users`

### Delivery

- `POST /api/v1/orders/{orderId}/deliveries/assign`
- `GET /api/v1/deliveries/my?page=0&size=20`
- `PATCH /api/v1/deliveries/{deliveryId}/start`
- `PATCH /api/v1/deliveries/{deliveryId}/complete`
- `GET /api/v1/orders/{orderId}/deliveries`

`POST /api/v1/orders/{orderId}/deliveries/assign` accepts optional `Idempotency-Key`.

### Reports

- `GET /api/v1/admin/reports/revenue`
- `GET /api/v1/admin/reports/orders-by-status`
- `GET /api/v1/admin/reports/top-menu-items`
- `GET /api/v1/restaurants/{restaurantId}/reports/revenue`
- `GET /api/v1/restaurants/{restaurantId}/reports/orders-by-status`
- `GET /api/v1/restaurants/{restaurantId}/reports/top-menu-items`

### Outbox ops

- `GET /api/v1/admin/outbox/summary`
- `GET /api/v1/admin/outbox/failed`
- `POST /api/v1/admin/outbox/{outboxId}/replay`

## Notes

- Default tests stay on H2 for fast feedback.
- PostgreSQL verification is intentionally separated so the repo keeps a fast inner loop and still proves real-database correctness.
- Kafka remains optional for local development because outbox and publisher support `local`, `kafka`, and `hybrid` modes.
