# Kafka Microservices Lab — Implementation Roadmap

Each milestone is a "Lego brick": runnable on its own, verifiable before moving on. This
means each milestone's ports/domain contracts should be stable enough that later
milestones extend or swap *implementations* behind them (e.g., M3 replacing M2's
in-memory `OrderRepository` adapter with a real JPA one) rather than requiring change to
the contract itself. It does **not** mean earlier work is frozen — if a later milestone
genuinely needs to change a previous module's contract or domain logic, that's allowed
and sometimes necessary. Do it deliberately: discuss why before changing it, then update
that earlier milestone's own doc (`docs/milestones/M<n>-*.md`) to reflect the change and
the reason, rather than letting the original doc go stale. Don't start a milestone until the previous
one is actually running and you've poked at it — that's the whole point of building it
this way instead of top-down.

**Rule of thumb for patterns**: every pattern listed below is included because it solves
a concrete problem *this milestone* has — not for coverage. If you get to a milestone and
a listed pattern doesn't feel justified by what you're actually building, skip it. The
"Why here" column is the test.

---

## M0 — Foundations (no code yet)

**Goal:** repo, build tooling, and CI skeleton exist before any service does.

- Multi-module Maven parent POM (`dependencyManagement` for shared versions)
- `sandbox-arch-rules` module (ArchUnit rules, empty ruleset for now)
- `sandbox-avro-schemas` module wired to `avro-maven-plugin`, one dummy `.avsc` to prove codegen works
- `.env.example`, `docker-compose.base.yml` skeleton (empty services list)
- `CONFIGURATION.md` documenting the env-var vs volume-mount/profile split we agreed on
- Git repo, `.gitignore` (target/, .env, docker-compose.override.yml)

**Definition of done:** `mvn clean install` succeeds on an empty multi-module tree. `docker compose -f docker-compose.base.yml config` validates.

**Patterns:** none yet — this is scaffolding, not design.

---

## M1 — Kafka Core (single broker first)

**Goal:** one broker, Schema Registry, Kafka UI running and provably working, before multi-broker complexity.

- `docker-compose.base.yml`: 1 Kafka broker (KRaft), Schema Registry, Kafka UI
- One real `.avsc` schema (`OrderCreated`) replacing the dummy from M0
- A throwaway CLI producer/consumer test (even just `kafka-console-producer`/`consumer` from inside the container) to prove the cluster works before any Java touches it

**Definition of done:** you can publish an Avro message via console producer and see it decoded correctly in Kafka UI.

**Patterns:** none — infrastructure-only milestone.

---

## M2 — First Spring Boot Service: `order-service` (Hexagonal skeleton)

**Goal:** prove the Hexagonal package structure, rich domain, and a real Kafka producer — with almost no business complexity yet.

- Package structure: `domain/`, `application/`, `adapters/in/rest`, `adapters/out/kafka`
- `Order` aggregate — rich, minimal (just `create()` for now)
- `OrderRepository` port — in-memory adapter only (no DB yet, keep this milestone small)
- `CreateOrderUseCase`
- `OrderController` (REST) → publishes `OrderCreated` Avro event to Kafka
- **API documentation**: `springdoc-openapi` wired into `order-service`, exposing
  both Swagger UI and Scalar UI over the same generated OpenAPI 3 spec.
  Documentation availability is environment-gated
  (`springdoc.api-docs.enabled`), following `CONFIGURATION.md`'s env-var
  convention — on for local/dev, off by default for anything resembling
  prod. This establishes the pattern every future REST-exposing service
  follows, not just `order-service`.
- Unit tests for `Order` (no Spring context)
- First 3–4 ArchUnit rules from `sandbox-arch-rules`: domain has no Spring/adapter dependencies

**Definition of done:** 

- `POST /orders` via curl produces a real Avro message, visible and schema-valid in Kafka UI.
- Both `/swagger-ui.html` and `/scalar` render the live OpenAPI spec for
    `order-service`'s endpoints, and toggling `springdoc.api-docs.enabled=false`
    actually disables both.

| Pattern | Why here |
|---|---|
| **Adapter (GoF) / Ports & Adapters** | This *is* the milestone — `OrderRepository` port with an in-memory adapter proves the boundary works before a real DB adds noise. |
| **Factory Method** | `Order.create(...)` as a named static factory instead of a public constructor — enforces that an `Order` can't exist in an invalid initial state, consistent with the rich-domain discussion. |

---

## M3 — Real Persistence + Outbox Pattern

**Goal:** swap the in-memory adapter for real Postgres, and implement the actual Outbox pattern correctly.

- `docker-compose.outbox.yml`: Postgres, Flyway migrations (`orders` table, `outbox_events` table)
- `JpaOrderRepository` adapter implementing the same `OrderRepository` port from M2 — **the port doesn't change**, only the adapter
- *(Watch for it)* If mapping between `Order` (domain) and its JPA entity representation gets tedious/repetitive by hand, this is a natural point to introduce **MapStruct** — see `docs/REFERENCES.md`. Don't add it preemptively if the mapping stays trivial.
- `Order.confirm()` — a real state transition, producing a domain event internally
- Outbox write happens in the *same transaction* as the aggregate save
- First Testcontainers integration test: real Postgres, assert outbox row exists in the same commit
- Debezium + Kafka Connect container, Outbox Event Router SMT configured
- ArchUnit rule: nothing outside `domain`/`application` may write to the outbox repository

**Definition of done:** 

- `POST /orders` → row in `orders` + row in `outbox_events` (same TX, verified by killing the app mid-request and confirming no partial state) → Debezium picks it up → real Kafka message, with zero application-level Kafka publish code involved.
- Publishing the same outbox event twice (simulate a Debezium redelivery) doesn't create a duplicate business effect.

| Pattern | Why here |
|---|---|
| **Transactional Outbox** (microservices pattern) | The actual subject of this milestone — solves the dual-write problem between DB and Kafka. |
| **Repository (DDD/GoF-adjacent)** | `OrderRepository` port formalized properly now that there's a real persistence concern behind it. |
| **Observer (conceptual)** | `Order.confirm()` producing a domain event is the Observer idea applied inside the aggregate — state change notifies interested parties (the outbox) without the aggregate knowing who's listening. |

**Note:** swapping M2's in-memory adapter for M3's JPA adapter with *zero change to the port or the use case* is worth pausing on deliberately — that's Hexagonal actually paying off, not just theory.

---

## M4 — CQRS Split

**Goal:** a second, independently-scaled consumer projecting into a differently-shaped store.

- `docker-compose.cqrs.yml`: MongoDB, `query-api` (thin, anemic read models — no rich domain here, as discussed)
- `query-api` consumes `order-events`, projects into denormalized Mongo documents
- *(Watch for it)* Same MapStruct consideration as M3, now for Mongo document ↔ read DTO shapes — likely a stronger candidate here given the denormalized document shape differs more from the source event than a straightforward JPA entity would.
- Scale `query-api` to 3 replicas; observe partition assignment in Kafka UI
- Awaitility-based integration test: write via `order-service`, assert Mongo reflects it within a timeout (proving eventual consistency, not pretending it's instant)
- Redis: cache-aside on `query-api` reads, plus event-driven invalidation (consumer listens for updates, evicts key)

**Definition of done:** `POST /orders` (write side) → visible via `GET /orders/{id}` (read side, from Mongo) within a few hundred ms, with cache hit/miss visibly logged.

| Pattern | Why here |
|---|---|
| **CQRS** (microservices pattern) | The subject of the milestone. |
| **Strategy** | Cache strategy (cache-aside vs write-through) implemented as a swappable `CacheStrategy` interface — genuinely useful here since you explicitly want to compare strategies later, not just pick one forever. |
| **Materialized View** (microservices pattern) | What the Mongo read model actually *is* — worth naming explicitly, it's the formal term for what `query-api` builds. |

---

## M5 — DLQ + Resilience

**Goal:** deliberately break things and prove failure handling works.

- `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` on `query-api`'s consumer
- Retryable vs non-retryable classification (deserialization failure → straight to DLQ; transient DB error → retry with backoff)
- Manual DLQ reprocessor endpoint
- Resilience4j circuit breaker on any outbound HTTP call that exists by now (none yet structurally, but wire the dependency and a health-check style breaker to prove config works — real use comes in M7)
- Integration test: publish a deliberately malformed Avro payload, assert it lands in `.DLT` with exception headers intact

**Definition of done:** 

- You can watch a bad message route itself to the DLQ topic in Kafka UI, then replay it successfully via your reprocessor endpoint.
- Replaying an already-successfully-processed message via the manual DLQ reprocessor doesn't double-apply its effect.

| Pattern | Why here |
|---|---|
| **Dead Letter Channel** (microservices/EIP pattern) | The subject of the milestone. |
| **Circuit Breaker** (microservices pattern) | Prevents a struggling downstream from being hammered by retries — pairs naturally with DLQ's "give up gracefully" philosophy. |
| **Retry with backoff** (stability pattern) | Distinguishing retryable failures is the core design decision here, not just a config toggle. |

---

## M6 — Multi-Broker + Gateway/Load Balancing

**Goal:** graduate from 1 broker to 3, and put a real entry point in front of everything.

- `docker-compose.multibroker.yml` overlay: 3-broker KRaft cluster, RF=3, min ISR=2
- Recreate topics with 6 partitions, replication factor 3
- Kill a broker mid-flow, observe leader election in Kafka UI, confirm zero message loss with `acks=all`
- Spring Cloud Gateway: routes to `order-service`, `command-api`/`query-api`
- Spring Cloud LoadBalancer across `query-api` replicas
- Resilience4j circuit breaker now used for real, at the Gateway route level

**Definition of done:** requests through the Gateway load-balance visibly across `query-api` replicas (check logs/traces), and a killed broker doesn't lose in-flight messages.

| Pattern | Why here |
|---|---|
| **API Gateway** (microservices pattern) | The subject of the milestone — single entry point, routing, cross-cutting concerns (LB, circuit breaking) centralized. |
| **Load Balancer** (microservices pattern) | Same. |
| **Facade (GoF, conceptual)** | The Gateway is structurally a Facade over multiple backend services from the client's point of view — worth naming since it clarifies *why* clients shouldn't need to know service topology. |

---

## M7 — SAGA (both flavors)

**Goal:** multi-service coordination — the most structurally demanding milestone, deliberately last among the "core" patterns since it reuses everything before it.

- `payment-service`, `inventory-service`, each own Postgres, each Hexagonal, each with a rich aggregate (`Payment`, `Inventory`) with real state machines
- **Choreography first**: services react to each other's events directly via Kafka — build and test this completely before touching orchestration
- Deliberately fail a step (e.g., `InventoryReserved` → `PaymentFailed` triggers a compensating `InventoryReleased`) — this is the actual test of the milestone, not the happy path
- **Then orchestration**: `saga-orchestrator` service, explicit state machine, drives `payment-service`/`inventory-service` rather than letting them react to each other
- Compare the two: trace a failure through Seq for both flavors, see the difference in visibility/debuggability directly rather than just reading about it

**Definition of done:**

- You can trigger a payment failure and watch the full compensating transaction complete correctly, in both choreography and orchestration modes, with a trace in Seq showing the whole chain.
- Redelivering an already-processed SAGA step event (in both choreography and orchestration modes) doesn't trigger a duplicate payment/reservation/compensation.

| Pattern | Why here |
|---|---|
| **Saga** (microservices pattern) | The subject of the milestone, both variants. |
| **State (GoF)** | `Payment`/`Inventory` aggregates as explicit state machines — a `State` pattern implementation (or a simpler enum-driven equivalent) makes illegal transitions structurally impossible, tying back to the rich-domain discussion. |
| **Command (GoF)** | The orchestrator issuing explicit commands (`ReservePaymentCommand`) to each service is a clean fit for Command — makes the orchestration flavor's intent explicit and testable in isolation from Kafka. |
| **Mediator (GoF, conceptual)** | `saga-orchestrator` is structurally a Mediator — services don't coordinate with each other directly, they go through it. Worth naming as the conceptual link between the GoF pattern and the microservices pattern. |

---

## M8 — Boundary Integrations (Webhooks, SMTP, FTP)

**Goal:** prove the Hexagonal boundary works for non-Kafka triggers and side effects — the batch-to-stream bridge is the most valuable single proof point here.

- `notification-service`: consumes events, fans out to Mailpit (email) and a mock webhook receiver
- Webhook retry/backoff via Resilience4j (reuse from M5, now against a real HTTP call)
- `batch-ingestion-service`: SFTP polling via Spring Integration, parses a batch file, publishes one event per row through the *same* `CreateOrderUseCase` port that the REST controller uses in M2
- Inbound webhook route on the Gateway, publishing to Kafka

**Definition of done:** drop a CSV into the SFTP container and watch it flow through Outbox → CQRS → SAGA exactly like an order created via REST — proving the domain doesn't care where a command came from.

| Pattern | Why here |
|---|---|
| **Adapter (GoF)** | The batch file parser and the REST controller are two different *inbound adapters* calling the same use case — this is the cleanest demonstration of the pattern in the whole project. |
| **Template Method** | If you end up with multiple file formats to ingest later, a `BatchFileProcessor` template (parse → validate → map → publish, with format-specific parse/map steps overridden) is a natural fit — only add this if/when a second file format actually shows up, not preemptively. |

---

## M9 — Observability

**Goal:** wire OpenTelemetry across everything built so far — deliberately late, so there's a real distributed flow (the SAGA from M7) worth tracing.

- `docker-compose.observability.yml`: OTel Collector, Seq
- `opentelemetry-spring-boot-starter` on every service, `OTEL_EXPORTER_OTLP_ENDPOINT` env var
- Verify Kafka header trace propagation across the SAGA chain — one trace, multiple services, visible in Seq
- Structured JSON logs routed through the same pipe

**Definition of done:** trigger the M7 SAGA failure scenario, pull up Seq, see one connected trace spanning `order-service → Kafka → payment-service → Kafka → inventory-service` with the compensating transaction visible.

**Patterns:** none new — this milestone is about *seeing* the patterns you already built, not introducing more. That's deliberate: observability's value only shows up once there's real complexity to observe.

---

## M10 — Architecture Tests Hardening

**Goal:** go back and make the `sandbox-arch-rules` module actually comprehensive, now that real violations are possible across a real codebase.

- Full Hexagonal layer rules (domain/application/adapters) across every service
- Bounded context isolation rules
- Outbox-write isolation rule
- Naming convention rules (`*UseCase`, `*Controller` location)
- Wire the shared module into every service's test suite

**Definition of done:** deliberately introduce one violation (e.g., import `KafkaTemplate` into `Order`) and watch the build fail.

**Patterns:** none new — this milestone is testing the *presence* of the patterns above, not adding more.

---

## Parked for a later phase (by design, not oversight)

- **Auth** (JWT/OAuth2/RBAC via Keycloak) — cross-cutting, added once there's a real Gateway and real services to protect
- **Frontend** — pure consumer of the Gateway API, zero backend impact either way

---

## Full pattern reference (where each one lives)

| Pattern | Category | Milestone | Role |
|---|---|---|---|
| Adapter | GoF / Hexagonal | M2, M8 | Port/adapter boundary; multiple inbound adapters (REST, batch) sharing one use case |
| Factory Method | GoF | M2 | Enforced valid aggregate creation |
| Repository | DDD | M3 | Persistence port, swappable adapter |
| Transactional Outbox | Microservices | M3 | DB/Kafka dual-write problem |
| Observer (conceptual) | GoF | M3 | Domain events emitted on state change |
| CQRS | Microservices | M4 | Read/write model split |
| Materialized View | Microservices | M4 | What the read model formally is |
| Strategy | GoF | M4 | Swappable cache strategies |
| Dead Letter Channel | Microservices/EIP | M5 | Poison message handling |
| Circuit Breaker | Microservices | M5, M6 | Downstream failure isolation |
| API Gateway | Microservices | M6 | Single entry point |
| Load Balancer | Microservices | M6 | Traffic distribution across replicas |
| Facade (conceptual) | GoF | M6 | What the Gateway structurally is |
| Saga | Microservices | M7 | Multi-service transaction coordination |
| State | GoF | M7 | Explicit aggregate state machines |
| Command | GoF | M7 | Orchestrator issuing explicit commands |
| Mediator (conceptual) | GoF | M7 | What the orchestrator structurally is |
| Template Method | GoF | M8 (conditional) | Only if a second batch format appears |

Notice what's *not* here: no Singleton (Spring's container handles lifecycle), no Builder forced everywhere (only where object construction is genuinely complex — test fixtures are a reasonable candidate if you find yourself needing it), no Decorator forced on top of Resilience4j (the library already implements that pattern internally — wrapping it again would be redundant ceremony). That's deliberate, in the same spirit as your original instruction: patterns earn their place by solving a real problem in front of you, not by completing a checklist.

---

## How to actually use this doc

Treat each milestone's "Definition of done" as a hard gate — don't start the next brick until you can demonstrate the current one working, ideally by breaking it on purpose (kill a broker, send a malformed message, fail a payment) and watching the system behave the way it's supposed to. That act of deliberately breaking each milestone is where most of the actual learning will happen, more so than the initial happy-path build.
