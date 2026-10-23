# 26: M05 quiz: Persistence: JDBC, JPA and migrations

**What to build:** The quiz for M05: five questions that test whether the reader understood when to reach for JDBC rather than JPA, what a migration owns, and what a connection pool owns, each with an answer that explains itself.

**Blocked by:** 25

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

The quiz was drafted with the doc (ticket 23); this ticket graded it against
its criteria and rebuilt the two questions that failed the grade.

- **Grading jury (the ticket's named understandings):** the drafted quiz
  covered *what a migration owns* (the `flyway-core` silence, and why
  `schema.sql`/`data.sql` retire for Flyway) but nothing tested *when to
  reach for JDBC rather than JPA* - the module's central teaching, and the
  hand-written SQL row - and its connection-pool question was trivia whose
  answer was guessable from the wording ("do the keys survive?" telegraphs
  "yes").
- **Question 1 rebuilt** (hand-written SQL row): the ISBN lookup - JPA
  repository or `JdbcClient`? One defensible answer: a read whose rows have
  no state of their own to change is catalogue plumbing, and `JdbcClient`
  maps the row straight into the payload record; a Copy the library acts on
  earns the entity and the derived repository. The tools coexist - that is
  what the row records: `JdbcTemplate` *and* `JdbcClient` in `spring-jdbc`
  7.0.9 - and the choice is model-by-model, not application-by-application.
- **Question 4 rebuilt** (connection pool row): what does the
  `spring.datasource.hikari.*` layer own, and what does it *not* own. The
  pool owns connections and their life - count, acquisition wait,
  retirement; it does not own the schema, the reads or the transaction
  boundaries. The unchanged three-major property spellings become the
  answer's proof of layering, not a trivia fact to recall.
- **Every new claim checked against the resolved artifacts**, not written
  from memory:
  - `JdbcClient` injectable in an application whose only persistence
    starter is `spring-boot-starter-data-jpa` - proven by the working
    `TitleCatalogue` in this Lab's own Solution module; the POM declares
    no `spring-boot-starter-jdbc`;
  - `max-lifetime`, `keepalive-time`, `idle-timeout` grow from setters
    present in HikariCP 7.0.2's jar (`setMaxLifetime`, `setKeepaliveTime`,
    `setIdleTimeout`), read off the resolved artifact;
  - `@Transactional`'s enablement lives in `spring-boot-transaction` 4.1.1
    and is absent from `spring-boot-autoconfigure` 4.1.1 - both read off
    the jars (`org.springframework.boot.transaction.autoconfigure
    .TransactionAutoConfiguration` in the former, zero matches in the
    latter);
  - remaining questions' claims (the `spring-boot-flyway` extraction, the
    `spring.sql.init.mode=embedded` default, the `@DataJpaTest` package
    and artifact move) were verified under ticket 23 and carry over.
- Questions 2, 3 and 5 kept: verified, one defensible answer each,
  unguessable from wording. Questions 2 to 5 each name their delta row;
  question 1 anchors to the row the ticket needed covered.
- The M02-style starter-name question ("Does the Boot 4 upgrade change
  that line?") was dropped to make room - half of its answer was
  guessable from the shape of the question.

## Comments

- **2026-10-23 · agent** — Graded the ticket-23 quiz against the
  acceptance criteria; two questions failed and were rebuilt with
  artifact-checked answers. Doc edit is the resolution; no code changed.
