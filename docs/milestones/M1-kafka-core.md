# M1 — Kafka Core

**Status:** ✅ Done

## Goal
One broker, Schema Registry, Kafka UI running and provably working, before multi-broker complexity.

## What was built

### `docker-compose.base.yml` services
```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.7.0
    container_name: kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CONTROLLER_QUORUM_VOTERS: "1@kafka:9093"
      KAFKA_LISTENERS: PLAINTEXT://kafka:9094,CONTROLLER://kafka:9093,EXTERNAL://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9094,EXTERNAL://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
    volumes:
      - kafka-data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

  schema-registry:
    image: confluentinc/cp-schema-registry:7.7.0
    container_name: schema-registry
    depends_on:
      kafka:
        condition: service_healthy
    ports:
      - "8081:8081"
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: PLAINTEXT://kafka:9094
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    depends_on:
      kafka:
        condition: service_healthy
      schema-registry:
        condition: service_started
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: sandbox
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9094
      KAFKA_CLUSTERS_0_SCHEMAREGISTRY: http://schema-registry:8081

volumes:
  kafka-data:
```

Key design point: **dual/triple listeners** — `EXTERNAL` (`localhost:9092`, host access), `PLAINTEXT` (`kafka:9094`, container-to-container), `CONTROLLER` (`kafka:9093`, internal KRaft consensus only). A `healthcheck` on `kafka` plus `depends_on: condition: service_healthy` on `schema-registry` ensures it waits for the broker to actually be ready, not just for the container to start.

### Real Avro schema: `OrderCreated.avsc` (replaces M0's `Dummy.avsc`)
- Fields: `orderId` (string), `customerId` (string), `totalAmount` (`decimal`, precision 10/scale 2), `currency` (string, default `"USD"`), `items` (array of nested `OrderItem{sku, quantity}`), `createdAt` (long)
- `totalAmount` uses Avro's `decimal` logical type (not `double`) to avoid floating-point rounding on money
- `currency` has a default deliberately — Schema Registry's `BACKWARD` compatibility mode requires new fields to have defaults
- Registered in Schema Registry: subject `order-events-value` (via `TopicNameStrategy`), schema ID `1`, version `1`, compatibility `BACKWARD`
- Topic `order-events` created (1 partition, replication factor 1)

### Build config fix
`sandbox-avro-schemas/pom.xml` needs `<enableDecimalLogicalType>true</enableDecimalLogicalType>` inside the `avro-maven-plugin`'s `<configuration>` so the generated `totalAmount` field is `java.math.BigDecimal`, not `java.nio.ByteBuffer`.

### Tooling
Installed IntelliJ **"Avro Schema Support"** plugin (Oscar Westra van Holthe - Kind, marketplace ID 15728) for real `.avsc`/`.avdl` syntax awareness — recommended directly by Apache Avro's own editor-support docs.

## Decisions & trade-offs
- **KRaft over Zookeeper** — per the context doc's mandate. One less distributed system to run/monitor/upgrade; Zookeeper is also deprecated for new Kafka deployments.
- **Three listeners (`EXTERNAL`/`PLAINTEXT`/`CONTROLLER`), not one** — a single address can't simultaneously mean "reachable from the host" and "reachable from another container," and KRaft controller traffic needs its own dedicated listener. With one broker, that same broker holds both the `broker` and `controller` roles; this changes once the multibroker overlay is introduced later.
- **`healthcheck` + `depends_on: condition: service_healthy` on `kafka`**, rather than plain `depends_on` — a container reporting "started" isn't the same moment as the broker being ready to accept connections; the plain form risks `schema-registry` racing ahead and failing to connect.
- **`kafka-ui`'s dependency on `schema-registry` uses the weaker `condition: service_started`**, not `service_healthy` — we didn't add a healthcheck to `schema-registry` itself, so that's the strongest condition available. Accepted for now since Kafka UI retries its own connections on startup; worth adding a `schema-registry` healthcheck later if this ever proves flaky in practice.
- **Topic named `order-events`**, not something invented fresh — chosen to match the existing architecture diagram in the context doc (`K1 -- "order-events" --> CMD`), keeping docs and infra consistent rather than introducing a second name for the same concept.
- **`totalAmount` as Avro `decimal` (bytes-backed), not `double`** — avoids floating-point rounding on money, which is the real-world-correct choice despite the added complexity (needed the `enableDecimalLogicalType` build flag, and is the direct reason the CLI producer test needed a throwaway workaround — see M1 troubleshooting #4).
- **Explicit schema registration chosen over relying on `auto.register.schemas`** (which defaults to `true` in the Avro serializer) — registering deliberately, as its own step, mirrors how a real CI pipeline would gate schema changes. `auto.register.schemas` is flagged as something to consciously disable once we write a real Spring Kafka producer in M2.
- **Kafka UI's form used to register the schema, not a raw REST call** — the REST API path is documented and understood (and is what real automation would eventually use), but Kafka UI removes a class of manual-JSON-escaping mistakes, and we're at the "verify it works" stage, not yet building CI automation.
- **`.avsc` (raw JSON) kept over switching to `.avdl` (Avro IDL)** — `.avsc` is what most tutorials/Confluent docs use, and is worth staying familiar with directly. `.avdl`'s more human-readable syntax is a good upgrade to revisit once schema-writing friction actually justifies it (e.g., by the 3rd or 4th schema), not before.
- **IntelliJ "Avro Schema Support" plugin chosen** over "Apache Avro™ support" and "Avro and Parquet Viewer" — it's the plugin Apache Avro's own editor-support docs recommend, is actively maintained, and covers both `.avsc` and a future `.avdl` switch in one plugin. The other two either had unclear maintenance status or solve a different problem (viewing compiled binary files, not authoring schemas).
- **Docker Desktop updated (27.5.1 → current)** even though nothing in M1 strictly required newer features — justified by real security patches (recent container-escape/privilege-escalation CVEs) and fewer first-run bugs in exactly the areas we were touching (WSL2, engine startup).
- **CLI Avro verification used a throwaway topic/schema, never a temporarily-mutated `OrderCreated`** — changing `totalAmount` from `decimal` to `double` on the real schema risked outright rejection by `BACKWARD` compatibility checking (it's not a compatible type change), and would have polluted the real schema's version history with a version that was never meant to exist.

## Verification (Definition of Done)
"Publish an Avro message via console producer and see it decoded correctly in Kafka UI" — confirmed via a **throwaway** topic/schema (`order-events-cli-test` / `order-events-cli-test-value`, `totalAmount` as `double`), since the real `decimal` field can't be hand-typed through the CLI Avro tools (see troubleshooting #4). Both deleted after verification; the real `order-events` topic/schema were never touched.

## Commands reference
```powershell
# Bring the stack up / check status
docker compose -f docker-compose.base.yml config
docker compose -f docker-compose.base.yml up -d
docker compose -f docker-compose.base.yml ps

# Plain topic round-trip test (test-topic, since deleted)
docker exec -it kafka kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
docker exec -it kafka kafka-console-producer --topic test-topic --bootstrap-server localhost:9092
docker exec -it kafka kafka-console-consumer --topic test-topic --from-beginning --bootstrap-server localhost:9092
docker exec -it kafka kafka-topics --delete --topic test-topic --bootstrap-server localhost:9092

# Schema Registry REST check
Invoke-RestMethod http://localhost:8081/subjects

# Real topic
docker exec -it kafka kafka-topics --create --topic order-events --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# CLI Avro verification (throwaway topic/schema — see troubleshooting)
docker exec -it kafka kafka-topics --create --topic order-events-cli-test --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
docker cp .\order-events-cli-test-schema.json schema-registry:/tmp/order-events-cli-test-schema.json
docker exec -it schema-registry bash
# then, inside the container:
kafka-avro-console-producer --topic order-events-cli-test --bootstrap-server kafka:9094 --property schema.registry.url=http://schema-registry:8081 --property value.schema="$(cat /tmp/order-events-cli-test-schema.json)"
# type message, Ctrl+C, exit

# Cleanup
docker exec -it kafka kafka-topics --delete --topic order-events-cli-test --bootstrap-server localhost:9092
# + delete subject order-events-cli-test-value via Kafka UI (Schema Registry → subject → delete)

# Stop / resume
docker compose -f docker-compose.base.yml stop
docker compose -f docker-compose.base.yml start
```

## Troubleshooting log
1. **Docker Desktop outdated (27.5.1)** — updated via Docker Desktop's built-in updater before proceeding (recent CVE fixes landed since that version).
2. **`enableDecimalLogicalTypes` (plural) silently ignored** — Maven doesn't error on an unrecognized plugin config element, so `mvn clean install` reported BUILD SUCCESS even though `totalAmount` stayed a `ByteBuffer`. The correct parameter is singular: `enableDecimalLogicalType`. IntelliJ's "element not allowed here" warning was actually right — worth trusting over a green build.
3. **`kafka-avro-console-producer`: executable file not found in $PATH`** — this tool ships with the `cp-schema-registry` image, not `cp-kafka`. Ran it against the `schema-registry` container instead, using `kafka:9094` as the bootstrap server (internal listener, since we're now inside a different container on the same Docker network).
4. **Avro `decimal` (bytes-backed) field can't be hand-typed via the CLI** — Avro's JSON encoding represents `bytes` as a literal byte sequence, not a human-readable number; there's no clean way to type "29.99" for a decimal field at the console producer prompt. Worked around with a throwaway topic/schema using `double` instead, purely for this one verification.
5. **PowerShell quoting broke the inline `--property value.schema='{...}'` command** — three escalating failures: bash-style single quotes silently dropped a `"`; PowerShell `\"` escaping passed literal backslashes through to the container; even `bash -c '...$(cat file)...'` broke on nested quote/paren layers. Fix: `docker cp` the schema to a file inside the container, then open an actual interactive shell (`docker exec -it schema-registry bash`) and run the command fully inside it — avoids PowerShell's argument re-encoding entirely.