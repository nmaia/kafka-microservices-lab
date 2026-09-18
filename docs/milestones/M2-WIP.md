# M2 — order-service (Hexagonal skeleton) — WIP

**Status:** in progress (started `ef71acb`, 2026-09-09). This is scaffolding, not a
permanent doc — see `docs/WORKING-AGREEMENTS.md`'s "Cross-session continuity"
section. Once M2's Definition of Done is verified, this file is promoted
(renamed + distilled) into `docs/milestones/M2-order-service.md`.

## Goal
Prove the Hexagonal package structure, rich domain, and a real Kafka producer —
with almost no business complexity yet. (`implementation-roadmap.md`)

## What was built so far
- Hexagonal package skeleton: `domain/`, `application/`,
  `adapters/out/{kafka,persistence}`, `config/` (`ef71acb`, `aae1905`, `b6467c7`)
- `Order` aggregate — factory-method creation via `Order.create()`, validates
  `customerId`/`items`/`currency`, computes `totalAmount`, raises
  `OrderCreatedEvent`; `OrderLine` value object; `OrderCreatedEvent`/
  `DomainEvent` (sealed)
- `OrderRepository` port + `InMemoryOrderRepository` adapter
  (`ConcurrentHashMap`-backed, temporary stand-in for M3's JPA adapter)
- `DomainEventPublisher` port + `KafkaOrderEventPublisher` adapter — publishes
  `OrderCreated` Avro events via `KafkaTemplate<String, OrderCreated>`, keyed
  by `orderId`
- `OrderEventMapper` (manual domain → Avro mapping)
- `CreateOrderUseCase` + `CreateOrderCommand` — orchestrates
  `Order.create()` → `OrderRepository.save()` → publish pulled domain events
- `KafkaProducerConfig` (typed `ProducerFactory`/`KafkaTemplate` beans),
  `KafkaTopicsProperties` (typed `app.kafka.topics.*` binding)
- `application.yml` Kafka producer config: `acks=all`,
  `enable.idempotence=true`, `auto.register.schemas=false`
- `.env.example` real entries for `KAFKA_BOOTSTRAP_SERVERS`,
  `SCHEMA_REGISTRY_URL`, `ORDER_EVENTS_TOPIC`
- Spotless + PMD bound into the Maven lifecycle (`validate`/`verify` phases)
  at the parent POM; `pmd-ruleset.xml` `excludeRoots` for Avro-generated
  sources (`4327802`)

## Not yet built (open for M2)
- `OrderController` (REST) — no inbound HTTP adapter exists yet; nothing
  currently calls `CreateOrderUseCase`
- `springdoc-openapi` (Swagger UI + Scalar) — not in `order-service/pom.xml`
- Unit tests for `Order` — no `src/test/java` exists yet in `order-service`
- First ArchUnit rules for `order-service` — `sandbox-arch-rules` dependency
  is present (test scope) but unused so far

## Decisions & trade-offs
- `CreateOrderCommand` as its own record rather than a long parameter list on
  `CreateOrderUseCase.execute()` — justified by Clean Code's parameter-object
  guideline (`docs/REFERENCES.md`, `b6467c7`); also decouples the use case's
  contract from `OrderController`'s eventual REST request shape.
- `DomainEvent` sealed + pattern-matching `switch` in
  `KafkaOrderEventPublisher.publish()` instead of `instanceof` chains or a
  visitor — the compiler rejects the adapter if a new event type is added
  without a matching `case`, no default/throw branch needed.
- `OrderEventMapper` written by hand instead of MapStruct — Avro's generated
  builder pattern doesn't map cleanly onto MapStruct's constructor/setter
  inference, and there's only one mapping so far to justify the
  annotation-processor setup. Revisit once a second mapping exists
  (`docs/REFERENCES.md` flags MapStruct as a watch-for-it for M3/M4).
- `InMemoryOrderRepository` is explicitly temporary — the `OrderRepository`
  port is shaped so M3's `JpaOrderRepository` swaps in with zero change to
  the port or `CreateOrderUseCase`.
- `auto.register.schemas=false` — schemas are registered by hand via Kafka UI
  as a reviewed step, mirroring a real CI pipeline gating schema changes,
  rather than letting the producer silently register an unreviewed schema at
  runtime.
- `acks=all` and `enable.idempotence=true` set explicitly in
  `application.yml` even though they're already the Kafka 3.x client
  defaults — durability/dedup guarantees should be visible in config, not
  implied.
- PMD's `excludeRoots` (not per-violation suppression) for
  `target/generated-sources/avro` — generated code isn't hand-edited to
  satisfy a style rule, so excluding the source root is correct. Found as a
  real issue (124 violations) once Spotless/PMD were bound into the
  lifecycle, not designed preemptively.
- Spotless/PMD declared under the parent POM's `<build><plugins>` (not
  `pluginManagement`) so every child module inherits *and executes* them
  without redeclaring config; bound to the `validate` (Spotless) and
  `verify` (PMD) phases so `mvn verify` fails the build automatically on a
  violation.

## Verification
- **Not done yet:** the Kafka producer has never been exercised end-to-end.
  No message has been published to `order-events` and none has been
  consumed — `KafkaOrderEventPublisher` compiles but nothing calls it yet
  (no `OrderController`, no test/driver code either).
- Domain logic (`Order.create()` validations, total calculation) and the
  in-memory repository haven't been exercised by an automated test yet — no
  `src/test/java` exists in `order-service`.

## Commands reference
*(none yet — to be filled in as the verification steps above are actually run)*

## Troubleshooting log
*(none recorded yet)*
