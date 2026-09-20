package dev.sandbox.orderservice;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import dev.sandbox.archrules.SandboxArchRules;

// DoNotIncludeTests: without it, ArchUnit would also import order-service's own test classes
// (e.g. OrderTest, which sits in the domain package) and check them against these rules too -
// test code legitimately uses JUnit/AssertJ/reflection, which isn't what these rules are meant
// to police.
@AnalyzeClasses(
    packages = "dev.sandbox.orderservice",
    importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

  @ArchTest
  static final ArchRule domainDoesNotDependOnSpring =
      SandboxArchRules.domainDoesNotDependOnSpring("dev.sandbox.orderservice");

  @ArchTest
  static final ArchRule domainDoesNotDependOnAdapters =
      SandboxArchRules.domainDoesNotDependOnAdapters("dev.sandbox.orderservice");

  @ArchTest
  static final ArchRule domainDoesNotDependOnJpa =
      SandboxArchRules.domainDoesNotDependOnJpa("dev.sandbox.orderservice");
}
