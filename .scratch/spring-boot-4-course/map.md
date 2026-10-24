# Map — Spring Boot 4 course

The implementation map for the feature described in
[`spec.md`](spec.md). One child file per ticket, under
[`issues/`](issues/), numbered from `01`, blockers first.

## Notes

- The course's build lives in the root parent POM; every Module inherits it.
  Nothing in a Lab Module configures a plugin.
- The two seams are the reactor build (`./mvnw -f labs verify`) and the
  diagnostics profile (`-Pdiagnostics`). Every ticket is verified against
  those two commands and nothing else.
- The domain vocabulary is fixed in [`GLOSSARY.md`](../../GLOSSARY.md):
  Title, Copy, Member, Loan, Reservation. A fixture may not mention the
  domain, because every Lab carries its own copy of the domain classes.
- Each Module is delivered as five tickets - doc, lab start, lab solution,
  quiz, checklist - and the chain is strictly linear, so the frontier is
  always a single ticket.

## Decisions so far

- **2026-10-22 · ticket 01** — The authoring guide fixes one structure for
  every Module: the doc skeleton with its seven sections, the delta table
  rules, the quiz format and the checklist format. Shared fixtures live in a
  separate module that publishes a test-jar, because a fixture cannot
  mention the domain. [Answer](issues/01-module-authoring-template-and-shared-test-support.md)
- **2026-10-22 · ticket 02** — Two diagnostics commands, not one:
  `-Pdiagnostics` is the learner's and runs the module you select, red on a
  Start module; `-Pdiagnostics-solutions` is CI's and runs every Solution
  module's diagnostics while skipping the Start modules. A Start module
  declares that with `diagnostics.skip` in its own POM. This refines the
  specification's acceptance criterion 3, which assumed a single command.
  [Answer](issues/02-one-diagnostics-command-that-selects-solution-modules.md)

- **2026-10-22 · ticket 03** — The M01 delta table is checked, not
  remembered: every cell was read off the published POMs and source trees of
  Boot 2.7, 3.0, 3.5 and 4.1.1. Two claims changed as a result - Boot 4
  renamed the JSON starter, and Jackson 3's annotations never moved.
  [Answer](issues/03-m01-doc-boot-2-to-boot-4-what-changed.md)
- **2026-10-22 · tickets 04-05** — The M01 Lab teaches the whole Boot 4
  treatment, so the start module carries all four Boot 2 habits: the hand-
  written validation check, the envelope around a created object, the empty
  error body, and the deprecated starter. Five diagnostics, five named
  failures.
  [Answer](issues/04-m01-lab-start-boot-2-to-boot-4-what-changed.md)
- **2026-10-22 · ticket 02 follow-up** — PMD's `OnlyOneReturn` was curated
  out of the shared ruleset: it is a style rule that fights the guard clause
  and catches no defects.

- **2026-10-22 · ticket 08** — The starter map is read off the published
  POMs, not remembered. Boot 4's starters are per-responsibility, and the
  test starter pulls the whole web stack in at compile scope. Maven's three
  mechanisms - parent, aggregator, BOM import - are taught as three things,
  because they are three things. The M02 Lab is the one Lab whose subject is
  the build, so its Diagnostics read the module's POM.
  [Answer](issues/08-m02-doc-the-boot-4-module-map-and-a-multi-module-build.md)

- **2026-10-22 · tickets 09-10** — M02's Lab is the one Lab whose subject
  is the build, so its Diagnostics read the module's POM rather than an HTTP
  response. The POM is read as text, with XML comments stripped, because the
  JDK's XML parser lives in `javax.xml` and this course teaches that a
  `javax.*` import is a smell. The application also gained an integration
  test that inspects the packaged jar.
- **2026-10-22 · ticket 08 follow-up** — `spring-boot-starter-parent`
  *manages* the `repackage` goal; it never activates it, in Boot 2, Boot 3
  or Boot 4. A module declares the plugin to get an executable jar.
  [Answer](issues/10-m02-lab-solution-the-boot-4-module-map-and-a-multi-module-build.md)

- **2026-10-22 · tickets 11-12** — M02's quiz and checklist close the
  Module. Two claims were checked rather than believed: an imported BOM
  contributes managed versions only (not properties, not plugins), and
  `spring-boot-starter-parent` manages the `repackage` goal without
  activating it.
  [Answer](issues/11-m02-quiz-the-boot-4-module-map-and-a-multi-module-build.md)

- **2026-10-22 · ticket 13** — Configuration changed less than the web
  stack, and the delta table says so: the real change is Boot 3's default
  constructor binding, the Jakarta move in the constraint annotations, and
  Boot 4's `@ConfigurationPropertiesSource`. Two beliefs were corrected by
  checking - `@DefaultValue` is not new, and Boot 4's configuration API
  carries no JSpecify annotations.
  [Answer](issues/13-m03-doc-typed-configuration-and-validation.md)

- **2026-10-22 · tickets 14-15** — M03's Lab is the first behavioural
  configuration Lab: the loan period is bound from `library.loan.period`, and
  an invalid limit must stop the application at startup. The application
  injects its clock, so the lab is the first consumer of the shared fixture
  module. A loan period is a number of days and a due date is a date, so the
  bound `Duration` is converted to days.
  [Answer](issues/14-m03-lab-start-typed-configuration-and-validation.md)

- **2026-10-22 · tickets 14-15** — M03's Lab is the first behavioural
  configuration Lab: the loan period is bound from `library.loan.period`, and
  an invalid limit must stop the application at startup. The application
  injects its clock, so the lab is the first consumer of the shared fixture
  module. A loan period is a number of days and a due date is a date, so the
  bound `Duration` is converted to days.
  [Answer](issues/14-m03-lab-start-typed-configuration-and-validation.md)

- **2026-10-22 · tickets 16-17** — The M03 quiz and checklist close the
  Module. One doc claim was corrected by reading the source: a bare number in
  a `Duration` is a number of milliseconds, not an error.
  [Answer](issues/16-m03-quiz-typed-configuration-and-validation.md)

- **2026-10-22 · ticket 18** — The Jackson 3 story is more than the package
  root: a JSON `null` for a primitive is now an error, while unknown
  properties are still ignored because Boot disables the feature. The
  declarative HTTP client and its starter are new in Boot 4 - Boot 3.5 has
  no `spring-boot-restclient` module at all.
  [Answer](issues/18-m04-doc-the-web-stack-mvc-jackson-3-and-problem-details.md)

- **2026-10-22 · ticket 19** — M04's Lab is the course's first outbound
  dependency: the library asks an availability service before it lends a
  copy. The start module declares the service and does not use it. The
  service is stubbed with WireMock on a dynamic port, and its base URL
  reaches the application through `@DynamicPropertySource`.
  [Answer](issues/19-m04-lab-start-the-web-stack-mvc-jackson-3-and-problem-details.md)

- **2026-10-22 · ticket 20** — The declarative client is built from an
  `@HttpExchange` interface over a `RestClient` whose base URL is
  configuration; a 4xx from the service becomes a typed exception, which is
  what makes "not available" (409) and "unknown copy" (404) two different
  failures. The proxy factory is
  `org.springframework.web.service.invoker.HttpServiceProxyFactory` in
  Framework 7.
  [Answer](issues/20-m04-lab-solution-the-web-stack-mvc-jackson-3-and-problem-details.md)

- **2026-10-22 · acceptance audit** — Every ticket's acceptance criteria were
  checked against the repository as it stands, and the boxes that hold are
  ticked. Tickets 01 to 22 are done: the reactor build is green, every
  Solution module's diagnostics pass, every Start module's diagnostics fail
  with a message that names what is missing, and the verified-versions table
  was re-confirmed against the resolved classpath. Three gaps were found and
  are recorded where they belong: the M02 checklist has no item about the
  reactor being green from the root (ticket 12), the M04 checklist has no
  item about the outbound call's timeout (ticket 22), and the Checkstyle gate
  is configured but never bound to a phase, so `verify` does not run it
  (ticket 70, which blocks the final audit). M05 onwards is unwritten; the
  `labs/lab-05-persistence-*` directories in the working tree are scratch
  copies of the M04 application, not Modules.
- **2026-10-22 · ticket 70** — The Checkstyle gate now runs: `check` is bound
  to `verify` in the parent POM, and the curated ruleset dropped the six
  rules that duplicate Spotless (`WhitespaceAround`, `WhitespaceAfter`,
  `LeftCurly`, `RightCurly`, `NewlineAtEndOfFile`, `FileTabCharacter`) - the
  ruleset's own header said it must not overlap the formatter, and Palantir's
  empty record body (`record Loan() {}`) was the proof. The application class
  is exempt from
  `HideUtilityClassConstructor` by annotation, not by name. The ruleset also
  lints the test sources now: `checkstyle.includeTestSourceDirectory` as a
  *property* was inert, because the `check` goal exposes no user property for
  it, so it lives in the plugin block instead.
- **2026-10-23 · ticket 26** — The M05 quiz was graded against its
  acceptance criteria and two questions rebuilt: the spec named three
  understandings to test - JDBC or JPA, what a migration owns, what a pool
  owns - and the draft covered only one of them. The starter-name question
  went, because its answer was guessable from the question's shape.
  [Answer](issues/26-m05-quiz-persistence-jdbc-jpa-and-migrations.md)

- **2026-10-23 · ticket 27** — The M05 checklist had a ready draft from
  ticket 23; grading it left two criteria open, and both became items of
  their own: the lab is *run* green on both databases (a skipped PostgreSQL
  diagnostic is no Docker daemon, not a pass), and the repository is
  exercised by a test that writes the migration's own SQL and asserts what
  the endpoints answer.
  [Answer](issues/27-m05-checklist-persistence-jdbc-jpa-and-migrations.md)

- **2026-10-23 · ticket 28** — The M06 delta table has no row for the domain
  rules because none of the three Boot versions ever owned them; the table is
  the boundary's. Two claims fell to checking: Framework 6.1's dispatch-level
  parameter validation turned Boot 2's `500` into a `400`, and
  `spring.mvc.problemdetails.enabled` renders only *standard* exceptions (a
  registered `ProblemDetailsExceptionHandler`), so a domain failure stays a
  `500` even with the switch on. Every snippet compiled against the
  application's own types in a throwaway 4.1.1 module.
  [Answer](issues/28-m06-doc-validation-and-error-handling.md)

- **2026-10-23 · ticket 30** — The M06 solution keeps the shared handler but
  gives the title to the request: `LoanRequest` and `ReservationRequest`
  implement a small `InvalidRequestTitle`, and the `@ExceptionHandler` asks
  the failing body whose words to use - one exception type, two features'
  titles, `ProblemDetail` still imported only in `api`. The Loan limit went
  the doc's way verbatim: `LoanService` counts the Member's Active loans
  against `properties.limit()` and throws `LoanLimitExceededException`
  (glossary wording), translated to a `409` titled "Loan limit reached".
  [Answer](issues/30-m06-lab-solution-validation-and-error-handling.md)

- **2026-10-23 · ticket 32** — The M06 checklist had a ready draft from
  ticket 28; grading it left two criteria open, and both became explicit:
  a domain failure is mapped in *exactly one place* (one handler method per
  exception, `ProblemDetail` imported nowhere else), and nothing unmapped is
  answered in invented words - no `@ExceptionHandler(Exception.class)`, the
  switch is for the failures nobody anticipated. Re-run of the solution's
  diagnostics was green.
  [Answer](issues/32-m06-checklist-validation-and-error-handling.md)

- **2026-10-23 · ticket 38** — The M08 delta table carries four rows and one
  deliberate absence: propagation and isolation did not move, so they get no
  row, only the checked proof (decompiled `Transactional`, `Propagation` and
  `Isolation` match across `spring-tx` 5.3.31, 6.2.19 and 7.0.9). Checking
  killed two remembered facts: there never was a
  `JpaTransactionManagerAutoConfiguration` in any Boot version (the `@Bean`
  has always lived on `JpaBaseConfiguration`, now packaged in
  `spring-boot-jpa`), and Jakarta Persistence 3.2 - what 4.1.1 builds against
  - widens `@Version` to `Instant` and `LocalDateTime`. Every snippet was
  compiled and run in a throwaway 4.1.1 probe module.
  [Answer](issues/38-m08-doc-transactions-and-consistency.md)

- **2026-10-23 · ticket 40** — The M08 solution's borrow works on database
  keys at the edge: the controller maps the request's barcode to the Copy
  row (`findByBarcode`, a new derived read) and hands the use case the Copy,
  so no barcode crosses the `@Transactional` boundary. The payload keeps the
  library's words - member id and barcode as named, the loan row's key as
  `id`. Design detail worth keeping: the Loan store's `save` is
  `SimpleJdbcInsert` because `JdbcClient` cannot return a generated key, and
  "one Loan per Copy" is deliberately not a unique constraint - the guard
  statement's `0` is the refusal, and a constraint would rephrase it.
  Unknown barcode is a 404 ("Copy not found"), not a 409: the availability
  service just said yes to it.
  [Answer](issues/40-m08-lab-solution-transactions-and-consistency.md)

- **2026-10-23 · ticket 41** — The M08 quiz graded against its criteria and
  two questions rebuilt: the boundary-placement understanding went untested
  (the draft's questions 1-2 both ask the machinery rows), and the rig
  question was guessable from its wording ("reports 'concurrency proven'
  without a Docker daemon in sight" narrates the verdict). Rebuilt: the
  colleague's two "cleanups" that break the proxy rule and the pool; the
  three-claim partition of what an H2 run can carry. The
  JPA-transaction-manager question was dropped to make room - the same
  trade tickets 26 and 36 made.
  [Answer](issues/41-m08-quiz-transactions-and-consistency.md)

- **2026-10-23 · ticket 42** — The M08 checklist had a ready draft from
  ticket 38; grading it left two criteria open, and both became items of
  their own, each checked off the resolved lab code: no `@Transactional`
  but the use case's (no read-only path opens a boundary, and the Loan
  limit's count is named as inside the boundary on purpose, not a read to
  fix), and no `isolation = ...` anywhere (the absence is the doc's stated
  choice - `READ_COMMITTED` plus the guard statement). The solution's
  diagnostics re-ran green with the PostgreSQL rig actually executing.
  [Answer](issues/42-m08-checklist-transactions-and-consistency.md)

- **2026-10-23 · ticket 43** — The M09 delta table is checked against the
  jars, not remembered: starter contents read off the published POMs of
  2.7.18, 3.5.16 and 4.1.1, endpoint counts by listing the class files (23
  in the 2.7.18 actuator jar, 15 left on 4.1.1), and the behaviour rows
  booted - 4.1.1 probes answer `200` unconfigured
  (`@ConditionalOnBooleanProperty(..., matchIfMissing = true)` in the
  bytecode), the loan counter and the custom indicator read back live, the
  logstash format prints its JSON line from the first boot line. Checking
  killed the habit the quizzes enforce: "Boot 4 auto-detects Kubernetes" is
  2.7's `ProbesCondition`, not 4.1.1's question - the property default is
  true on every platform.
  [Answer](issues/43-m09-doc-actuator-and-observability.md)
- **2026-10-23 · ticket 44** — The M09 lab start is the lab-08 solution's
  application byte for byte plus one diagnostics class, and the
  observability contract is three sockets, two properties and one counter:
  probes `UP` at `/actuator/health/liveness` and `/actuator/health/readiness`
  (4.1.1's `matchIfMissing = true` default - no probe property in the
  change), `library.loans.issued` tagged `title` and read back through
  `/actuator/metrics`, and `management.server.port` with the public socket
  answering `404` for `/actuator/health`. The diagnostics are a real-socket
  rig (`RANDOM_PORT` + `local.management.port`, JDK HTTP client), MockMvc
  being unable to ask a second socket: the first lab where the
  "no third seam" line says nothing new, because the requirement itself is
  about ports, not payloads.
  [Answer](issues/44-m09-lab-start-actuator-and-observability.md)

- **2026-10-24 · ticket 45** — The M09 lab solution is the starter, two
  configuration lines and one increment: `spring-boot-starter-actuator`
  puts the probes on the classpath (4.1.1's `matchIfMissing = true` default
  means no probe property exists in the change),
  `management.server.port: 0` plus `exposure.include: "health,metrics"`
  keep the management surface on its own socket with one step past the
  default exposure, and `LoanService.borrow` counts
  `library.loans.issued` where the Loan row is written, tagged with the
  Title the M08 `Copy` payload already carries. The suite's one new
  assertion reads the registry (one borrow of one fresh Title leaves exactly
  1.0), the suite's read being registry-level because MockMvc cannot ask the
  management socket - the diagnostics own that half. Diagnostics red 3/3 on
  the start, green 3/3 on the solution, CI's single diagnostics command
  green across the whole 20-module reactor.
  [Answer](issues/45-m09-lab-solution-actuator-and-observability.md)

- **2026-10-24 · ticket 46** — The M09 quiz graded against its criteria
  and two questions rebuilt: the ticket named three understandings -
  liveness against readiness, metric rather than log, management surface
  off the public path - and the draft tested none of the first two
  (question 2 asked the probes condition and telegraphed its verdict,
  question 3 the property rename's spelling). Rebuilt: the availability
  client moved into the liveness group - the restart loop against the
  routing decision, published as an event, answered `OUT_OF_SERVICE` -
  and the counter against the INFO line, with the Copy-tag cardinality
  mistake as the second half. The rename question went, the same drop
  tickets 26 and 41 made.
  [Answer](issues/46-m09-quiz-actuator-and-observability.md)

- **2026-10-24 · ticket 47** — The M09 checklist graded against its
  criteria and the three open ones closed: readiness verified as its own
  question beside liveness (the outage moves readiness, not the restart
  decision), a new item asking the metric's question - who reads the
  answer - against the counter's stated purpose, and a new item reading
  the metrics as a stranger would (no [Member](../GLOSSARY.md) tag, the
  global-tag leak the diff can hide nowhere). The custom-indicator item
  gained the conditional its absence in the lab needed.
  [Answer](issues/47-m09-checklist-actuator-and-observability.md)

- **2026-10-24 · ticket 49** — The M10 lab start is the lab-09
  solution's application carried forward untouched with no security starter
  in the build, and the security contract is five diagnostics over the real
  sockets: `401` for the unauthenticated borrow with the catalogue and probe
  still public, `403` for a principal without the `MEMBER` role, the Loan
  row's `member_id` equal to the caller's credentials whether the body named
  somebody else or named nobody at all, the limit counted against the
  principal (at-limit `409` titled "Loan limit reached", under-limit borrow
  through), and the management socket's metrics read closed while
  `/actuator/health/**` stays open. The diagnostics name the principals a
  solution must define - Members `ada`, `bruno`, `cecil`
  (`member-password`/`MEMBER`) and `operator` (`operator-password`/
  `OPERATOR`) - and tests 3 and 4 borrow as different Members so the Active
  loan counts cannot depend on JUnit's order. Diagnostics red 5/5 on the
  start, ordinary build green with every gate but the coverage floor.
  [Answer](issues/49-m10-lab-start-spring-security-7.md)

- **2026-10-24 · ticket 50** — The M10 lab solution is the lab-09 answers
  with the security upgrade written in: the chain as a bean and the lambda
  the only form (two chains - management `@Order(1)` behind a
  `PathPatternRequestMatcher`, the health probe public and the rest
  `OPERATOR`; the API `@Order(2)` with the catalogue's GETs public,
  `MEMBER` on `/api/loans/**` and `anyRequest().authenticated()` closing),
  the delegating `PasswordEncoder` producing every stored `{bcrypt}` value
  for the diagnostics' five principals, the borrow's Member the principal
  (`LoanRequest` keeps only the barcode; the controller passes
  `authentication.getName()`) and the use case's
  `@PreAuthorize("#memberId == authentication.name")` the second lock. The
  own suite asserts behaviour at the real chains riding the slices, the
  wiring home borrows over Basic as `ada`, and two 7.1.1 facts surfaced:
  `PathPatternRequestMatcher` moved to the `.servlet.util.matcher` package,
  and the configurers no longer declare `throws Exception` - which kept the
  shared PMD ruleset un-suppressed. Diagnostics red 5/5 on the start, green
  5/5 on the solution, JaCoCo 87.6%/87.5%, the full reactor green both with
  the plain build and the diagnostics-solutions profile.
  [Answer](issues/50-m10-lab-solution-spring-security-7.md)

## Fog

- Whether the M08 lab's due-date tests will want a mutable clock rather than a
  fixed one. If they do, the fixture grows a second factory; the guide
  already lists what the module holds.
