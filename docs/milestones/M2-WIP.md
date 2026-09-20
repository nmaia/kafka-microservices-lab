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
- `spring-boot-starter-validation` added to `order-service/pom.xml`
- REST DTOs in `adapters/in/rest/dto/` — `CreateOrderRequest`/`OrderLineRequest`
  (inbound, Bean-Validation-annotated: `@NotBlank`/`@NotEmpty`/`@Positive`),
  `OrderResponse`/`OrderLineResponse` (outbound), all anemic records
- `OrderRestMapper` (`adapters/in/rest/`) — `CreateOrderRequest → CreateOrderCommand`,
  `Order → OrderResponse`, manual (same reasoning as `OrderEventMapper`)
- `OrderNotFoundException` (`application/`) — thrown from the controller's
  GET lookup, mapped to 404
- `OrderController` (`adapters/in/rest/`) — `POST /orders` (201 + `Location`,
  delegates to `CreateOrderUseCase`) and `GET /orders/{orderId}` (delegates
  directly to `OrderRepository`, no dedicated query use case for a plain
  unconditional lookup)
- `OrderRestExceptionHandler` (`@RestControllerAdvice`) — `IllegalArgumentException`
  → 400, `OrderNotFoundException` → 404, both as RFC 7807 `ProblemDetail`
  (`spring.mvc.problemdetails.enabled: true`)
- `server.port: ${SERVER_PORT:8082}` in `application.yml` / `SERVER_PORT=8082`
  in `.env.example` — `order-service` can't share 8080 with Kafka UI
- `app.kafka.topics.order-events` binding actually populated in
  `application.yml` (was previously just a comment with no key under it)

## Not yet built (open for M2)
- `springdoc-openapi` (Swagger UI + Scalar) — not in `order-service/pom.xml`
- Unit tests for `Order` — no `src/test/java` exists yet in `order-service`
- First ArchUnit rules for `order-service` — `sandbox-arch-rules` dependency
  is present (test scope) but unused so far
- **`GET /orders/{orderId}` is implemented but broken** — root-caused, fix
  not yet applied (see Troubleshooting log). Root pom.xml needs a
  `maven-compiler-plugin` entry with `<parameters>true</parameters>`.

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
- REST DTOs split into `adapters/in/rest/dto/`, separate from
  `OrderController`/`OrderRestMapper`/the exception advice which stay flat
  in `adapters/in/rest/` — documented in `docs/CODING-STYLE.md`'s new
  "Subpackaging within adapters" section; earned by the mix of three
  structural roles in one folder, not file count alone.
- `OrderRestMapper` kept as its own `@Component` (not inlined as private
  methods on `OrderController`) — discussed explicitly rather than
  defaulted to, for symmetry with `OrderEventMapper` on the Kafka side.
- `GET /orders/{orderId}` reads `OrderRepository` directly rather than
  through a use case — a plain, unconditional repository lookup has no
  orchestration logic to justify one; consistent with Hexagonal (inbound
  adapters may depend on ports directly, not only through use cases).

## Verification
- **`POST /orders` → Kafka: confirmed end-to-end.** A real request
  produced `201`, a correct `Location` header, and the correct computed
  total (`2 × 19.99 = 39.98`); the resulting `OrderCreated` message was
  confirmed present and correctly decoded on the `order-events` topic via
  Kafka UI (http://localhost:8080). M2's core Kafka-producer DoD is met.
- **`GET /orders/{orderId}`: still broken.** Root cause diagnosed (see
  Troubleshooting log, last entry) but the fix was never applied before
  the session ended — needs to be picked back up.
- Domain logic (`Order.create()` validations, total calculation) and the
  in-memory repository still haven't been exercised by an automated test —
  no `src/test/java` exists in `order-service` yet.

## Commands reference

**Manually testing `OrderController` end to end**, from a clean start:

```powershell
# 1. Start the stack (Kafka, Kafka UI, Schema Registry)
docker compose -f docker-compose.base.yml up -d

# 2. Run order-service on the host (not in Docker) so it reaches Kafka's
#    EXTERNAL listener on localhost:9092
mvn -pl order-service spring-boot:run

# If Spotless rejects a formatting mismatch during compile:
mvn -pl order-service spotless:apply
mvn -pl order-service compile

# 3. In a second terminal, write the POST body to request.json (repo root).
#    Don't inline the JSON directly into curl.exe -d — PowerShell re-quotes
#    arguments for native exes and corrupts embedded double quotes (see
#    Troubleshooting log). A single-quoted here-string sidesteps that:
@'
{"customerId":"cust-123","items":[{"sku":"SKU-1","quantity":2,"unitPrice":19.99}],"currency":"USD"}
'@ | Set-Content -Encoding utf8 request.json

# 4. POST /orders — reads the body from request.json instead of inlining it
curl.exe -i -X POST http://localhost:8082/orders `
  -H "Content-Type: application/json" `
  -d "@request.json"

# 5. Copy the orderId out of the response body/Location header, then:
curl.exe -i http://localhost:8082/orders/<orderId>

# 6. Cross-check delivery: open http://localhost:8080 (Kafka UI), the
#    order-events topic, and confirm the newest message decodes correctly
#    as the OrderCreated Avro record (schema must already be registered —
#    see Troubleshooting log's Schema Registry entry if it 404s).
```

## Troubleshooting log
- **Spotless formatting rejection on compile** — new record/Javadoc
  wrapping didn't match Google Java Format. Fix: `mvn -pl order-service
  spotless:apply`, then recompile. Not a real bug, just needed running.
- **Port collision on 8080** — `order-service` defaulted to `8080`, same
  as Kafka UI (`docker-compose.base.yml` maps `8080:8080`). Fixed by
  adding `server.port: ${SERVER_PORT:8082}` to `application.yml` and
  `SERVER_PORT=8082` to `.env.example`, following the existing
  `${VAR:default}` pattern.
- **`curl.exe` returned `400 "Failed to read request"` on a well-formed
  body** — not a JSON problem; PowerShell re-quotes arguments before
  handing them to a native exe like `curl.exe`, so embedded double quotes
  in an inline `-d '{...}'` payload arrive corrupted. Same class of issue
  as the pre-existing "PowerShell + Docker + embedded JSON quoting"
  gotcha, just hitting a native Windows exe directly instead of `docker
  exec`. Fixed by writing the payload to `request.json` via a
  single-quoted PowerShell here-string (`@'...'@`, no interpolation) and
  using `curl.exe -d "@request.json"` instead of inlining JSON.
- **`"Topic cannot be null"` at publish time** — `app.kafka.topics.order-events`
  had never actually been set: `application.yml` had a `topics:` key with
  only a comment underneath it, no `order-events` entry, so
  `KafkaTopicsProperties.orderEvents()` bound to `null`. Fixed by adding
  the missing key as a *child* of `topics:` (an initial attempt indented
  it as a sibling, which bound it to the wrong property path):
  `order-events: ${ORDER_EVENTS_TOPIC:order-events}`.
- **Schema Registry `404 "Schema not found"` on publish, after the topic
  fix** — `auto.register.schemas=false` makes the Avro serializer do an
  *exact-match* schema lookup, not a compatibility check. The plain
  `.avsc` source had been pasted into Kafka UI, but
  `sandbox-avro-schemas`'s Avro plugin config (`<stringType>String</stringType>`)
  makes the *generated* class embed an extra `"avro.java.string":"String"`
  property on every string field. Semantically compatible, but not
  byte-identical to the `.avsc` — hence the exact-match 404. Fixed by
  pulling the literal schema out of the generated `OrderCreated.java`'s
  `SCHEMA$` field (`sandbox-avro-schemas/target/generated-sources/avro/...`)
  and registering *that* as a new version of `order-events-value` in Kafka
  UI, instead of the `.avsc` source. **General gotcha**: whenever
  `stringType=String` is set, the `.avsc` source must never be what's
  registered — always pull the schema from the generated class.
- **`GET /orders/{orderId}` fails — open, unresolved.** Root cause
  diagnosed: `order-service` doesn't inherit `spring-boot-starter-parent`
  (this project's modules extend their own multi-module parent and import
  the Spring Boot BOM instead), so it never gets the
  `<parameters>true</parameters>` compiler flag a stock Spring Boot
  project gets for free. Without it, javac drops parameter names from
  bytecode, so Spring can't resolve `@PathVariable String orderId` via
  reflection. Fix identified but **not yet applied**: add to the root
  `pom.xml`'s `<build><plugins>` (same inherited-by-every-module pattern
  as Spotless/PMD):
  ```xml
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
      <parameters>true</parameters>
    </configuration>
  </plugin>
  ```
  Then `mvn -pl order-service compile` (parameter names are baked in at
  compile time, so a fresh `.class` is required) and restart the running
  process before retrying `GET`.
