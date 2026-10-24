# 45: M09 lab solution: Actuator and observability

**What to build:** The Solution module for M09: the reference implementation of exposing a liveness and a readiness probe, publishing a metric about loans, and making sure the management endpoints are not reachable from the public API, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 44

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Answer

The module is `labs/lab-09-observability-solution`, listed in the labs
aggregator directly after the start module (`labs/README.md`'s lab-index row
already carried both names). Its sources are authored fresh - the
application the lab starts from (M08's accounting, unchanged) plus the
observability upgrade written into it. Ticket 44's prototype surfaced three
facts about what the diagnostics accept; all three are honoured, and the
implementation closes the one gap the diagnostics cannot reach (the own
suite's metric assertion).

**The probes answer with no probe property anywhere.** The one POM addition
is `spring-boot-starter-actuator` - the same name a Boot 2 POM carried,
whose 4.1.1 contents (`spring-boot-health`, the metrics starter, the
endpoint machinery) put the health endpoint and its probe groups on the
classpath. Nothing is written for liveness and readiness:
`management.endpoint.health.probes.enabled` defaults to true on 4.1.1
(`@ConditionalOnBooleanProperty(..., matchIfMissing = true)`), so writing a
probe property would be the doc's named pull-the-own-default mistake - the
diff contains none. The application.yml carries exactly two management
settings, each commented with the never-exposed reasoning:

- `management.server.port: 0` - the management surface is a different
  socket. The public API's context serves no actuator path (`404` on
  `/actuator/health` from the API's port), and the "which socket the ingress
  reaches is the deployment's decision" line from the doc's never-exposed
  list is in the comment. Zero rather than a pinned number: every socket
  this lab's boots own is asked for from the environment, and the values
  the diagnostics read (`local.management.port`, `local.server.port`) are
  exactly the properties a monitor discovers - a deployment that must pin
  the number for its monitor overwrites one line.
- `management.endpoints.web.exposure.include: "health,metrics"` - one step
  past the starter's default (`health`), the metric read the dashboard
  makes. The comment names what `include=*` would hand out and why nothing
  else is on the list; `show-details` is untouched (the default `never` in
  every column's metadata).

**The counter is published where the Loan is issued.** `LoanService.borrow`
gains a `MeterRegistry` and one increment line, right after
`loans.save(...)` writes the row the issuance is:
`registry.counter("library.loans.issued", "title",
Long.toString(copy.titleId())).increment()`. The name is the glossary's
sentence; the tag is the Title the Loan was issued against, carried by the
M08 `Copy` payload - no extra repository trip - and bounded by the
catalogue, which the increment's own comment argues against the unbounded
alternatives (a Copy identifier or a Member id would mint a time series per
Copy and per Member). A counter is not transactional state, and the comment
says so rather than implying it rolled back with the boundary. Nothing else
in the feature changed: the borrowing use case, its `@Transactional`
boundary and the whole M08 accounting stand untouched.

**The suite asserts the lab's behaviour.** `LoanWiringTests` - the one
full-context MockMvc home, the only class that earns wiring claims - gains
one test: a borrow through the same boundary the tests above drive leaves
exactly one issuance under `library.loans.issued` with the borrowed Title's
tag (`find(...).tag("title", ...)` is the registry read; each test's Title
is fresh, so the count is literally 1.0). It asserts the observable result,
not the constructor's shape, and its Javadoc hands the socket half of the
claim to the diagnostics, which MockMvc cannot reach. The types moved for
the indicator package (`org.springframework.boot.health.contributor` for a
custom one) go unused deliberately: this lab needs no custom indicator, and
writing one to prove the doc's snippet would be code no requirement asked
for.

**Verification.** The module's own build
(`./mvnw -f labs -pl lab-09-observability-solution -am verify`) is green:
23 tests plus the packaged-jar IT, JaCoCo present (86% line across the
module, floor met), Spotless/PMD/SpotBugs/Checkstyle applied. The
diagnostics on the solution
(`./mvnw -f labs -pl lab-09-observability-solution -am verify
-Pdiagnostics`) are green 3/3; the same command on the start module is red
3/3 with its messages, so the asymmetry is intact. CI's statement,
`./mvnw -f labs verify -Pdiagnostics-solutions`, is green across all 20
modules - lab-09's solution diagnostics run and pass, the start module
skipped through `diagnostics.skip=true` - and the plain
`./mvnw -f labs verify` builds the whole reactor green. Attested twice:
ticket 44's prototype (red-then-green on the start module), and this
module's committed run.
