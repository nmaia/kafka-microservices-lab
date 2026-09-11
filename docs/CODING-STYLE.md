# Coding Style

Concrete formatting and static-analysis rules for this project — separate
from `WORKING-AGREEMENTS.md` (how we work together) and
`sandbox-context-document.md` (architectural decisions). This is about what
the code itself looks like, line by line.

## Formatter: Google Java Format

Enforced via the **Spotless** Maven plugin, using **Google Java Format**
(the standard variant, not AOSP). This is deliberately non-negotiable —
GJF has no configuration knobs, by design, which is the point: no
tabs-vs-spaces or brace-placement debates.

What this means concretely:
- 2-space indentation, no tabs
- 100-character line length
- K&R brace style (opening brace on the same line)
- No wildcard imports — every import fully explicit
- One class per file
- Import ordering is decided by the tool, not manually

**Run it:**

```powershell
mvn spotless:apply
```

formats every file. `mvn spotless:check` (wired into the build) fails the
build if anything isn't formatted — meaning `spotless:apply` should be run
before every commit, not treated as optional.

## Static analysis: PMD

Chosen over Checkstyle for this project — Checkstyle mostly re-covers
formatting/convention ground GJF already owns; PMD focuses on code
smells and bug patterns GJF doesn't touch (empty catch blocks, unused
variables, unnecessary object creation), and its built-in
**cyclomatic complexity rule** directly reinforces the discipline already
established in `WORKING-AGREEMENTS.md`'s complexity-analysis section —
making that a checked build signal, not just a pairing-conversation habit.

Starts from PMD's default `quickstart` ruleset, via a project-level
`pmd-ruleset.xml` at the repo root rather than referencing `quickstart`
directly — this lets exclusions be layered on top with an inline reason,
rather than either accepting every default rule or suppressing findings
one class at a time. Current exclusions:

- **`UseUtilityClass`** — false-positives on every Spring Boot
  `*Application.java` main class (PMD sees "all methods static" and
  assumes a utility class needing a private constructor, but a Spring
  Boot entry point's `public static void main(...)` is the standard,
  correct idiom). Excluded project-wide rather than suppressed per-class,
  since it recurs identically in every service.

PMD is also configured with `excludeRoots` pointing at
`target/generated-sources/avro`, so Avro-generated POJOs are skipped
entirely rather than analyzed and suppressed. Generated code isn't
something anyone hand-edits to satisfy a style rule, so excluding the
source root is the correct fix — the same reasoning as excluding a
recurring rule ruleset-wide, just applied at the source-root level
instead of the rule level. This was discovered as a real finding (124
violations on `sandbox-avro-schemas`'s generated sources) once
Spotless/PMD were bound into the build lifecycle, not designed
preemptively.

New rules get added or tuned as real findings come up, not preemptively —
consistent with the project's general "earn its place" philosophy for
tooling (same reasoning already applied to `sandbox-arch-rules` in M0). A
genuine finding on non-boilerplate code (like `sandbox-arch-rules`'
`MissingStaticMethodInNonInstantiatableClass`, addressed via a documented
`@SuppressWarnings` on that specific class) is suppressed individually,
not excluded ruleset-wide — the distinction is whether the false positive
is a one-off (suppress locally) or a structural, recurring pattern
(exclude in `pmd-ruleset.xml`).

PMD findings are treated as a **signal, not a hard gate**, matching the
same JaCoCo philosophy already stated in `sandbox-context-document.md`
Section 5 — a real finding worth discussing beats a build blocked on a
rule that doesn't fit this specific case.

## Naming conventions

Following the reference package layout in `sandbox-context-document.md`
Section 4.1:
- `*UseCase` — application-layer orchestration classes (`CreateOrderUseCase`)
- `*Controller` — inbound REST adapters (`OrderController`)
- `*Repository` — persistence ports (interfaces in `domain/`)
- Adapter implementations of a port are prefixed by their technology
  (`JpaOrderRepository`, `InMemoryOrderRepository`) — the port itself
  stays technology-agnostic in its name (`OrderRepository`, not
  `OrderRepositoryPort` or similar redundant suffixing)

These naming conventions are intended to eventually become explicit
ArchUnit rules in M10 (`sandbox-arch-rules`), turning this documented
convention into an enforced one — not yet wired as of M2.

## Structural role comments

Every domain/application/adapter class carries a short Javadoc block at
the top stating its structural role (aggregate root, value object, domain
event, port, adapter, use case, etc.) plus a one-line *why* — not a
restatement of what the code already shows. Applied as each class is
written, not retrofitted afterward. Established in M2 on `Order`,
`OrderLine`, `OrderCreatedEvent`, `DomainEvent`, `OrderRepository`, and
`InMemoryOrderRepository` — same discipline every service follows going
forward.

## Where this gets wired in

Spotless and PMD are configured once, in the parent `pom.xml`'s
`<build><plugins>` (not `pluginManagement` — plugins declared directly
under `<plugins>` are inherited *and executed* by every child module
automatically, which is what lets every module enforce the same rules
without redeclaring anything). Both checks are bound into the standard
Maven lifecycle via `<executions>`, not left as manual goals:

- `spotless:check` runs at the `validate` phase — first in the
  lifecycle, so a formatting problem fails fast before compiling or
  testing anything.
- `pmd:check` runs at its default `verify` phase, after compile and
  test.

This means `mvn verify` (and anything that includes it, like `install`)
fails the build automatically on unformatted code or a PMD violation —
`spotless:apply`/`spotless:check` and `pmd:check` are no longer commands
that have to be remembered and run by hand.