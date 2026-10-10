# M3 — WIP (Real Persistence + Outbox Pattern)

> In-progress notes. Raw material, not polished prose — promoted to
> `M3-<short-name>.md` once the Definition of Done is verified.

## Goal
Swap the in-memory adapter for real Postgres, and implement the actual
Outbox pattern correctly.

## Definition of Done (from roadmap)
- [ ] `POST /orders` → row in `orders` + row in `outbox_events`, same TX
  (verified by killing the app mid-request — no partial state)
- [ ] Debezium picks it up → real Kafka message, zero app-level Kafka
  publish code
- [ ] Same outbox event delivered twice → no duplicate business effect

## Progress
- [ ] 1 - WIP file created
- [ ] 2 - `docker-compose.outbox.yml` — Postgres + Flyway migrations
- [ ] 3 - `JpaOrderRepository` behind the existing `OrderRepository` port
- [ ] 4 - `Order.confirm()` state transition + domain event
- [ ] 5 - Outbox write in same TX + ArchUnit rule
- [ ] 6 - First Testcontainers integration test
- [ ] 7 - Debezium + Kafka Connect + Outbox Event Router SMT
- [ ] 8 - Redelivery idempotency + kill-mid-request verification

## Decisions & trade-offs
<!-- design decisions as they're made, with alternatives considered -->
TBD

## Commands run
<!-- in order, as they're run -->
TBD

## Troubleshooting
<!-- real errors hit, how diagnosed, how fixed -->
TBD