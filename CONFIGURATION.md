# Configuration Guide

This sandbox is meant to be a **team resource**: other developers should be able to
plug in their own producer/consumer implementations, schemas, and data models
without fighting its structure. That only works if it's clear which lever to pull
for which kind of change. There are two distinct mechanisms - don't conflate them.

## 1. Environment variables — for *values*

Use env vars (`.env`, copied from `.env.example`) for anything that's a value, not a
structural choice:

- Kafka bootstrap servers, ports, topic names, consumer group IDs
- DB connection strings and credentials
- Schema Registry URL and compatibility mode
- Replica counts, cache TTLs, retry/backoff intervals
- **Which Docker image or build context to use** for a service, e.g.:
  ```
  PRODUCER_IMAGE=./producer-api
  ```
  This is the actual "bring your own producer" mechanism - swapping the value
  lets a developer's own build replace the reference implementation while the
  rest of the sandbox is untouched.

## 2. Volume mounts / build-context swaps — for *structure*

Use a mount or a `build:` context swap when what's changing is shape, not a value:

- A developer's own `.avsc` schema directory, mounted via `AVRO_SCHEMA_PATH`
- A developer's own service source, via `build:` pointing at their own directory

This is deliberate: configuring DB table shape or schema structure through
environment variables would mean building a config language instead of a lab. If
you find yourself reaching for an env var to describe *structure*, that's the
signal you want a mount or a build-context swap instead.

## 3. Spring profiles — for *behavior*

`SPRING_PROFILES_ACTIVE=outbox|cqrs|saga` selects which behavior a service runs
with. This is the code-side counterpart to the two mechanisms above - it doesn't
touch infra wiring.

## Files involved

| File | Committed? | Purpose |
|---|---|---|
| `.env.example` | Yes | Documents every value-level var; copy to `.env` locally |
| `.env` | No (gitignored) | Your actual local values |
| `docker-compose.base.yml` + overlays | Yes | Shared infra/service topology |
| `docker-compose.override.yml` | No (gitignored) | Your local tweaks, auto-merged by Compose |

## Optional: Claude Code (AI assistant) setup

If you want to use Claude Code inside IntelliJ for this project, see
[`docs/CLAUDE-CODE-SETUP.md`](./docs/CLAUDE-CODE-SETUP.md) for the full
walkthrough, including known Node/npm upgrade issues on Windows.

This file gets updated whenever a milestone introduces a new class of
configuration - not just a new variable name.
