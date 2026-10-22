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
