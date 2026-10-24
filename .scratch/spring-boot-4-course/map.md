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

- **2026-10-24 · ticket 48** — The M10 doc is `modules/10-security7.md`:
  the delta table splits the three releases whose break the reader meets
  (6.0 removes `WebSecurityConfigurerAdapter`, 7.0 deletes the deprecated
  DSL and adds `csrf.spa()`, Boot 4.1.1 moves the auto-configuration and
  renames the resource-server starter) with every row read off the 7.1.1
  jars and one booted 4.1.1 app - the CSRF `403` before the authentication
  `401` included - not remembered. Every "how it works now" snippet
  compiles against the Solution module's classpath. Review closed two
  doc-versus-lab divergences: the chain carries no `throws Exception`
  (`build()` lost its checked exception in 7.0, and PMD's
  `SignatureDeclareThrowsException` would flag the declaration), and the
  API chain carries no `/actuator/health/**` rule because the management
  chain owns `/actuator/**`.
  [Answer](issues/48-m10-doc-spring-security-7.md)

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

- **2026-10-24 · ticket 51** — The M10 quiz graded against the ticket's
  three understandings; question 3 was rebuilt, the other four kept. The
  kept ones: the two-release break a Boot 2 upgrade meets (6.0's
  `WebSecurityConfigurerAdapter` removal, 7.0's deletions of
  `authorizeRequests()`, `and()` and the no-arg configurers, the smallest
  7.1.1 rewrite), the CSRF attribution the colleague's upgrade note gets
  wrong (Security 6.5 already had the modern defaults, 7.0 only added
  `csrf.spa()` and the narrowed cookie repository, Boot 4 changed none of
  it - the `POST` endpoints needed the token before the upgrade), the
  `{noop}` prefix as the delegating encoder's instruction rather than a
  comment (Boot's `NOOP_PASSWORD_PREFIX` prepend, the two-line production
  shape), and the token-versus-session argument (a bearer credential is
  not ambient, `STATELESS` and `csrf.disable()` stand or fall together).
  The rebuilt question reviews the solution module itself with two
  half-true simplifications - drop the `MEMBER` rule ("the expression
  already refuses a caller who is not the [Member](../GLOSSARY.md) the
  borrow acts on") or drop the `@PreAuthorize` (the comparison is true by
  construction) - and the reader must name what each loss costs: the
  rule's `403` before the use case runs, which method security cannot
  answer because it reads arguments, not authorities; and the second lock
  for the path that ever supplies a different id, whose absence today is
  invisible on the happy path. The annotation turn carries the two
  defaults: `@EnableMethodSecurity`'s `prePostEnabled = true` and the
  still-compiling `@EnableGlobalMethodSecurity`'s default of **false**,
  the quietest upgrade defect in the Module.
  [Answer](issues/51-m10-quiz-spring-security-7.md)

- **2026-10-24 · ticket 52** — The M10 checklist graded against its
  criteria and rebuilt where the criteria failed. The draft's first three
  items searched the diff for *deleted* names - `WebSecurityConfigurerAdapter`,
  `authorizeRequests()`, the no-arg configurers, the gone matcher classes -
  states compilation against the 7.1.1 jar already refuses (`javap`:
  `HttpSecurity` has only `Customizer` overloads), so they became checks the
  compiler cannot do: default-deny with the load-bearing `@Order`, the live
  `@EnableGlobalMethodSecurity` trap, and the old FQCNs in property form -
  the jar read showed
  `org.springframework.boot.autoconfigure.security.servlet.*` absent from
  4.1.1 entirely, so a class name inside an `application.yml` compiles
  against nothing. The criteria's open halves landed: unnamed paths closed
  *as observed* (health `200`, metrics refused, reservation `401`), and the
  secrets item finally says *logged* - the generated principal's password
  on one warn line (`javap` of `UserDetailsServiceAutoConfiguration`),
  stopped by an own-store `UserDetailsService` bean. Two jar-grade
  corrections: the cookie repository's four removed setters named exactly
  (httpOnly, domain, secure, maxAge; name and path survive), and the
  resource-server spellings bind only where the change adds one. The
  two-way diagnostics run confirmed the asymmetry: red 5/5 on the start,
  green 5/5 on the solution.
  [Answer](issues/52-m10-checklist-spring-security-7.md)

- **2026-10-24 · ticket 53** — The M11 doc's seven delta rows split into
  three kinds of change, each verified its own way: the *switches*
  (`spring.threads.virtual.enabled` absent from 2.7.18 and present from
  3.2, default still `false` in 4.1.1; Boot's Tomcat customizer installs
  `VirtualThreadExecutor` with the `tomcat-handler-` prefix, read off the
  bytecode; `server.shutdown` enumerated across six jars - `immediate`
  through 3.3, `graceful` from **3.4**, with 4.1's own move being
  packaging: `ServerProperties` now in `spring-boot-web-server`), the
  *renaming* (`spring.http.clients.*` plural spellings, each singular
  entry carrying `"deprecation": {"replacement": ..., "since": "4.0.0"}`
  in the http-client module's shipped metadata - and the lab's own
  application.yml already plural), and the *arrival* (no
  `org.springframework.resilience` / `org.springframework.core.retry` in
  the 6.2.19 jars; in 7.0.9 the `@Retryable` defaults are read off the
  constant pool - `maxRetries` 3 -> 1 + maxRetries attempts, `delay` 1000,
  `timeout` 0 meaning none - and the empty-`includes` semantics plus the
  advice ordering (outside `@Transactional`, inside `@Async`) are quoted
  from the published contract). Runtime observation turned into the Lab's
  claim: a flag-on boot of the lab-10 app listed 22 threads in the
  threaddump and not one `tomcat-handler-` worker - the idle guard pool
  does not exist. The five Java snippets compile verbatim against
  spring 7.0.9 + micrometer 1.17.1; the Lab section names
  `lab-11-resilience-and-concurrency-start`/`-solution` (tickets 54-55)
  and the `-Pdiagnostics` command.
  [Answer](issues/53-m11-doc-resilience-and-virtual-threads.md)

- **2026-10-24 · ticket 54** — The M11 lab start is the lab-10 solution's
  application carried forward byte for byte with the M10 chain, the Member
  principal and the probes all kept, and the resilience contract is four
  diagnostics over the real sockets: a thread dump with no idle platform
  worker (the boot's own naming calibrated - platform pool lists
  `http-nio-<port>-exec-` workers, never `tomcat-handler-`, so both
  spellings are asserted absent next to the
  `spring.threads.virtual.enabled` property read), the availability
  blip `CONNECTION_RESET_BY_PEER` then recovered replaying through to a
  `201` with a `MethodRetryEvent`-driven `library.availability.retried`
  count the Operator reads on the management socket, the cut-wire
  exhaustion answered `502` `application/problem+json` with a titled
  problem detail instead of the error page, and the drain test booting its
  own `LibraryApplication` - a `@SpringBootTest` context the test itself
  stops cannot be closed without the framework reaching for it again - to
  prove a borrow begun across the stop lands while a request after it is
  refused and `spring.lifecycle.timeout-per-shutdown-phase` is read back
  named. Two facts harvested along the way: the 7.0.9 retry interceptor
  rethrows the spent call's last failure (`ResourceAccessException`) not
  the `RetryException`, so the diagnostics assert wire observables only;
  `SpringApplicationBuilder.properties(...)` lands in `defaultProperties`
  - lowest precedence - so the drain boot passes its overrides as
  command-line args. Diagnostics red 4/4 on the start, green 4/4 against a
  validated solution, ordinary build green with every gate but the
  coverage floor.
  [Answer](issues/54-m11-lab-start-resilience-and-virtual-threads.md)

- **2026-10-24 · ticket 55** — The M11 lab solution is the lab-10 surface
  with the resilience move written in: the switch is
  `spring.threads.virtual.enabled: true` and nothing else (threaddump
  added to the management exposure so the absence of idle workers is
  observable on the guarded socket), `AvailabilityChecker` carries the
  M04 client behind the replay policy (`@EnableResilientMethods` once on
  `ResilienceConfiguration`, `@Retryable` on the method the borrow calls
  across the proxy, includes naming `ResourceAccessException`, geometry
  stated with its sentence: three attempts, 100ms then 200ms, 50ms
  jitter, maxDelay never reached), `RetryMeter` counts
  `library.availability.retried` on the `MethodRetryEvent`, the exhausted
  budget answers `502 application/problem+json` on the M08 surface, and
  the drain window is named at 10s. Strict build green (JaCoCo 89.1%
  line), diagnostics red 4/4 on the start and green 4/4 on the solution,
  both reactor seams green. Pointer backfilled by ticket 56: the entry
  had not landed with the ticket's resolution.
  [Answer](issues/55-m11-lab-solution-resilience-and-virtual-threads.md)

- **2026-10-24 · ticket 56** — The M11 quiz graded against the ticket's
  three named understandings and three questions rebuilt: the virtual
  threads' help/not-help contrast (the upgrade memo's three promises -
  the cheap held shore, the retired-but-not-wholly pinning audit,
  the connector caps that the flag never raised), "what a timeout does
  not solve" as the reader's job (the memo's "bounds every outbound
  call" sentence, fenced twice: the configurer-bypassing call base and
  the bound-vs-repair split), and observing saturation (the busy-worker
  gauge reads nothing under the flag; the saturation surfaces as
  problem-detail refusals and the `library.availability.retried`
  counter). Question 5's telegraphed "only error is its date" clause
  went; question 3 kept. Claims re-verified on the 7.0.9/4.1.1 jars and
  the lab-11 solution, not remembered.
  [Answer](issues/56-m11-quiz-resilience-and-virtual-threads.md)

- **2026-10-24 · ticket 57** — The M11 checklist graded and rebuilt from
  seven items to eight: the carrier property the draft never covered got
  its read-audit item (the one pinning shape left on JDK 25 is a wait
  bottoming out in a native frame), saturation was given the metric item
  its criterion asks for (the busy-worker gauge dead under the flag;
  `library.availability.retried` read beside M09's counter), the timeout
  item was re-grounded on the state the Start module already carries (the
  `spring.http.clients.*` lines ride along from M04 - the review is the
  survival and the provenance search, not the property), and the
  shutdown item tightened to the diagnostic's actual shape. Confirmed
  with the run both ways: red 4/4 on the start, green 4/4 on the
  solution.
  [Answer](issues/57-m11-checklist-resilience-and-virtual-threads.md)

- **2026-10-24 · ticket 58** — The M12 doc's eight delta rows split
  into three kinds of change, each verified against artifacts its own
  way: the *removals* (all six plugin descriptors read -
  `executable`/`embeddedLaunchScript` gone from the 4.1.1 repackage
  mojo with `loaderImplementation`, the latter having arrived in 3.2;
  `spring-boot-loader-classic` present at 3.5.16 and absent at
  2.7.18/4.1.1 while `org.springframework.boot.loader.launch.
  PropertiesLauncher` still ships in the 4.1.1 loader jar; `layertools`
  renamed `tools` in 3.3, the tool artifact absent at 3.2.12 and
  present from 3.3.13, with the verb set `extract`/`list-layers`/
  `help` confirmed by running `help` against the real 4.1.1 lab-11
  artifact), the *source-invisible version jump* (`native-build-tools-
  plugin.version` 0.10.6 in 3.5.16's dependencies POM -> 1.1.8 in
  4.1.1's; the GraalVM Native Image Kit floor is version 25, quoted
  from the how-to), and the *expansion* (`process-aot`/`process-test-
  aot` from 3.0; Framework 7.0.9 consolidates the 6.2.19 per-family
  hint writers into `RuntimeHintsWriter` and adds `LambdaHint` - the
  comm diff of both jars; the 3.5 "Class Data Sharing" packaging page
  becomes "AOT Cache", its 3.5 URL 302-redirecting and its 4.1 text
  reading "supports the AOT cache for Java 25 and above"; the
  buildpack default builder enumerated per release from the
  buildpack-platform bytecode: `jammy-base` -> 3.4 `jammy-java-tiny` ->
  3.5/4.1 `noble-java-tiny`). Jar-grade corrections along the way: the
  AOT hint annotation package `org.springframework.aot.hint.annotation`
  lives in **spring-core** (javap-verified; `ImportRuntimeHints` alone
  is spring-context), and `spring.context.exit` enters Framework 6.1
  (carried by `DefaultLifecycleProcessor` in 6.1.14 and 6.2.19,
  absent from 6.0.23) - so the training-run switch predates Framework
  7. The row's strictest jar read: 4.1.1's `spring-boot` jar (846
  classes) carries `spring.aot.enabled` in exactly one class,
  `AotInitializerNotFoundFailureAnalyzer`, whose constant pool names
  the switch twice. The two Java snippets compile against the 7.0.9
  classpath; the doc-tickets 61/62 own the quiz and checklist.
  [Answer](issues/58-m12-doc-native-images-and-packaging.md)

## Fog

- Whether the sibling checklists' diagnostics commands (M08, M09: `-pl
  <module> verify -Pdiagnostics` without `-am`) resolve on a fresh clone,
  where `labs-support` is not yet in the local repository; the M10
  checklist spells `-am` after ticket 52 ran it both ways, the siblings
  predate the observation.

- Whether the M08 lab's due-date tests will want a mutable clock rather than a
  fixed one. If they do, the fixture grows a second factory; the guide
  already lists what the module holds.

- ~~The lab index's row 12 still reads `lab-12-capstone-*` with the Infra
  column "Docker" - a stale entry from before the M12 tickets named the
  pair `lab-12-native-images-and-packaging-start`/`-solution`~~ - the row
  was corrected to the real pair with "GraalVM (opt.)" as ticket 59
  authored the Start module (the fog's reading that the row belongs to
  the ticket that creates the modules proved right).

- **2026-10-24 · ticket 59** — The M12 lab's Start module is the
  resilience lab's application carried forward byte for byte, its
  diagnostics replaced by one **failsafe IT** rather than a surefire
  test: an M12 diagnostic has to boot the *packaged* jar the diagnostics
  are about, and the jar only exists after `package`, so the diagnostics
  have to ride failsafe. The parent POM gained the mirror of the surefire
  diagnostics convention (`**/diagnostics/*IT.java` excluded by default,
  lifted and narrowed by both diagnostics profiles,
  `failIfNoSpecifiedTests=false` because the two halves of the lab
  package don't stream symmetrically). The three reds name one missing
  `<execution>`, the `process-aot` goal, because the four deployment
  forms the Lab walks (plain jar, tools-extracted form, AOT-processed
  jar, AOT-cached run) all differ only in what the build put around the
  same missing initializer - the scratch prototype had proven the whole
  green chain before the reds were pinned. Two jar-grade facts worth
  keeping: Boot's `extract` verb refuses a destination that already holds
  anything (its own extract log has to live elsewhere), and the training
  JVM resolves `-XX:AOTCacheOutput` against the child process's working
  directory, so diagnostic scratch paths must be absolute.
  [Answer](issues/59-m12-lab-start-native-images-and-packaging.md)


- **2026-10-24 · ticket 60** — The M12 lab's Solution module packages the
  M11 application with one AOT binding: `process-aot` on the
  spring-boot-maven-plugin in its default phase, so the generated
  bean-definition sources, bytecode and hint JSON land in the jar before
  the repackage and the boot under `spring.aot.enabled=true` plays the
  initializer back. Two facts surfaced that shape shared build config:
  Boot 4.1.1's `process-aot` compiles the generated classes straight into
  the main output directory, so generator output was charged by both
  gates - SpotBugs read its generated shapes as defects, and the JaCoCo
  branch ratio fell to 0.033 because the AOT walk never executes what it
  writes; both tools now exclude the generated class-name families
  (SpotBugs by a regex in `config/spotbugs/exclude.xml`, JaCoCo in the
  root POM's gate), which takes the module to 0.875 branch coverage.
  [Answer](issues/60-m12-lab-solution-native-images-and-packaging.md)

- **2026-10-24 · ticket 61** — The M12 quiz drafted fresh against the
  doc and graded against its three named understandings before
  landing: what AOT processing changes (the memo's three promises - the
  frozen conditions/beans/profiles, the drop-the-flag regular-mode
  fallback which is the TRUE and counterintuitive one per the
  analyzer's own sentence, and the process-aot-needs-GraalVM claim
  that is backwards; plus the untransformable registrations -
  instance suppliers and `registerSingleton` - with the hints ledger
  kept strictly separate from the definition ledger), what a native
  image costs (four build/run-ledger cost sentences, one mis-filed:
  the invented "build where it runs" premise puts the version-25
  distribution on the run fleet, when the executable ships no JVM at
  all), and when to stay on the JVM (the AOT-cache runbook whose
  extracted-form and two-condition lines fail, and the three-site form
  review whose middle site blocks itself - a build-time-frozen form
  cannot serve a constraint that wants boot-time deciding; the
  flag-free regular run is what keeps it). No delta-table version
  number or pin was made an asked-for recall item; facts were checked
  against the doc's jar/page reads and the resolved lab-12 solution.
  [Answer](issues/61-m12-quiz-native-images-and-packaging.md)
