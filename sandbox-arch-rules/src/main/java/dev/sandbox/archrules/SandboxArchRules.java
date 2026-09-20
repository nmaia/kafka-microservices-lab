package dev.sandbox.archrules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Home for the sandbox's shared ArchUnit rules.
 *
 * <p>Rules are parametrized by basePackage rather than hardcoded to one service - this module is
 * depended on by every service's test suite (see its own {@code pom.xml} description), so a
 * teammate plugging in a new service (payment-service, inventory-service, ...) reuses the same rule
 * definitions against their own package.
 *
 * <p>M2: domain must not depend on Spring/adapters/JPA. Grows through M3-M8 (application must not
 * depend on adapters directly, bounded-context isolation between services, only domain/application
 * may write to the outbox repository), hardened at M10 (naming conventions, full layer rules across
 * every service).
 *
 * <p>Deliberately not over-specified early: a rule is added here only once it maps to an actual
 * correctness property that was violated or nearly violated in a real service, not preemptively for
 * coverage.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class SandboxArchRules {

  private SandboxArchRules() {}

  public static ArchRule domainDoesNotDependOnSpring(String basePackage) {
    return noClasses()
        .that()
        .resideInAPackage(basePackage + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..")
        .because("the domain layer must stay framework-agnostic (Hexagonal)");
  }

  public static ArchRule domainDoesNotDependOnAdapters(String basePackage) {
    return noClasses()
        .that()
        .resideInAPackage(basePackage + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(basePackage + ".adapters..")
        .because(
            "dependencies point inward (Hexagonal) - the domain layer must not know adapters exist");
  }

  public static ArchRule domainDoesNotDependOnJpa(String basePackage) {
    return noClasses()
        .that()
        .resideInAPackage(basePackage + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..", "javax.persistence..")
        .because(
            "the domain layer must stay persistence-agnostic - JPA belongs in the adapter"
                + " (M3's JpaOrderRepository), never annotated directly onto the aggregate");
  }
}
