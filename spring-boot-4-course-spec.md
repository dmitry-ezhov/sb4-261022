# Spring Boot 4 Course — Specification

## 1. Purpose

A self-paced text course with code labs that takes a senior Java developer from
"my Spring Boot knowledge stopped at 2.x" to "I can build, test and ship a
Spring Boot 4 application and I know what changed under me since Boot 2".

## 2. Audience

Senior Java developers who:

- wrote production Spring Boot 2.x applications, possibly years ago;
- have not used Spring Boot 3 (no familiarity with `jakarta.*`, Jackson 3,
  modularised starters, the observability stack, Spring Security 6/7);
- are comfortable with Java itself, build tools and SQL.

**Not** the audience: developers new to Java, or developers who want a Spring
Boot 3 course.

## 3. Prerequisites

- JDK 25 (Temurin). Maven comes from the wrapper.
- Docker — optional, only for the Testcontainers labs (M05, M07, M08).

## 4. The sample application

One application runs through the whole course: a **library that lends physical
items to members**. It is small enough to read in a sitting and rich enough to
carry every topic: entities, validation, transactions, security, concurrency.

Domain vocabulary and business rules are fixed in [`GLOSSARY.md`](GLOSSARY.md).

## 5. Tracks and modules

Twelve modules in three tracks. Each module has a doc, a lab (start + solution
module pair), a quiz and a checklist.

| Track | Modules | Theme |
|---|---|---|
| A — The bridge | M01–M04 | What changed between Boot 2, Boot 3 and Boot 4; the module map; typed configuration; the web stack. |
| B — Data & testing | M05–M08 | Persistence, validation and error handling, testing strategy, transactions and consistency. |
| C — The platform | M09–M12 | Actuator and observability, Spring Security 7, resilience and virtual threads, native images and production. |

The full module table with times and lab paths is in the
[README](README.md#module-table).

## 6. The build

### 6.1 Structure

- The **root `pom.xml` is the parent and the aggregator**. It owns every
  dependency version, plugin version and quality gate.
- `labs/pom.xml` is an aggregator that configures nothing; it lists the lab
  modules.
- Every lab module declares only its own dependencies and its own application
  class.

### 6.2 Toolchain

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.1 | Latest stable. 4.2.x is still a milestone. |
| Spring Framework | 7.0.9 | Managed by Boot. |
| Java | 25 | `maven.compiler.release=25`. |
| Maven | 3.10.0 | Pinned by the wrapper; Enforcer requires ≥ 3.9.11. |

### 6.3 Dependencies

The parent manages the whole catalogue; children inherit only what they always
need.

- **Managed**: Boot starters, Testcontainers 2.0.5 (BOM), WireMock 3.13.2
  (`wiremock-standalone`), ArchUnit 1.5.1, REST Assured 6.2.0, MapStruct 1.6.3.
- **Inherited by every module**: `spring-boot-starter-test` (test scope),
  `jspecify` (compile scope).

### 6.4 Quality gates

Configured once in the root parent POM, inherited by every module, failing the
build by default.

| Gate | Tool | Version | Configuration |
|---|---|---|---|
| Formatting | Spotless (Palantir Java Format) | 3.10.4 / 2.102.0 | in the root POM |
| Static analysis | PMD | 3.28.0 (PMD 7.17.0) | `config/pmd/ruleset.xml` |
| Bug patterns | SpotBugs | 4.10.5.0 | `config/spotbugs/exclude.xml` |
| Style & correctness | Checkstyle | 3.6.0 (14.3.0) | `config/checkstyle/checkstyle.xml` |
| Coverage | JaCoCo | 0.8.15 | 60% line / 50% branch |
| Environment | Maven Enforcer | 3.6.3 | JDK 25, Maven 3.9.11+ |
| Analysis | Sonar | 5.8.0.7211 | `sonar` profile only |

Also configured: Maven Wrapper (3.10.0), Maven Failsafe (`**/*IT.java`),
versions-maven-plugin (2.23.0), git-commit-id (managed by Boot).

### 6.5 Profiles

| Profile | Effect |
|---|---|
| `quality-lenient` | Turns every gate into a warning. Opt-in, visible in the command line. |
| `diagnostics` | Runs only the diagnostic tests (the surefire default excludes them). |
| `sonar` | Runs the Sonar scanner. Host and credentials come from the command line. |

## 7. Lab conventions

- A lab is a **pair** of modules: `lab-NN-<slug>-start` and `lab-NN-<slug>-solution`.
- Start modules compile, are formatted, pass every gate except the coverage
  floor, and pass their own tests. The defects are behavioural.
- Diagnostics live in `src/test/java/.../diagnostics/` and are excluded from the
  default build.
- The solution module is written from scratch, not patched from the start module.
- Packages are feature-first: `com.example.library.<feature>`.

## 8. Documentation conventions

- `modules/NN-<slug>.md` — the module doc: concepts, internals, the lab, the
  quiz, the checklist.
- `modules/00-welcome.md` — environment, orientation, workflow, pacing.
- `labs/README.md` — the lab index, the run commands, the gates, the verified
  versions.
- `tasks/T-NN-<slug>.md` — one file per unit of work.
- `docs/adr/` — decisions that are hard to reverse, surprising, and the result
  of a real trade-off.

## 9. Acceptance criteria

The course is done when:

1. Every module has a doc, a start module, a solution module, a quiz and a
   checklist.
2. `./mvnw -f labs verify` is green on the whole reactor.
3. `./mvnw -f labs verify -Pdiagnostics` is green on every solution module and
   red on every start module.
4. `./mvnw -f labs verify -Pdiagnostics` on a start module becomes green after a
   learner applies the documented fix — and the CI workflow checks this for the
   lab 01 pair.
5. Every module doc states the Boot 2 → Boot 3 → Boot 4 delta it covers.
6. Every version in the build is verified against a real build on the date
   recorded in `labs/README.md`.
