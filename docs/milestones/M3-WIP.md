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
- [x] 1 - WIP file created
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

## Concepts & Q&A
<!-- concepts discussed + developer questions, distilled as they happen -->

### The dual-write problem
Saving an order to Postgres and then calling `kafkaTemplate.send()` is two
writes to two independent systems — no single transaction spans both.
`@Transactional` only governs the DB connection.

| Failure | Result |
|---|---|
| DB commits, Kafka send fails | Order exists, no event — silently lost to downstream |
| DB commits, app crashes before/during send | Same |
| Send first, then DB commit fails | Phantom event for an order that doesn't exist |
| Send times out (but actually landed), DB rolled back | Phantom event |

Reordering the writes only changes which inconsistency you get.

Why the obvious fixes don't work:
- **Retry the send** — doesn't survive a crash.
- **Send in an after-commit hook** — narrows the window, doesn't close it.
- **Kafka transactions / EOS** — atomic Kafka-to-Kafka only; can't enlist a
  Postgres write.
- **XA / 2PC** — Kafka doesn't support XA; industry avoids 2PC anyway
  (couples availability, holds locks across network round-trips,
  coordinator is a failure point).

### The Transactional Outbox
Write to **one** system: insert the event as a row in `outbox_events` in
the **same local transaction** as the `orders` insert — atomic, both or
neither. A separate relay moves outbox rows to Kafka; if it crashes, the
rows are still there.

Trade-off: the relay can publish and crash before recording progress →
republishes on restart. Outbox gives **at-least-once**, not exactly-once.

Two relay styles:
1. **Polling publisher** — scheduled `SELECT ... WHERE published = false`,
   send, mark published. Simple, but polling load/latency, tricky with
   multiple instances.
2. **Log tailing (CDC)** — read the DB's transaction log; no polling, no
   flags, relay isn't app code. **M3 uses this (Debezium).**

**Q: If we only use the Outbox pattern, can it duplicate data without
idempotency?**
Yes — the outbox produces **duplicate messages**, which become **duplicate
effects** only if consumers aren't idempotent. The DB side never
duplicates (one row per event); duplicates appear in Kafka. This isn't
Outbox-specific — any at-least-once Kafka system redelivers (producer
retries after an ambiguous timeout; consumer crash/rebalance before
offset commit). So consumers must be idempotent regardless; the Outbox
just doesn't remove that need. Industry pairs it with the Idempotent
Consumer ("inbox") pattern:
- **Naturally idempotent ops** — `SET status = CONFIRMED`, upsert by key.
- **Dedup by event ID** — each outbox row has a unique ID carried with the
  message (Debezium's Outbox Event Router puts it in a header); the
  consumer records processed IDs in a `processed_events` table **in the
  same TX as the business effect** and skips IDs already seen.

Net: "event might be lost or phantom" (unfixable downstream) is traded
for "event might arrive twice" (cheaply fixable downstream).

### Change Data Capture (CDC)
Observing row-level changes in a DB and emitting each as an event.

| Approach | How | Problems |
|---|---|---|
| Query-based | Poll `WHERE updated_at > last_seen` | Misses deletes and intra-window changes, query load, clock precision |
| Trigger-based | Triggers copy changes to a shadow table | Extra write per TX, logic hidden in the DB |
| **Log-based** | Read the DB's transaction log | Needs DB config/permissions — otherwise none of the above |

Log-based is the industry standard and what Debezium does.

### Postgres WAL, logical decoding, replication slots
- **WAL (write-ahead log)** — every change hits this append-only log
  before table files; a commit is durable once its WAL record is flushed.
  Used for crash recovery and to stream replicas. Already a complete,
  ordered history — CDC is just another reader.
- **Logical decoding** — turns physical WAL ("bytes on page X") into
  logical row changes ("INSERT into `outbox_events` ..."). Needs:
  - `wal_level = logical` (default `replica` isn't enough) — one line in
    `docker-compose.outbox.yml`
  - an **output plugin** — `pgoutput`, built into Postgres 10+, Debezium's
    default; no extension to install
  - a **publication** — which tables get decoded; for us only
    `outbox_events`
- **Replication slot** — Postgres-side bookmark of a consumer's WAL
  position (LSN). Consumer resumes from it after a crash (no gaps; also
  the source of redelivery if progress wasn't confirmed yet).
  **Gotcha:** Postgres keeps WAL until every slot confirms it — an
  abandoned slot grows WAL until the disk fills. Monitor slot lag
  (relevant for M9).

Why it fits the Outbox: logical decoding emits **only committed
transactions, in commit order** — rolled-back outbox inserts never appear
(no phantoms), committed ones appear only after the order is durable, and
no `published` flag or cleanup query is needed.

## Commands run
<!-- in order, as they're run -->
TBD

## Troubleshooting
<!-- real errors hit, how diagnosed, how fixed -->
TBD