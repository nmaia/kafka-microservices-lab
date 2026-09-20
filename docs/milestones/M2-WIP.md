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

- `maven-compiler-plugin` (`3.15.0`, pinned explicitly) added to the root
  `pom.xml`'s `<build><plugins>` with `<parameters>true</parameters>` —
  fixes `@PathVariable`/`@RequestParam` resolution for every module (see
  Troubleshooting log)
- `OrderTest` (`order-service/src/test/java/dev/sandbox/orderservice/domain/`)
  — 17 tests, JUnit 5 + AssertJ, grouped into three `@Nested` classes:
  `Create` (field assignment, total calculation, `OrderCreatedEvent`
  raising), `Validation` (parameterized null/blank/invalid-currency
  matrices for `customerId`/`items`/`currency`), and
  `EqualsAndHashCode` (DDD identity contract, including the same-`orderId`
  case via reflection on the private constructor — see Decisions)
- First ArchUnit rules: `SandboxArchRules` (`sandbox-arch-rules`) gained
  three parametrized rule methods — `domainDoesNotDependOnSpring`,
  `domainDoesNotDependOnAdapters`, `domainDoesNotDependOnJpa` (the last one
  forward-looking, ahead of M3's `JpaOrderRepository`). `order-service`
  gained `HexagonalArchitectureTest` (`@AnalyzeClasses` +
  `@ArchTest`-annotated `static final ArchRule` fields, per ArchUnit's own
  JUnit 5 integration pattern), invoking all three against
  `dev.sandbox.orderservice`. Verified with a negative-control check (see
  Verification) — not just a pass, confirmed to actually catch a
  violation.

## Not yet built (open for M2)
- `springdoc-openapi` (Swagger UI + Scalar) — not in `order-service/pom.xml`

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
- `OrderTest` (`Order`'s first unit test class) does **not** introduce a
  shared test-data builder — same "earn it, don't design for hypothetical
  future" reasoning already applied to MapStruct and the DTO
  subpackaging. It's the first test class in the project; a builder is
  premature until a second test class (e.g. `CreateOrderUseCase`'s)
  actually duplicates the same order-construction boilerplate.
- `EqualsAndHashCode`'s same-`orderId`-different-fields case uses
  reflection (`Constructor.setAccessible(true)`) to invoke `Order`'s
  private constructor directly, rather than skipping the case. `create()`
  always assigns a fresh `orderId`, so it's the only way to get two
  instances sharing one — and without it, the actual DDD identity
  contract the `equals()` override exists for (same `orderId` ⇒ equal,
  regardless of every other field) would go unverified. Deliberate
  trade-off: a test that reaches past the public API, chosen over leaving
  the override's real behavior untested.
- ArchUnit rules defined as **parametrized static methods** in the shared
  `sandbox-arch-rules` module (`domainDoesNotDependOnSpring(basePackage)`,
  etc.) rather than hardcoded against `dev.sandbox.orderservice` — chosen
  deliberately over the simpler "just write them in `order-service`'s test
  suite" option, since `sandbox-arch-rules`'s whole purpose (per its own
  module description) is being reused by every future service's test
  suite (`payment-service`, `inventory-service`, ...) without
  reimplementing the same checks.
- `HexagonalArchitectureTest`'s `@AnalyzeClasses` uses
  `importOptions = ImportOption.DoNotIncludeTests.class` — without it,
  ArchUnit also imports `order-service`'s own test classes (e.g.
  `OrderTest`, which sits in the `domain` package) and checks *them*
  against the rules too. Test code legitimately uses JUnit/AssertJ/
  reflection; these rules exist to police production code.
- `domainDoesNotDependOnJpa` was added even though nothing in
  `order-service` uses JPA yet — deliberately forward-looking, so the
  constraint is locked in *before* M3 introduces `JpaOrderRepository`,
  rather than relying on nobody putting `@Entity` on `Order` itself later.

## Verification
- **`POST /orders` → Kafka: confirmed end-to-end.** A real request
  produced `201`, a correct `Location` header, and the correct computed
  total (`2 × 19.99 = 39.98`); the resulting `OrderCreated` message was
  confirmed present and correctly decoded on the `order-events` topic via
  Kafka UI (http://localhost:8080). M2's core Kafka-producer DoD is met.
- **`GET /orders/{orderId}`: confirmed working.** After the
  `maven-compiler-plugin` fix, a fresh `POST` (`201`,
  `orderId=01a0bfba-56ba-7700-b90a-a4038c0b9442`) followed by `GET` on that
  same id returned `200` with a body matching the `POST` response exactly.
  Both `OrderController` endpoints are verified end to end; M2's core DoD
  is met.
- **`Order` unit tests: confirmed.** `OrderTest` (17 tests — creation,
  validation, identity) passes via `mvn -pl order-service test`. The
  in-memory repository itself remains untested (no dedicated
  `InMemoryOrderRepositoryTest` — its only real behavior is delegating to
  `ConcurrentHashMap`, exercised indirectly via the end-to-end `POST`/`GET`
  checks above).
- **ArchUnit rules: confirmed, with a negative control.** All three
  `HexagonalArchitectureTest` rules pass against current `order-service`
  code (`mvn -pl order-service -am test`, 20/20 total incl. `OrderTest`).
  Verified the rules aren't vacuously passing (e.g. from a package-name
  typo matching zero classes): temporarily added a genuinely-used
  `@Component` import to `Order.java`, reran the build, and confirmed
  `domainDoesNotDependOnSpring` failed with a violation message correctly
  naming `Order` and the `because(...)` reason — then reverted and
  confirmed a clean 20/20 pass again.

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

**Running the full test suite (unit + ArchUnit)**, after changing anything in
`sandbox-arch-rules` as well as `order-service`:

```powershell
# -am (--also-make): builds order-service together with the sibling
# modules it depends on (sandbox-arch-rules, sandbox-avro-schemas), using
# their freshly-compiled classes directly. Plain `mvn -pl order-service
# test` alone resolves sandbox-arch-rules from whatever's already in
# ~/.m2 - stale if it was only just recompiled, not installed (see
# Troubleshooting log).
mvn -pl order-service -am test
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
- **`GET /orders/{orderId}` failed — resolved.** Root cause: `order-service`
  doesn't inherit `spring-boot-starter-parent` (this project's modules
  extend their own multi-module parent and import the Spring Boot BOM
  instead), so it never gets the `<parameters>true</parameters>` compiler
  flag a stock Spring Boot project gets for free. Without it, javac drops
  parameter names from bytecode, so Spring can't resolve
  `@PathVariable String orderId` via reflection. Fixed by adding to the
  root `pom.xml`'s `<build><plugins>` (same inherited-by-every-module
  pattern as Spotless/PMD):
  ```xml
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.15.0</version>
    <configuration>
      <parameters>true</parameters>
    </configuration>
  </plugin>
  ```
  Then `mvn -pl order-service compile` (parameter names are baked in at
  compile time, so a fresh `.class` is required) and restarting the
  running process before retrying `GET`.
  - First `mvn -pl order-service compile` after adding the plugin
    succeeded but logged `'build.plugins.plugin.version' ... is missing`
    for every module — the plugin block above initially had no
    `<version>`, unlike Spotless/PMD which both pin one explicitly, so
    Maven silently resolved the newest available version instead of a
    fixed one. Fixed by pinning `<version>3.15.0</version>` (the version
    the unpinned build had actually resolved to), matching the same
    "explicit, not implied" reasoning already applied elsewhere.
  - The first `GET` retry after the fix still 404'd, but that was a red
    herring, not a fix failure: the `orderId` being reused was from an
    order created in an *earlier* run of `order-service`, before this
    session's restarts. `InMemoryOrderRepository` is wiped on every
    process restart (it's explicitly temporary), so that order no longer
    existed in memory — even though its `OrderCreated` message was still
    sitting on `order-events` from when it was originally produced. Kafka
    and the in-memory store are independent; an id present in one doesn't
    imply it's present in the other. Confirmed the actual fix by POSTing a
    **new** order in the current run and immediately `GET`-ing that same
    id: `201` then `200`, response bodies matching exactly
    (`orderId=01a0bfba-56ba-7700-b90a-a4038c0b9442`).
- **`order-service` test build failed with `cannot find symbol` for the new
  `SandboxArchRules` methods, right after `sandbox-arch-rules` had just
  compiled clean.** Not a code error — a Maven multi-module reactor gotcha.
  `mvn -pl order-service test` only builds `order-service` in isolation,
  resolving `sandbox-arch-rules` from whatever jar is already sitting in
  the local repo (`~/.m2`) — a stale one predating the new methods, since
  compiling `sandbox-arch-rules` alone doesn't publish it anywhere.
  **Fixed** with `mvn -pl order-service -am test` (`--also-make`): builds
  `order-service` together with the sibling modules it depends on, in one
  reactor run, using their freshly-compiled classes directly — no local
  repo `install` step needed. Preferred over `mvn -pl sandbox-arch-rules
  install` for day-to-day iteration, since `install` would need repeating
  on every further `sandbox-arch-rules` change.
- **Negative-control check for the new ArchUnit rules didn't trigger a
  failure on the first attempt** — added `import
  org.springframework.stereotype.Component;` to `Order.java` with nothing
  using it, expecting `domainDoesNotDependOnSpring` to fail, but Spotless
  failed the build first with a format violation showing the import being
  *removed*. Google Java Format strips unused imports automatically as
  part of formatting, so an unused import can never survive to create a
  real dependency for ArchUnit to catch. Fixed by annotating the class
  itself (`@Component` on `Order`) instead of just importing — a genuine
  usage Spotless won't touch. That reran and correctly failed
  `domainDoesNotDependOnSpring`, naming `Order` and the `because(...)`
  reason; reverting both the annotation and the import returned to a clean
  20/20.
