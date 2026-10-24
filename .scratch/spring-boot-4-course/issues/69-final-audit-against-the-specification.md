# 69: Final audit against the specification

**What to build:** Every acceptance criterion in the specification is checked against the repository as it stands, and the verified-versions table is re-confirmed against a clean build, so that the course's claims are the course's facts.

**Blocked by:** 68, 70

**Status:** resolved

- [x] every acceptance criterion is checked and the result recorded
- [x] the reactor build is green from a clean checkout
- [x] the diagnostics of every Solution module are green
- [x] the verified-versions table is re-confirmed and dated

## Comments

- **2026-10-22 · partial audit** — Three of the four criteria were checked
  today, against the repository as it stands:
  - `./mvnw -f labs clean verify` is green from a clean tree, with every
    gate the build runs enabled - Spotless, PMD, SpotBugs, JaCoCo and the
    Enforcer.
  - `./mvnw -f labs verify -Pdiagnostics-solutions` is green: every
    Solution module's diagnostics pass, and the Start modules are skipped.
  - The verified-versions table in `labs/README.md` was re-confirmed against
    the resolved classpath and the wrapper: Boot 4.1.1, Framework 7.0.9,
    Jackson 3.1.5 (`tools.jackson`), `jackson-annotations` 2.21,
    Testcontainers 2.0.5, JDK 25.0.4.1 (Temurin), Maven 3.10.0, and every
    plugin version in the table.
- **Still open**: the audit itself - every acceptance criterion in the
  specification checked and the result recorded - cannot be written until the
  Modules exist. M05 onwards are unwritten, and M05 to M13 are the bulk of
  the specification's content.
- **Found during the audit**: the Checkstyle gate is configured but never
  bound to a phase, so `verify` does not run it - and run by hand it reports
  21 violations in the course's own code. Tracked as ticket 70; it should
  be closed before this audit is written.

## Answer

The audit is complete, run 2026-10-25 against a clean tree at `ca37f9f`
(both blockers, 68 and 70, resolved and merged). Three criteria were first
checked on 2026-10-22, before the last Modules were written and before
ticket 70 wired the Checkstyle gate - all three were re-run today, not just
carried over.

### The two seams, re-run from the clean tree

- `./mvnw -f labs clean verify` - **green** across the reactor
  (26 lab modules + `labs-support`), 4m33s, with every gate executing and
  failing the build on purpose: `spotless:3.10.4:check`,
  `pmd:3.28.0:check` (PMD 7.17.0), `spotbugs:4.10.5.0:check`,
  `checkstyle:3.6.0:check` (Checkstyle 14.3.0 - now bound to `verify`, per
  ticket 70, so this run covers what the 2026-10-22 audit could not),
  `jacoco:0.8.15:check`, and `enforcer:3.6.3:enforce`. No
  plugin-parameter warnings in the log.
- `./mvnw -f labs verify -Pdiagnostics-solutions` - **green**. All 17
  diagnostics classes ran on the Solution modules (Start modules skipped by
  `diagnostics.skip=true`), including `PersistenceOnPostgresqlDiagnosticsTest`
  and `TransactionsOnPostgresqlDiagnosticsTest`, which ran against real
  Testcontainers PostgreSQL instances, not assumptions.
- The asymmetry re-proven on the other side:
  `./mvnw -f labs/lab-05-persistence-start verify -Pdiagnostics` is red
  with named failures (`theCatalogueIsTheDatabase`, `theSchemaIsOwnedByAMigration`,
  `theCatalogueIsTheDatabaseOnPostgresql`) - Start red, Solution green.

### The verified-versions table, re-confirmed row by row

The resolved classpath (three representative modules), the wrapper
distribution descriptor, the POM properties and the build log each row:

| Row | Confirmed as |
|---|---|
| Spring Boot 4.1.1 | jars on the test classpath |
| Spring Framework 7.0.9 | jars (`spring-core` … `spring-webmvc`) |
| Spring Security 7.1.1 | jars (`spring-security-core` … `-test`) |
| Jackson 3.1.5 (`tools.jackson`) | `jackson-core`, `jackson-databind` 3.1.5 |
| jackson-annotations 2.21 | coexists in the resolved set |
| Testcontainers 2.0.5 | `testcontainers`, `-jdbc`, `-junit-jupiter`, `-postgresql` |
| JDK 25.0.4.1 (Temurin) | `Java 25.0.4.1` in the surefire runs |
| Maven 3.10.0 | wrapper `distributionUrl` |
| Spotless 3.10.4 / Palantir 2.102.0 | build log + POM property |
| PMD 3.28.0 / PMD 7.17.0 | build goal + property |
| SpotBugs 4.10.5.0, JaCoCo 0.8.15, Checkstyle 3.6.0 / 14.3.0, Enforcer 3.6.3, Failsafe 3.6.0 | build goals |
| Sonar 5.8.0.7211, Versions 2.23.0 | POM properties (nominal, profile-gated / out of the default build) |
| WireMock 3.13.2 (`wiremock-standalone`) | classpath, test scope, module-declared |
| ArchUnit 1.5.1, REST Assured 6.2.0, MapStruct 1.6.3 | managed catalogue properties |
| JSpecify 1.0.1 | inherited compile scope, jars on the classpath |

The table in `labs/README.md` is re-dated to 2026-10-25 in the same commit.

### The specification's criteria, checked

- **Thirteen Modules, three tracks (US 7-10, 30, 40; ADR 0006)** - M01 to
  M13 exist under `modules/` as authored docs, each carrying `## Quiz` and
  `## Checklist` sections, and each pairing with a built lab pair under
  `labs/`. `labs/README.md`'s module table and the welcome doc's pacing
  table both match ticket 68's amendment (~38.5 h, Track C M09-M13).
- **Start modules stay green, Solutions are written from scratch (US 10-13,
  ADR 0003)** - proven by the reactor run: every Start module passes its own
  tests, and each Solution module compiles as its own module, not a patch.
- **Diagnostics state the requirement (US 11-12)** - red on Start with named
  failures (re-proven above), green on Solution, one command
  (`-Pdiagnostics`, `-Pdiagnostics-solutions`).
- **Quality gates (US 16-21)** - Spotless/Palantir, PMD, SpotBugs,
  Checkstyle and JaCoCo all run and fail the build by default; the
  `quality-lenient` profile softens them (verified by ticket 70 and the
  lenient actions it re-ran); the Enforcer's JDK rule names the requirement
  in its message and Maven 3.9.11+ with the reason; the wrapper pins 3.10.0.
- **JSpecify (US 28)** - ✗ **not satisfied. See the finding below.**
- **Feature-first packages, one vocabulary (US 25-27, 35)** - lab packages
  are `com.example.library.<feature>` (e.g. `loan`, `member`, `reservation`,
  `availability`, `book`, `api`); `GLOSSARY.md` at the root; ADR 0002 pins
  the Title/Copy split.
- **One parent POM (US 31-33)** - every version the audit confirmed reads
  from `pom.xml` alone; children repeat only declared dependencies, and
  `ModulePom.java` (in diagnostics) audits that inheritance mechanically.
- **Boot 4 specifics (US 2-5, 29)** - `spring-boot-starter-webmvc` plus a
  separate `spring-boot-starter-webmvc-test`, both resolved from the
  classpath; RFC 9457 problem details served by `ApiExceptionHandler`, with
  a diagnostics test naming the `application/problem+json` media type;
  payload types are records (`LoanRequest`, `ReservationRequest`, `Loan`,
  `Member`, `Reservation`).
- **Data strategy (US 23-24, ADR 0005)** - H2 is the default in every lab;
  the M05/M07/M08 Solution modules pull Testcontainers' PostgreSQL in and
  the Postgres diagnostics prove it against a daemon.
- **Sonar behind a profile (US 36-38)** - `sonar` profile exists and keeps
  host/organisation/token on the command line; `.github/workflows/verify.yml`
  runs the same two learner commands and asserts every Solution module's
  diagnostics - the specification's CI story, as written.
- **Decision records and the backlog (US 34, 39)** - ADRs 0001-0006 under
  `docs/adr/`, and the backlog under `.scratch/spring-boot-4-course/issues/`
  with this ticket's record now closing the last entry.

### Findings

**One criterion does not hold.** The specification requires "the code uses
JSpecify null-safety annotations throughout" (US 28), and M01 teaches
"Boot 4's own API is annotated with JSpecify annotations, and the course
code is too" - but no lab source file carries a JSpecify annotation: no
import, no `@NullMarked`, no `package-info` anywhere in
`labs/*/src/{main,test}`. The `jspecify` jar is inherited from the parent
(as designed) and otherwise unmentioned except as a POM-audit literal in
`ModulePom.java`. The course's docs overclaim the practice it is teaching.

Tracked as **ticket 71** (Put JSpecify null-safety annotations on the lab
code). It is the only gap between the audit and a fully faithful repository.

Everything else the specification claims about the repository was checked
today, and holds.
