# 36: M07 quiz: The testing strategy

**What to build:** The quiz for M07: five questions that test whether the reader understood which starter provides which slice, what replaced the removed mock-bean annotation, and when a container-backed test is worth its cost, each with an answer that explains itself.

**Blocked by:** 35

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

The quiz was drafted with the doc (ticket 33); this ticket graded it against
its criteria and rebuilt the one question that failed the grade.

- **Grading jury (the ticket's named understandings):** the drafted quiz
  covered the first two well - question 1 asks the slice row as a failing
  compile with two fixes that each undo half the move (the artifact no
  longer carries the annotation, the starter no longer brings the
  artifact), question 2 asks the mock-bean row as the migration *order*
  the deprecation's shape records (migrate on Boot 3.4+ first, `@MockitoBean`
  already in 3.5's `spring-test`, so the 4.1.1 weekend changes nothing about
  the tests). The third named understanding - *when a container-backed
  test is worth its cost* - went untested: question 3 asks the full
  context's cost against the slice's, and question 4 asked the container
  row's *mechanics* ("name everything that has to change for 2.0.5"),
  never whether the container earns its place.
- **Question 4 rebuilt** (the container's cost): the scenario is the
  suite's own shape - green on a machine with no Docker daemon,
  `TitleRepositoryTests` guarded `@EnabledIfDockerAvailable` - put to a
  reviewer who calls the container class "a second opinion the suite pays
  a daemon for: green on H2 already." The answer reverses the premise:
  the class's three assertions are claims about the engine the library
  ships on, which the H2 rig has never asked - the migration's unique
  constraint refused by PostgreSQL, not by "H2's courtesy" (on H2 such a
  test passes without asking the engine a question at all); rows written
  in plain SQL read back into the `Title` record; the derived query run
  as real SQL on the second engine. It then states both halves of the
  worth: a boundary claim (the slices' status/medium/payload answers) is
  exactly what the container is not worth - one container class, like
  one full-context home - and the cost when paid is paid honestly, a
  no-daemon skip being a skip, never a green tick. Claims checked
  against the Solution module: `TitleRepositoryTests` is the suite's only
  container class, `@SpringBootTest @EnabledIfDockerAvailable` with
  `@Container @ServiceConnection static PostgreSQLContainer` and three
  tests - `theMigrationConstraintsHoldOnTheSecondEngine` expecting
  `DataIntegrityViolationException` containing "unique constraint",
  `aTitleWrittenInSqlIsWhatTheCatalogueReads`, and
  `aTitlesCopiesAreReadByTheDerivedQueryOnPostgresql`; the default rig is
  `jdbc:h2:mem:library` (verified in `application.yml`).
- **Questions 1, 2, 3 and 5 kept**: one defensible answer each, none
  guessable from wording alone - question 1's answer needs the exact
  artifact and package names, question 2's the deprecation's shape,
  question 3's the measured boots and the wiring-assertion criterion,
  question 5's the claim-over-source distinction (both files break at the
  rename; the first's claims survive, the second *has* only arrangement
  claims and is retired).
- **The old Testcontainers 1.x to 2.x mechanics question was dropped to
  make room** - the same trade ticket 31 made for M06: the artifact
  renames and the moved import are the mechanics pattern question 1
  already asks for the slice row, the delta itself stays fully taught in
  the doc's table and Testcontainers section, and the rebuilt answer
  still anchors the row (`testcontainers-postgresql`,
  `@ServiceConnection`). No code changed; the doc edit is the
  resolution.

## Comments

- **2026-10-23 · agent** — Graded the ticket-33 quiz against the
  acceptance criteria; one question failed the jury (the
  container-worth-its-cost understanding untested) and was rebuilt with
  claims checked off the resolved lab code. Doc edit is the resolution;
  no code changed.
