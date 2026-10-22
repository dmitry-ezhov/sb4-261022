# 24: M05 lab start: Persistence: JDBC, JPA and migrations

**What to build:** The Start module for M05: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers replacing the in-memory catalogue with a real Title and Copy schema, a repository and a migration, running the same lab on H2 and on PostgreSQL through Testcontainers.

**Blocked by:** 23

**Status:** ready-for-agent

- [ ] the Start module exists and is listed in the labs aggregator
- [ ] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [ ] its diagnostics fail, and each failure message names what is missing
- [ ] the default build stays green; the diagnostics run is red
- [ ] the application is the one the previous Module left, extended rather than rewritten

## Comments

- **2026-10-22 · acceptance audit** — Not started, but there is scratch work in
  the working tree: `labs/lab-05-persistence-start/` and
  `labs/lab-05-persistence-solution/` hold a copy of the M04 application
  (both sides are byte-identical, and neither has a POM, a diagnostics
  package, a schema or a migration). They are not in `labs/pom.xml`, so the
  reactor does not see them. Treat them as a starting point for the M05 POM
  and the schema, not as a half-finished Lab.
