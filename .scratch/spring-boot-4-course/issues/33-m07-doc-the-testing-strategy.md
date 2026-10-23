# 33: M07 doc: The testing strategy

**What to build:** The Module doc for M07: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers JUnit 5, the Boot 4 test starters, slice tests versus a full context, the annotation that replaced the removed mock-bean annotation, Testcontainers, and what makes a test survive refactoring, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 32

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering the test starters, slices, and the replaced mock-bean annotation
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

The doc is `modules/07-testing-strategy.md`, following its structure from
M05 and M06: what the reader knew in Boot 2, a three-column delta table over
2.7.18 / 3.5.16 / 4.1.1, how it works now, the Lab, a quiz and a checklist.

Every claim in the delta table was checked against the version in use
(4.1.1) and against the versions the neighbours promise (2.7.18 and 3.5.16),
never from memory:

- the three test-starter POMs were read from Maven Central - 2.7.18's shows
  `spring-boot-test-autoconfigure` inside it with JUnit Jupiter 5.8.2,
  Mockito 4.5.1 and `spring-test` 5.3.31; 3.5.16 the same shape (5.12.2,
  5.17.0, 6.2.19); 4.1.1 the same name again (Jupiter **6.0.3**, Mockito
  5.23.0, `spring-test` 7.0.9). Boot 4's own `spring-boot-test-autoconfigure`
  jar was listed: only `@JsonTest` and the jdbc-initialisation periphery are
  left in it - the slices are gone from the starter.
- the slice artifacts and their packages were read off the resolved jars -
  `spring-boot-webmvc-test` 4.1.1 holds `@WebMvcTest` and
  `@AutoConfigureMockMvc` at `org.springframework.boot.webmvc.test.autoconfigure`;
  `spring-boot-starter-data-jpa-test`'s existence was checked on Central.
- `@MockBean`'s fate was read, not remembered: 2.7.18's and 3.5.16's
  `spring-boot-test` both carry `org.springframework.boot.test.mock.mockito.MockBean`,
  3.5.16's class file is `@Deprecated(since="3.4.0", forRemoval=true)`, and
  4.1.1's `spring-boot-test` jar contains no such package - while
  `MockitoBean`/`@MockitoSpyBean` were verified in `spring-test` 7.0.9 at
  `org.springframework.test.context.bean.override.mockito` and in 3.5.16's
  `spring-test` 6.2.19, so the replacement predates the removal.
- Testcontainers 2.0.5 was verified through its jars and the published
  artifact matrix: renamed coordinates (`testcontainers-junit-jupiter`,
  `testcontainers-postgresql`; the old ones stop at 1.x), annotations still
  at `org.testcontainers.junit.jupiter`, `PostgreSQLContainer` re-homed to
  `org.testcontainers.postgresql` with the old class surviving as a
  deprecated bridge, and no longer self-typed. Boot 2's non-management of
  Testcontainers was checked in 2.7.18's published BOM, Boot 3's
  `1.21.4` in 3.5.16's.

The compile check of the snippets is a scratch Maven module (offline,
JDK 25, `spring-boot-dependencies` 4.1.1 BOM) that compiled each snippet
exactly as it appears in the doc. It caught one real API delta, which the
doc now teaches rather than repeats: Testcontainers 2 dropped the
self-typed generic, so `new PostgreSQLContainer<>("postgres:17")` is no
longer valid and `new PostgreSQLContainer("postgres:17.11")` is what
compiles. Measurements cited in the doc (slice 1.4 s vs full context 2.7 s
booting the M06 lab application) were taken against the real
`lab-06-validation-and-errors-solution` classes on one machine, once, and
are recorded with that context.

The Lab pointer names `lab-07-testing-start` per the lab index, with the
diagnostics command `./mvnw -f labs -pl lab-07-testing-start verify
-Pdiagnostics`, and the container-diagnostics need-a-Docker-daemon caveat
worded as the labs do - a skip on a no-daemon machine is a skip, never a
pass. Two cross-axis review notes were recorded and left as-is: the
"old coordinates stop at 1.x" cell is verified exactly for `junit-jupiter`
and `postgresql` (and claimed for `jdbc` on the same shape), and the
`lab-07-testing-*` artifact name follows the labs README index and ticket
34's slug and may need a one-string fix when the lab is authored.
