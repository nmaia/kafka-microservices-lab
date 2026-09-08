# References

Authoritative sources informing the architectural and implementation
decisions in this project — organized by topic, roughly matching the
patterns covered across the milestones in `implementation-roadmap.md`.

## How this is used

These are **anchors for judgment, not rules to follow blindly**. When a
milestone touches one of these topics, the relevant source(s) inform the
"why" behind a decision — but `sandbox-context-document.md` and each
milestone's own Decisions & Trade-offs section record what we *actually*
did and why, including any deliberate deviation from the source material
for a good reason specific to this project (e.g., choosing Avro `decimal`
over a textbook-simple type for money, because floating-point rounding is
a real concern even in a lab).

Canonical texts are a starting point, not a ceiling — where a more recent,
well-regarded source offers better or more current thinking than an older
"bible" of the field, we should prefer it, and update this file accordingly.

## Design Patterns (general)
- **Gamma, Helm, Johnson, Vlissides — *Design Patterns: Elements of
  Reusable Object-Oriented Software* ("Gang of Four", 1994)** — the
  foundational reference for the object-oriented patterns underlying much
  of this project (Factory Method for `Order.create()`, etc.). Old, but
  still the common vocabulary everyone else uses.

## SOLID Principles / Clean Architecture
- **Robert C. Martin ("Uncle Bob") — *Clean Architecture: A Craftsman's
  Guide to Software Structure and Design* (2017)**, and *Agile Software
  Development, Principles, Patterns, and Practices* (2002) — the source of
  SOLID as a discipline; directly informs how Hexagonal's `domain/` layer
  should stay dependency-free (Dependency Inversion in particular).

## Hexagonal Architecture (Ports & Adapters)
- **Alistair Cockburn — original "Hexagonal Architecture" writings
  (alistair.cockburn.us)** — the source of the pattern itself; the
  `domain/`, `application/`, `adapters/in/`, `adapters/out/` structure used
  starting M2 traces back to this.

## Domain-Driven Design
- **Eric Evans — *Domain-Driven Design: Tackling Complexity in the Heart
  of Software* (2003)** — the origin of bounded contexts, aggregates, and
  the domain-modeling vocabulary this project uses (`Order` as an
  aggregate, etc.).
- **Vaughn Vernon — *Implementing Domain-Driven Design* (2013)** — a more
  practical, applied companion to Evans; useful when Evans is more
  theoretical than the project needs.

## Microservices Architecture
- **Sam Newman — *Building Microservices* (2nd ed., 2021)** — broad
  architectural guidance: service boundaries, data ownership, deployment.
- **Chris Richardson — *Microservices Patterns* (2018)**, and
  microservices.io — the direct source for several patterns this roadmap
  names explicitly: Transactional Outbox (M3), CQRS (M4), SAGA (M7), and
  API Gateway (M6).

## Enterprise Integration / Messaging Patterns
- **Gregor Hohpe, Bobby Woolf — *Enterprise Integration Patterns* (2003)**
  — predates Kafka, but the messaging pattern vocabulary (Dead Letter
  Channel, Message Router, Content-Based Router) still underlies M5 (DLQ)
  and M6 (Gateway) even when implemented with modern tools.

## Kafka Specifically
- **Confluent's official documentation (docs.confluent.io)** — kept as a
  living reference rather than a book, since Kafka/Confluent Platform
  tooling and best practices evolve faster than any book edition; used
  throughout M1 for KRaft, Schema Registry, and Avro specifics.

## Event-Driven Architecture (Kafka-specific)
- **Ben Stopford — *Designing Event-Driven Systems* (Confluent, free
  ebook)** — narrower and more current than Hohpe/Woolf specifically for
  Kafka-based systems; useful alongside Enterprise Integration Patterns'
  more general messaging vocabulary.

## API Documentation
- **springdoc-openapi's official documentation (springdoc.org)** — the
  direct reference for wiring OpenAPI 3 generation, Swagger UI, and Scalar
  UI into every REST-exposing service, starting with `order-service` in
  M2. Chosen over the older SpringFox, which is no longer maintained.

## Change Data Capture
- **Debezium's official documentation (debezium.io)** — living reference
  for how the Outbox Event Router SMT actually taps the Postgres WAL (M3);
  kept as docs rather than a book, since CDC tooling specifics move fast.
- **Martin Kleppmann — *Designing Data-Intensive Applications* (2017)** —
  broader theoretical grounding for *why* CDC/log-based replication works
  the way it does; also relevant to M6's multi-broker replication and
  partitioning behavior generally.

## CQRS (dedicated)
- **Greg Young — original CQRS talks and writings (e.g. "CQRS Documents",
  freely available)** — the pattern's originator; goes deeper on the
  read/write model separation itself than Richardson's treatment, which
  covers CQRS as one pattern among several. Worth reading directly given
  M4 is a dedicated milestone, not just a passing implementation.

## NoSQL / Polyglot Persistence
- **Pramod Sadalage, Martin Fowler — *NoSQL Distilled* (2012)** — despite
  its age, still the clearest conceptual grounding for *why* and *when* a
  document store fits a use case (denormalized read models) versus a
  relational one — directly relevant to the write-side Postgres /
  read-side MongoDB split in M4.
- **MongoDB's official documentation (mongodb.com/docs), specifically its
  data modeling guidance** — the practical, current reference for actually
  shaping the denormalized `orders_read` documents well, since document
  schema design has real anti-patterns not covered by the more theoretical
  Sadalage/Fowler book.

## Caching
- **AWS Caching Best Practices (docs.aws.amazon.com)** and **Redis's own
  documentation (redis.io)** — practical references for cache-aside vs.
  write-through, used for Redis in the CQRS module (M4). Kept as living
  docs rather than a single book, since this is more a set of well-known
  trade-offs than one canonical text.

## Resilience Patterns (Circuit Breaker, Retry, Bulkhead)
- **Michael Nygard — *Release It!: Design and Deploy Production-Ready
  Software* (2nd ed., 2018)** — the original, pre-Resilience4j source for
  Circuit Breaker, Bulkhead, and Timeout patterns; the conceptual basis
  for what Resilience4j implements at the Gateway (M6) and in
  `notification-service`'s webhook retries (M8).
- **Resilience4j's official documentation (resilience4j.readme.io)** and
  **Spring Cloud Gateway's official documentation
  (spring.io/projects/spring-cloud-gateway)** — practical, current
  references for actually configuring circuit breakers, retries, and
  routing/load-balancing at the Gateway layer in M6, as distinct from
  Nygard's conceptual grounding above.

## SAGA (dedicated)
- **Hector Garcia-Molina, Kenneth Salem — "Sagas" (1987)** — the original
  academic paper introducing the saga concept and compensating
  transactions, predating microservices entirely; the root of both the
  choreography and orchestration variants implemented in M7.
- **Chris Richardson — *Microservices Patterns*** (see Microservices
  Architecture above) — the applied, microservices-specific treatment of
  the same concept, including the choreography vs. orchestration framing
  used directly in M7's roadmap.

## Batch Processing
- **Spring Batch's official documentation
  (spring.io/projects/spring-batch)** — the direct reference for
  `batch-ingestion-service`'s file-polling, row-parsing, and
  chunk-processing setup in M8.

## Webhooks
- No single canonical text — webhooks are a pragmatic HTTP convention
  rather than an academic or formally documented pattern. M8's outbound
  (notification-service → external URL) and inbound (external system →
  Gateway) implementations lean on the Resilience Patterns references
  above (retry/circuit-breaking) rather than a webhook-specific source.

## Observability / Distributed Tracing
- **OpenTelemetry's official documentation (opentelemetry.io)** — the
  direct reference for M9's OTel Collector setup and OTLP instrumentation
  across every service.
- **Austin Parker, Daniel Spoonhower, et al. — *Distributed Tracing in
  Practice* (2020)** — conceptual grounding for trace/span/context
  propagation, ahead of implementing M9.

## Architecture Testing
- No single canonical text; **ArchUnit's own documentation
  (archunit.org)** is the practical reference for M0/M10's rule-writing.

## To revisit
As milestones progress into less-settled territory, add sources here as
they're actually used, rather than speculatively listing texts we haven't
yet drawn on.