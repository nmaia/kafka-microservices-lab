# M0 — Foundations

**Status:** ✅ Done

## Goal
Repo, build tooling, and CI skeleton exist before any service does.

## What was built
- Multi-module Maven parent POM (`dependencyManagement` for shared versions: Spring Boot, Spring Cloud, Testcontainers, Resilience4j BOMs; Avro, Confluent client, ArchUnit pinned)
- `sandbox-arch-rules` module — ArchUnit dependency wired, empty ruleset placeholder (real rules start M2)
- `sandbox-avro-schemas` module — `avro-maven-plugin` wired to `generate-sources`, one dummy `.avsc` to prove codegen
- `docker-compose.base.yml` skeleton (`services: {}`)
- `.env.example`, `CONFIGURATION.md` (env-var-for-values vs mount-for-structure convention)
- Git repo initialized, `.gitignore` (`target/`, `.env`, `docker-compose.override.yml`)

## Decisions & trade-offs
- **JDK: Amazon Corretto 21 (LTS)** over Corretto 26 (a non-LTS Feature Release, supported only through Oct 2026 — would've meant another JDK migration soon) and over Corretto 25 (a valid LTS alternative, but would've required bumping `maven.compiler.release` for no real benefit at this stage). Corretto vs Eclipse Temurin was treated as a coin flip — both are equivalent OpenJDK builds; picked based on installer convenience, not a technical difference.
- **Monorepo, one Maven multi-module tree** for all future services, rather than one repo per service (the typical real-world default). Deliberate: this sandbox's goal is centrally shared/enforced `sandbox-arch-rules` and `sandbox-avro-schemas` across every pattern exercise, not independent per-team ownership — the usual reason for splitting repos doesn't apply to a single-developer lab.
- **Spring Boot Initializr deliberately not used here** — M0's two modules (`sandbox-arch-rules`, `sandbox-avro-schemas`) aren't Spring Boot applications at all (no starters, no main class), so Initializr has nothing to generate yet. Revisit at M2, when `order-service` is a real runnable Spring Boot app.
- **`sandbox-arch-rules` left genuinely empty**, not pre-populated with guessed rules — rules should be added once a real violation (or near-violation) exists to justify them (starting M2), not written speculatively for coverage.
- **Maven installed manually** (binary zip from maven.apache.org) rather than via a package manager — `winget search maven` returned nothing usable on this machine.

## Environment established
- **JDK:** Amazon Corretto 21 (LTS) — chosen over Corretto 26 (non-LTS Feature Release, support ends Oct 2026) and over Corretto 25 (valid LTS, but would've required bumping `maven.compiler.release`; not necessary since the POM already targets 21)
- **Maven:** installed manually from maven.apache.org (winget had no working package for it)
- **JAVA_HOME / PATH:** set as System environment variables on Windows
- **Docker Desktop:** updated from 27.5.1 to current via the built-in updater

## Verification (Definition of Done)
- `mvn clean install` → BUILD SUCCESS
- `docker compose -f docker-compose.base.yml config` → resolves cleanly

## Commands reference
```powershell
java -version
mvn -v
mvn clean install
docker compose -f docker-compose.base.yml config
git init
git add -A
git commit -m "M0: repo, build tooling, and CI skeleton"
```

## Troubleshooting log
1. **`mvn` not recognized** — Maven wasn't installed; `winget search maven` / `winget install Apache.Maven` found nothing usable → installed manually (binary zip from maven.apache.org), added its `bin` folder to `PATH`.
2. **No JDK installed** — evaluated Corretto 21 vs 25 vs 26; picked 21 (LTS, matches the parent POM as-is).
3. **`mvn -v` still failed after installing the JDK** — `JAVA_HOME` wasn't set. Fixed via System Environment Variables GUI, pointing at the Corretto 21 install directory; required reopening PowerShell to take effect.
4. **"The goal you specified requires a project to execute but there is no POM in this directory"** — ran `mvn clean install` from the outer extracted folder (`kafka-microservices-lab-M0`) instead of the actual project folder one level deeper (`kafka-microservices-lab`, containing `pom.xml`).
5. **`LF will be replaced by CRLF` warnings on `git add`** — harmless; Git's `core.autocrlf` normalizing line endings on Windows, not an error.