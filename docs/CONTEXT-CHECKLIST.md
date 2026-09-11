# Context Checklist — Kafka Microservices Lab

Fast-orientation doc for picking this project back up — by you, or another AI
assistant — without re-reading every milestone doc in full.

## Reference documents
- `docs/WORKING-AGREEMENTS.md` — how we work: pairing style, post-milestone documentation process
- `sandbox-context-document.md` — full architecture rationale & decisions (treat as settled)
- `implementation-roadmap.md` — milestone-by-milestone plan, M0–M10
- `docs/milestones/*.md` — what was actually done + troubleshooting, per milestone

## Environment (established in M0)
- JDK: Amazon Corretto 21 (LTS)
- Maven: installed manually from maven.apache.org, on `PATH`
- Docker Desktop: kept updated via built-in updater
- IntelliJ plugin: "Avro Schema Support" (Oscar Westra van Holthe - Kind) for `.avsc`/`.avdl` editing
- IntelliJ plugin: "google-java-format" — enabled so Reformat Code
  (Ctrl+Alt+L) applies Google Java Format instead of IntelliJ's default
  formatter, matching the Spotless-enforced style in the build.

## Milestone status
| Milestone | Status | Doc |
|---|---|---|
| M0 — Foundations | ✅ Done | `docs/milestones/M0-foundations.md` |
| M1 — Kafka Core | ✅ Done | `docs/milestones/M1-kafka-core.md` |
| M2 — order-service (Hexagonal skeleton) | ⏳ Not started | — |
| M3–M10 | ⏳ Not started | — |

## How to start the stack
```powershell
cd kafka-microservices-lab
docker compose -f docker-compose.base.yml up -d
docker compose -f docker-compose.base.yml ps
```
- Kafka UI: http://localhost:8080
- Schema Registry REST: http://localhost:8081

## How to stop
```powershell
docker compose -f docker-compose.base.yml stop
```

## Starting a Claude Code session
See [`docs/CLAUDE-CODE-SETUP.md`](./CLAUDE-CODE-SETUP.md#starting-a-session-every-time-after-setup-is-done) for daily usage (`claude`, `claude -c` to resume, `/mcp` to check the IDE connection).

## Known gotchas (full detail in the relevant milestone doc)
- Avro Maven plugin's decimal flag is `enableDecimalLogicalType` — **singular**, no trailing "s".
- PMD analyzes every compile source root by default, including
  Avro-generated code under `target/generated-sources/avro` — exclude
  it via `excludeRoots` in the parent `pom.xml`'s PMD plugin config,
  not per-violation suppression.
- `kafka-avro-console-producer` lives in the `schema-registry` container, not `kafka`.
- Avro `decimal` fields can't be hand-typed via the CLI console producer — use a throwaway topic/schema for CLI smoke tests, never the real one.
- PowerShell + Docker + embedded JSON quoting is fragile — prefer `docker cp` + an interactive `docker exec -it <container> bash` shell over trying to escape everything inline.

## Next up
M2 — first Spring Boot service (`order-service`), Hexagonal package skeleton, real Kafka producer publishing `OrderCreated`.