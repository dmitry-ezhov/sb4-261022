# 27: M05 checklist: Persistence: JDBC, JPA and migrations

**What to build:** The checklist for M05: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 26

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers the schema and its migrations are versioned
- [x] it covers no in-memory catalogue is left behind
- [x] it covers the same lab is green on both databases
- [x] it covers the repository is exercised by a test, not only by the application
- [x] no item restates a Quality gate that the build already enforces

## Answer

Built in commit `4013937`.

Seven observable properties: the catalogue is the database, the schema belongs
to the migration, the failure behaviour survived the persistence swap, one
schema on two engines, the repository is exercised by a test, the vocabulary is
the glossary's in the database too, and the entity/payload split is an
intentional seam.

The doc already carried a checklist draft, written under ticket 23, that met
five of the criteria. Two gaps remained and both are now items of their own:

- **The same lab is green on both databases.** The draft said the migration is
  portable - standard types, no `MODE=PostgreSQL`, no dialect - which makes the
  *schema* portable but never runs the claim. The item now says the same module
  is green on both: H2 in the module's own build, PostgreSQL through
  `./mvnw -f labs -pl lab-05-persistence-solution verify -Pdiagnostics` with a
  Docker daemon - and that a skipped PostgreSQL diagnostic is no Docker daemon,
  not a pass. That last clause is the item that turns "works on my machine"
  from something the learner might get away with into a named evasion.
- **The repository is exercised by a test, not only by the application.** The
  Solution's `TitleControllerTests` is the reference: rows are written with the
  migration's own SQL and the endpoints are asserted, so the repository is the
  thing being driven. Without such a test, the reads are exercised by a boot
  that asserts nothing about what they return - a solution can serve an empty
  page and every green checkbox above it says nothing.

Every item is checkable by reading the change or running the command named in
it. None restates a gate the build already enforces: the build owns formatting,
the static analysis rules, the coverage floor and the environment guard, and
none of them says anything about where the schema is owned or whether the lab
boots on the second engine.
