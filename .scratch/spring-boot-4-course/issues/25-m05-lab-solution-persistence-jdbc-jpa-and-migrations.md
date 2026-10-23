# 25: M05 lab solution: Persistence: JDBC, JPA and migrations

**What to build:** The Solution module for M05: the reference implementation of replacing the in-memory catalogue with a real Title and Copy schema, a repository and a migration, running the same lab on H2 and on PostgreSQL through Testcontainers, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 24

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Answer

Built in commits `a789acc` (the module) and `f76eb09` (a diagnostics-command
wording fix found in review).

- **The Solution module** `labs/lab-05-persistence-solution`, written from
  scratch:
  - the schema is `V001__create_title_and_copy.sql` under
    `src/main/resources/db/migration` - the M05 doc's standard SQL exactly:
    identity keys, `varchar`, `boolean not null`, the `title_id` foreign
    key, `isbn` and `barcode` unique - so one migration is taken by both
    engines;
  - the Title reads are catalogue plumbing: `TitleCatalogue` over
    `JdbcClient`, with the row mapped straight into the `Title` payload
    record (the row type *is* the payload type - no entity for a table whose
    rows have no state of their own to change);
  - the Copy reads are the derived-query path: `CopyEntity` (JPA class with
    accessors, `Copy()` for the framework alone) behind `CopyRepository`,
    whose `findByTitleId` returns the `Copy` payload records - the
    entity/payload split is made in the repository method, per the doc;
  - `ddl-auto: validate` in application.yml: Flyway creates, Hibernate
    validates; no `spring.sql.init.*`, no dialect, no compatibility mode;
  - the departure from the M04 payload shapes is deliberate and visible:
    `Title(Long id, ...)` and `Copy(Long id, Long titleId, ...)`, because
    the keys are the migration's identity columns;
  - the M04 endpoints and failure behaviour survive: a Title the database
    does not hold is still a 404 problem detail from the M04 handler.
- **The second engine, and ticket 24's findings resolved:**
  - `spring-boot-starter-flyway` is the starter, as the lab teaches - a bare
    `flyway-core` configures nothing;
  - `flyway-database-postgresql` rides as a **runtime** dependency with a
    comment explaining why: Flyway 12 speaks H2 from `flyway-core` but not
    PostgreSQL, and a solution that could only reach the container from its
    tests would not be one configuration. The M05 doc carries the erratum
    next to the starter trap (the doc edit is the decision ticket 24 left
    to this ticket).
  - `flyway_schema_history` is quoted in the diagnostics already, since the
    H2 history check needs the backward-quoted lowercase name.
- **Both diagnostics pass** for the solution (H2 and PostgreSQL verified
  locally with a Docker daemon), the reactor is green with every gate
  including the coverage floor, and `./mvnw -f labs verify
  -Pdiagnostics-solutions` is green with the Start modules skipped. The
  Start module's diagnostics stay red through `-Pdiagnostics`
  (PersistenceDiagnosticsTest 2 failures, PersistenceOnPostgresql 1), naming
  what is missing.
- **The Solution's own tests** assert the lab's behaviour, not the
  arrangement: `TitleControllerTests` writes rows with the migration's own
  words through `JdbcClient` and asserts what the endpoints answer, including
  the 404 problem detail; the loan, reservation and packaging tests carry
  over unchanged.
- **PMD's `DataClass` was curated out** of the shared ruleset (`config/pmd/
  ruleset.xml`, design category, commented): a JPA entity is a data class by
  the framework's contract - mutable fields, accessors, a no-arg constructor
  - so the rule would flag every entity the labs author and no defect among
  them. This is the first module where the rule fires.

## Comments

- **2026-10-22 · acceptance audit** — Not started. The scratch directory
  `labs/lab-05-persistence-solution/` in the working tree is currently a copy
  of the M04 solution: it has no POM, no schema, no migration, no repository
  and no diagnostics. It is a placeholder, not a reference implementation.
- **2026-10-23 · findings from ticket 24's diagnostics grading** — The M05
  Start module's diagnostics were graded red/green against a throwaway
  implementation; two verified facts apply to this ticket:
  1. **Flyway cannot reach PostgreSQL from `spring-boot-starter-flyway`
     alone.** Boot 4.1.1 manages Flyway 12.4.0, whose database support is
     modularised: booting the migrating application against the container
     failed with `Unsupported Database: PostgreSQL 17.11` until
     `org.flywaydb:flyway-database-postgresql` was on the classpath. The M05
     doc's PG check ran the migration SQL in the container directly, so this
     second starter trap is untaught - decide where the doc gets the erratum
     (doc is ticket 23, already resolved) and where the artifact belongs in
     the Solution POM.
  2. **Flyway names the history table in backward-quoted lowercase** -
     `"flyway_schema_history"` - which on H2 is case-sensitive, so any
     history check must quote it (the Start module's diagnostics already
     do).
