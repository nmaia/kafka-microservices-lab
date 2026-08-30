package dev.sandbox.archrules;

/**
 * Home for the sandbox's shared ArchUnit rules.
 *
 * <p>Intentionally empty at M0 - there is no service code yet to check structure
 * against. Rules get added starting M2 (domain must not depend on
 * Spring/adapters/JPA), grow through M3-M8 (application must not depend on
 * adapters directly, bounded-context isolation between services, only
 * domain/application may write to the outbox repository), and are hardened at
 * M10 (naming conventions, full layer rules across every service).
 *
 * <p>Deliberately not over-specified early: a rule is added here only once it
 * maps to an actual correctness property that was violated or nearly violated
 * in a real service, not preemptively for coverage.
 */
public final class SandboxArchRules {

    private SandboxArchRules() {
    }
}
