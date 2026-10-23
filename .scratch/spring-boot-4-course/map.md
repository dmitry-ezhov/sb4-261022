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

## Fog

- Whether the M08 lab's due-date tests will want a mutable clock rather than a
  fixed one. If they do, the fixture grows a second factory; the guide
  already lists what the module holds.
