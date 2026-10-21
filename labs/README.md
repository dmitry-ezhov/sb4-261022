# Labs

Every lab is a pair of Maven modules:

- a **start module** — compiles, is formatted, passes every static analysis rule
  and passes its own tests, but does not yet do what the lab asks for;
- a **solution module** — the reference implementation of the same lab.

The gap between them is described by **diagnostic tests**. Diagnostics are
excluded from the default build, so the start module stays green while its
behaviour is still wrong. Run them explicitly:

```bash
./mvnw -f labs -pl lab-01-boot2-to-boot4-start verify -Pdiagnostics   # one module
./mvnw -f labs verify -Pdiagnostics-solutions                       # every Solution module
```

The first command is the learner's: it runs a Start module's diagnostics, which
fail until the Lab is done. The second is CI's: it runs the diagnostics of every
Solution module and skips the Start modules, so it is green when the reference
implementations are correct.

## The lab index

| Lab | Module | Start | Solution | Infra |
|---|---|---|---|---|
| 01 | M01 Boot 2 → Boot 4: what changed | `lab-01-boot2-to-boot4-start` | `lab-01-boot2-to-boot4-solution` | none |
| 02 | M02 The Boot 4 module map & multi-module build | `lab-02-module-map-and-build-start` | `lab-02-module-map-and-build-solution` | none |
| 03 | M03 Typed configuration & validation | `lab-03-typed-configuration-start` | `lab-03-typed-configuration-solution` | none |
| 04 | M04 The web stack: MVC, Jackson 3, ProblemDetail | `lab-04-web-stack-jackson3-start` | `lab-04-web-stack-jackson3-solution` | none |
| 05 | M05 Persistence: JDBC, JPA, Flyway | `lab-05-persistence-start` | `lab-05-persistence-solution` | Postgres (opt.) |
| 06 | M06 Validation & error handling | `lab-06-validation-and-errors-start` | `lab-06-validation-and-errors-solution` | none |
| 07 | M07 Testing: slices, Testcontainers, REST Assured | `lab-07-testing-start` | `lab-07-testing-solution` | Postgres (opt.) |
| 08 | M08 Transactions & consistency | `lab-08-transactions-start` | `lab-08-transactions-solution` | Postgres (opt.) |
| 09 | M09 Actuator & observability | `lab-09-observability-start` | `lab-09-observability-solution` | none |
| 10 | M10 Spring Security 7 | `lab-10-security7-start` | `lab-10-security7-solution` | none |
| 11 | M11 Resilience & virtual threads | `lab-11-resilience-and-concurrency-start` | `lab-11-resilience-and-concurrency-solution` | none |
| 12 | M12 Native images & the production capstone | `lab-12-capstone-start` | `lab-12-capstone-solution` | Docker |

Lab modules are added to `labs/pom.xml` by the task that authors them. Do not
pre-create module directories.

## Conventions

- **Package layout** — feature-first vertical slices: `com.example.library.book`,
  `com.example.library.member`, `com.example.library.loan`,
  `com.example.library.reservation`. A feature keeps its controller, service and
  repository together.
- **Domain vocabulary** — see [`GLOSSARY.md`](../GLOSSARY.md). `Title` is a work,
  `Copy` is a physical item, a `Member` borrows, a `Loan` is an active
  borrowing, a `Reservation` is a hold on a `Title`.
- **A lab is a pair, not a diff** — the solution module is written from scratch,
  not patched. Read it after you have solved the lab, not instead of it.
- **Diagnostic tests** live in `src/test/java/.../diagnostics/` and are named
  `<Subject>DiagnosticsTest`.

## Run commands

```bash
./mvnw -f labs verify                                    # everything, all gates
./mvnw -f labs -pl lab-01-boot2-to-boot4-start verify     # one module
./mvnw -f labs -pl lab-01-boot2-to-boot4-start spring-boot:run   # run it
./mvnw -f labs spotless:apply                            # format
./mvnw -f labs -pl lab-01-boot2-to-boot4-start verify -Pquality-lenient
```

## Quality gates

| Gate | Tool | Configuration | Fails on |
|---|---|---|---|
| Formatting | Spotless + Palantir Java Format | in the root POM | `process-sources` |
| Static analysis | PMD 7.17.0 | [`config/pmd/ruleset.xml`](../config/pmd/ruleset.xml) | `verify` |
| Bug patterns | SpotBugs 4.10.5.0 | [`config/spotbugs/exclude.xml`](../config/spotbugs/exclude.xml) | `verify` |
| Style & correctness | Checkstyle 14.3.0 | [`config/checkstyle/checkstyle.xml`](../config/checkstyle/checkstyle.xml) | `verify` |
| Coverage | JaCoCo 0.8.15 | 60% line / 50% branch | `verify` |
| Environment | Maven Enforcer | JDK 25, Maven 3.9.11+ | `validate` |

Why curated rulesets rather than the stock ones:

- **PMD** — the stock categories flag most Spring code (DTOs, records, fields
  used in one place). The curated set keeps the rules that catch real defects
  and drops the ceremony. Every exclusion carries a comment saying why.
- **Checkstyle** — Spotless already owns whitespace, indentation and import
  order, so Checkstyle only covers naming, correctness and structure.
- **SpotBugs** — only the patterns that are intentional here are excluded:
  data classes handing out their own state, and null-analysis noise around
  Spring proxies.

Start modules opt out of the coverage floor only (`<jacoco.skip>true</jacoco.skip>`);
every other gate applies to them.

To work without the gates blocking you:

```bash
./mvnw -f labs verify -Pquality-lenient
```

The profile skips Spotless, PMD, SpotBugs, Checkstyle and JaCoCo. It changes
nothing else, so a green lenient build is not a green build — always finish with
a plain `verify`.

## Verified versions

Checked on 2026-10-22 against the versions actually resolved by the build.

| Component | Version |
|---|---|
| Spring Boot | 4.1.1 (latest stable; 4.2.0-M2 is a milestone) |
| Spring Framework | 7.0.9 |
| Jackson | 3.1.5 (Jackson 3 — `tools.jackson.*`) |
| Testcontainers | 2.0.5 (managed by Boot) |
| JDK | 25.0.4 (Temurin) |
| Maven | 3.10.0 (pinned by the wrapper) |
| Spotless | 3.10.4 |
| palantir-java-format | 2.102.0 |
| maven-pmd-plugin | 3.28.0 (PMD 7.17.0) |
| spotbugs-maven-plugin | 4.10.5.0 |
| jacoco-maven-plugin | 0.8.15 |
| maven-checkstyle-plugin | 3.6.0 (Checkstyle 14.3.0) |
| sonar-maven-plugin | 5.8.0.7211 |
| maven-enforcer-plugin | 3.6.3 |
| maven-failsafe-plugin | 3.6.0 |
| versions-maven-plugin | 2.23.0 |
| WireMock | 3.13.2 (`wiremock-standalone`) |
| ArchUnit | 1.5.1 |
| REST Assured | 6.2.0 |
| MapStruct | 1.6.3 |

Notes that matter:

- **Palantir Java Format needs no extra JVM flags on JDK 25** — it formats
  through a shaded compiler, so `./mvnw spotless:apply` works out of the box.
- **JaCoCo 0.8.15** is the first line with official Java 25 support (0.8.14);
  0.8.13 and older cannot read class file version 69.
- **PMD 7.16+** understands Java 25 language features; 3.28.0 bundles PMD 7.17.0.
- **SpotBugs 4.10.1+** works on JDK 25 (4.10.0 had a fat-jar initialisation
  bug on Java 25).
- **WireMock 3.x is Jackson 2** (`com.fasterxml.*`) while Boot 4 defaults to
  Jackson 3 (`tools.jackson.*`). They coexist, but never serialize application
  types with WireMock's mapper.
