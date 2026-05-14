# ⭐ 100% Vibe Coded ⭐

# Techora

Techora is a Java/Spring e-commerce backend showcase focused on order lifecycle design, stock reservation, payment orchestration, idempotent APIs, and reliable event publishing.

The project is built as a modular monolith so common backend concerns can be demonstrated clearly in one codebase without hiding the workflow behind service-to-service complexity.

## What This Project Demonstrates

- JWT-based authentication and role-based authorization
- Public product catalog with category filtering and keyword search
- Admin management for categories, products, low-stock review, and order operations
- Shopping cart flow with stock-aware quantity validation
- Checkout pipeline with order creation and stock reservation
- Payment lifecycle with confirm/fail transitions
- Order status history and actor-aware event tracking
- Idempotent checkout and payment confirmation/failure APIs
- Outbox pattern for reliable async event persistence
- Local-first development setup with PostgreSQL, Redis, and Kafka

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Validation
- Spring Kafka
- PostgreSQL
- Redis
- H2 for tests
- Lombok
- JWT (`jjwt`)
- Docker Compose for local infrastructure

## Repository Layout

- [`backend/`](./backend): main Spring Boot application
- [`infrastructure/docker-compose.yaml`](./infrastructure/docker-compose.yaml): local PostgreSQL, Redis, ZooKeeper, and Kafka stack

## Architecture Snapshot

Main modules inside `backend/src/main/java/com/techora/domain`:

- `auth`: register/login and JWT issuance
- `user`: user model, role handling, and current-user lookup
- `category`: category CRUD and active-category listing
- `product`: catalog, slugging, public browse/search, admin CRUD, low-stock queries
- `cart`: cart ownership, cart-item lifecycle, quantity validation
- `order`: checkout flow, order status transitions, admin/user order views, event history
- `payment`: payment creation, confirm/fail handling, payment state policy
- `inventory`: stock reservation, confirmation, release, and expiration handling
- `idempotency`: replay-safe checkout and payment actions
- `outbox`: transactional event recording and async publish workflow

High-level style:

- API layer in `api/rest`
- Shared response/error concerns in `app/...`
- Feature modules in `domain/...`
- Security, config, JWT, scheduling, and runtime wiring in `infrastructure/...`

## Core Features

### Auth and Access Control

- Register and login with JWT token issuance
- Stateless security with request filtering
- Public catalog endpoints for browsing
- Protected cart, order, and payment endpoints
- Admin-only endpoints for product, category, and order management

### Catalog and Cart

- List active categories
- Browse active products with optional category and keyword filters
- View product details by slug
- Add, update, remove, and clear cart items
- Enforce active-product and available-stock checks in cart operations

### Checkout and Order Workflow

- Convert cart items into an order with immutable order-item snapshots
- Reserve stock during checkout before payment confirmation
- Maintain explicit order status transitions such as `CREATED`, `STOCK_RESERVED`, `PAYMENT_PENDING`, `PAID`, `PAYMENT_FAILED`, and `CANCELLED`
- Record order events for user, admin, and system-driven transitions
- Expose order detail, listing, and event history APIs

### Payment and Inventory Reliability

- Create one payment per order with generated provider reference
- Confirm payment to finalize stock reduction and mark order as paid
- Fail payment to cancel the order and release reserved inventory
- Prevent duplicate checkout and duplicate payment confirmation/failure with idempotency keys
- Persist outbox events for order placement, status change, payment result, cancellation, and stock reduction

## Key API Areas

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `GET /api/v1/categories`
- `GET /api/v1/products`
- `GET /api/v1/products/{slug}`
- `GET /api/v1/cart`
- `POST /api/v1/cart/items`
- `PUT /api/v1/cart/items/{itemId}`
- `DELETE /api/v1/cart/items/{itemId}`
- `POST /api/v1/orders/checkout`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{orderId}`
- `GET /api/v1/orders/{orderId}/events`
- `POST /api/v1/payments`
- `GET /api/v1/payments/{paymentId}`
- `POST /api/v1/payments/{paymentId}/confirm`
- `POST /api/v1/payments/{paymentId}/fail`
- `POST /api/v1/admin/categories`
- `POST /api/v1/admin/products`
- `GET /api/v1/admin/products/low-stock`
- `GET /api/v1/admin/orders/{orderId}`
- `GET /api/v1/admin/orders/status-summary`
- `PUT /api/v1/admin/orders/{orderId}/status`

## Local Run

Infrastructure:

```bash
cd techora/infrastructure
docker compose up -d
```

Application:

```bash
cd techora/backend
./mvnw spring-boot:run
```

Optional profile examples:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=api
./mvnw spring-boot:run -Dspring-boot.run.profiles=worker
```

Notes:

- Default configuration uses PostgreSQL on `localhost:5432`, Redis on `localhost:6379`, and Kafka on `localhost:9092`.
- Tests run with the `test` profile and use H2.
- `application.yaml` defaults event publishing to local mode, while `api` and `worker` profiles switch event mode to Kafka.

## Demo Flows

### 1. Golden Path Checkout

- register a customer
- login as admin
- create category and product
- browse public catalog
- add product to cart
- checkout to create order and reserve stock
- create payment
- confirm payment and reduce stock

### 2. Failure and Recovery Path

- create order from cart
- create payment
- fail payment
- cancel order
- release reserved stock

### 3. Idempotency Path

- call checkout with `Idempotency-Key`
- replay the same request and receive the same order
- confirm payment with `Idempotency-Key`
- replay the same confirmation without causing an invalid duplicate transition

## Verification

From `techora/backend`:

```bash
./mvnw test
```

Representative coverage already present in the repo includes:

- API smoke test for auth, catalog, cart, checkout, and payment
- security tests for catalog/cart/order/payment access
- service tests for cart, product, order, payment, inventory reservation, outbox, and idempotency

## Tradeoffs

- Chosen architecture: modular monolith over microservices
  - easier to explain and run locally
  - still strong enough to demonstrate realistic backend workflows
- Payment integration is mock-oriented
  - enough to model lifecycle, state transitions, and idempotency
  - avoids premature integration with a real gateway
- Event-driven reliability uses outbox inside one service boundary
  - highlights consistency and async delivery concerns
  - keeps operational scope manageable for a portfolio project

## Out of Scope

- Frontend storefront or admin dashboard
- Real payment gateway integration
- Shipment, coupon, review, or recommendation systems
- Full warehouse or multi-location inventory modeling
- Production-grade observability and deployment automation

The repo is optimized for backend engineering discussion and portfolio presentation rather than for being a full commercial e-commerce product.
