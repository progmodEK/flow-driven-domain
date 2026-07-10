# checkout-poc-fdd-skill

A process-centric Spring Boot application built with the **flow-driven-domain (FDD)** library
(`io.github.progmodek:flow:1.1.0`). It drives an e-commerce **checkout** from an open cart through
shipping and payment to a confirmed order, with asynchronous payment confirmation (retried on
transient gateway failures) and an abandoned-cart timeout.

Mode: **Flowable-direct** · Java 25 · Spring Boot 4.1.0 · Postgres (jsonb). This is a **Gradle
subproject** of the `flow-driven-domain` build — it has no wrapper of its own and is driven from the
repo root (`:checkout-poc-fdd-skill`).

## The process

States: `CART` (initial) → `AWAITING_SHIPPING` → `AWAITING_PAYMENT` → `PROCESSING_PAYMENT` →
`CONFIRMED`. Terminal states also include `PAYMENT_FAILED` (payment confirmation exhausted its
retries), `CANCELLED` (customer cancelled before paying) and `EXPIRED` (cart abandoned in `CART`).
`RETRY_PAYMENT` is the intermediate state the system loops through while retrying the gateway.

Actions:

| Action | Type | Transition |
|---|---|---|
| `START_CHECKOUT` | USER | `CART` → `AWAITING_SHIPPING` (validates the cart is non-empty) |
| `SET_SHIPPING` | USER | `AWAITING_SHIPPING` → `AWAITING_PAYMENT` |
| `SUBMIT_PAYMENT` | USER | `AWAITING_PAYMENT` → `PROCESSING_PAYMENT` (amount must match order total) |
| `CONFIRM_PAYMENT` | SYSTEM | `PROCESSING_PAYMENT` → `CONFIRMED`; on gateway failure → `RETRY_PAYMENT`, retried 3× every 10s, then → `PAYMENT_FAILED` |
| `CANCEL` | USER | `CART` / `AWAITING_SHIPPING` / `AWAITING_PAYMENT` → `CANCELLED` |
| `EXPIRE` | SYSTEM | `CART` → `EXPIRED` after a 60s abandoned-cart timer |

The full state machine lives in
[`src/main/resources/flow/checkout-workflow.json`](src/main/resources/flow/checkout-workflow.json).
The Java delegates in `domain/delegate/` are intentionally thin — read the JSON first to understand
the runtime behaviour.

> `CONFIRM_PAYMENT` simulates a flaky gateway (random failure) so you can watch the retry machinery
> work. Re-`GET` the checkout after submitting payment to see it move through `PROCESSING_PAYMENT` /
> `RETRY_PAYMENT` and finally to `CONFIRMED` (or `PAYMENT_FAILED`).

## Run it

From the **repo root** (`flow-driven-domain/`), reusing the repo's root `docker-compose.yml` for
Postgres:

```bash
# 1. start Postgres (root docker-compose exposes 5432)
docker compose up -d

# 2. run this module via the root Gradle build (listens on :8081, Flyway creates schema/tables)
./gradlew :checkout-poc-fdd-skill:bootRun
```

Build just this module: `./gradlew :checkout-poc-fdd-skill:build`.

## API

```bash
# create a checkout (state: CART) — capture the returned "id"
curl -s localhost:8081/checkouts -H 'Content-Type: application/json' -d '{
  "cartRef": "CART-1001",
  "items": [
    { "skuId": "SKU-1", "name": "Coffee mug",  "qty": 2, "unitPriceCents": 1500 },
    { "skuId": "SKU-2", "name": "Tea sampler", "qty": 1, "unitPriceCents": 2000 }
  ]
}'
# order total above = 2*1500 + 1*2000 = 5000 cents

ID=<paste-the-id-here>

# start checkout: CART -> AWAITING_SHIPPING
curl -s -X POST localhost:8081/checkouts/$ID/start

# set shipping: AWAITING_SHIPPING -> AWAITING_PAYMENT
curl -s -X POST localhost:8081/checkouts/$ID/shipping \
  -H 'Content-Type: application/json' -d '{ "address": "10 Rue de Rivoli, 75004 Paris" }'

# submit payment (amountCents MUST equal the order total): AWAITING_PAYMENT -> PROCESSING_PAYMENT
curl -s -X POST localhost:8081/checkouts/$ID/pay \
  -H 'Content-Type: application/json' -d '{ "paymentRef": "PAY-777", "amountCents": 5000 }'

# watch the async CONFIRM_PAYMENT fire (poll a few times): -> CONFIRMED (or PAYMENT_FAILED)
curl -s localhost:8081/checkouts/$ID

# cancel a checkout that hasn't been paid yet: -> CANCELLED
curl -s -X POST localhost:8081/checkouts/$ID/cancel
```

Leaving a fresh checkout untouched in `CART` for 60s triggers the SYSTEM `EXPIRE` action → `EXPIRED`.

## How it's wired

- `domain/` — the `Checkout` aggregate (`implements Flowable<UUID>`) with its `LineItem`s and
  business invariants (non-empty cart, positive quantities, payment amount == order total).
- `domain/flow/` — the `CheckoutAction` / `CheckoutState` / `CheckoutFlowType` enums that declare
  the process.
- `domain/delegate/` — one `ActionDelegate` bean per action (bean name matches the JSON `delegate`).
  SYSTEM delegates extend `SystemActionDelegate`; `ConfirmPaymentDelegate` throws `DelegateException`
  to drive the retry / exception transitions.
- `CheckoutConfig.java` — the `FlowRepository` (Postgres jsonb) and `FlowEngine` beans.
- `resources/flow/checkout-workflow.json` — the workflow / state machine.
- `resources/db/migration/` — Flyway migration (aggregate table + mandatory `flow_task` table).

USER actions are exposed as REST endpoints; SYSTEM actions (`CONFIRM_PAYMENT`, `EXPIRE`) fire
automatically via the framework's scheduled task consumer (timers / retries).
