# Kafka Microservices Lab

A personal/team learning sandbox for testing distributed-systems and
microservices patterns — SAGA, CQRS, Transactional Outbox, DLQ, API Gateway,
and more — over Kafka (KRaft) and Spring Boot, built incrementally and
verified milestone by milestone.

This isn't a production system. It's a space to validate architectural ideas
with speed and independence, before proposing changes to real applications —
and a resource other developers can plug their own producers, consumers, and
schemas into without fighting the project's structure.

## Status

| Milestone | Description | Status |
|---|---|---|
| M0 | Foundations — repo, build tooling, CI skeleton | ✅ Done |
| M1 | Kafka Core — single broker (KRaft), Schema Registry, Kafka UI | ✅ Done |
| M2 | First Spring Boot service (`order-service`, Hexagonal skeleton) | 🔄 In progress |
| M3 | Real persistence + Transactional Outbox | ⏳ Not started |
| M4 | CQRS split | ⏳ Not started |
| M5 | DLQ + resilience | ⏳ Not started |
| M6 | Multi-broker + API Gateway | ⏳ Not started |
| M7 | SAGA (choreography + orchestration) | ⏳ Not started |
| M8 | Boundary integrations (webhooks, SMTP, FTP) | ⏳ Not started |
| M9 | Observability | ⏳ Not started |
| M10 | Architecture tests hardening | ⏳ Not started |

Full milestone plan, sequencing, and "definition of done" gates: see
[`implementation-roadmap.md`](./implementation-roadmap.md).

## Getting started

**Prerequisites:**
- JDK 21 (Amazon Corretto or Eclipse Temurin — see [`docs/milestones/M0-foundations.md`](./docs/milestones/M0-foundations.md) for setup notes)
- Maven
- Docker Desktop

**Build:**
```powershell
mvn clean install
```

**Bring up the infrastructure:**
```powershell
docker compose -f docker-compose.base.yml up -d
```
- Kafka UI: [http://localhost:8080](http://localhost:8080)
- Schema Registry REST API: [http://localhost:8081](http://localhost:8081)

**Stop:**
```powershell
docker compose -f docker-compose.base.yml stop
```

## Project structure
```plaintext
kafka-microservices-lab/
├── sandbox-arch-rules/       # Shared ArchUnit rules...
├── sandbox-avro-schemas/     # .avsc schema sources...
├── docker-compose.base.yml   # Kafka (KRaft)...
├── CONFIGURATION.md          # env-var vs volume-mount...
├── .env.example
└── docs/
    ├── CONTEXT-CHECKLIST.md  # fast-orientation doc...
    └── milestones/           # what was built...
```

## Documentation

- **[Context Checklist](./docs/CONTEXT-CHECKLIST.md)** — start here. Environment setup, current status, how to run the stack, known gotchas.
- **[Working Agreements](./docs/WORKING-AGREEMENTS.md)** — how this project gets built: pairing style, and the documentation process followed after every milestone.
- **[Context Document](./sandbox-context-document.md)** — full architectural rationale: why Hexagonal over VSA, why KRaft, why Spring Cloud Gateway, etc. Settled decisions, not open questions.
- **[Implementation Roadmap](./implementation-roadmap.md)** — the milestone-by-milestone build plan (M0–M10), with patterns and "definition of done" gates.
- **[CONFIGURATION.md](./CONFIGURATION.md)** — the env-var-for-values vs. mount-for-structure convention used throughout.
- **[References](./docs/REFERENCES.md)** — the books, authors, and docs informing the patterns used throughout this project.

**Milestone logs** (what was built, why, and how issues were debugged):
- [M0 — Foundations](./docs/milestones/M0-foundations.md)
- [M1 — Kafka Core](./docs/milestones/M1-kafka-core.md)

## Contributing (for teammates plugging in their own work)

This sandbox is built so you can bring your own producer/consumer
implementations, schemas, or data models without restructuring the project.
See `CONFIGURATION.md` for the exact mechanism (env vars for values, mounts
for structure) and the context document for the architectural decisions
already made — please don't re-litigate a settled decision without reading
the rationale first.