# 25: M05 lab solution: Persistence: JDBC, JPA and migrations

**What to build:** The Solution module for M05: the reference implementation of replacing the in-memory catalogue with a real Title and Copy schema, a repository and a migration, running the same lab on H2 and on PostgreSQL through Testcontainers, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 24

**Status:** ready-for-agent

- [ ] the Solution module exists and is listed in the labs aggregator
- [ ] it is written from scratch, not patched from the Start module
- [ ] every diagnostic of the Start module passes against it
- [ ] it meets the coverage floor
- [ ] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [ ] CI asserts its diagnostics through the single diagnostics command

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
