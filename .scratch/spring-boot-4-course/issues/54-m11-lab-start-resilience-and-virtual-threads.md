# 54: M11 lab start: Resilience and virtual threads

**What to build:** The Start module for M11: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers running the application on virtual threads, adding a timeout and a retry with backoff to an outbound call, and making shutdown drain in-flight work.

**Blocked by:** 53

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Answer

`labs/lab-11-resilience-and-concurrency-start` is the M11 lab's Start
module, added to `labs/pom.xml` after `lab-10-security7-solution`. It is
the security lab's application carried forward: every main source, the
migrations, the `application.yml` and every test class is the lab-10
solution's byte for byte (a full-tree `diff -r` against
`lab-10-security7-solution/src` outside `diagnostics/` reports zero
differences), with three deliberate differences - the POM (artifactId,
description, and the two Start-module skips, `<jacoco.skip>` so the
coverage floor does not apply and `<diagnostics.skip>` so the Start
module's own build never runs the diagnostics), and the diagnostics
package, whose two classes are new.

`ResilienceDiagnosticsTest` states the Lab's first two moves as three
diagnostics over the real sockets (`RANDOM_PORT`, fresh H2 per run, the
availability service behind a real WireMock on its own port, the
management socket read the way M09/M10's diagnostics read it, no MockMvc
anywhere because the thread list and the problem detail are wire facts):

1. `theRequestsHoldNoPlatformWorkerBecauseTheSwitchIsOn` - the Operator
   reads `/actuator/threaddump` on the management socket after the
   application has served a real request, and the dump answers `200`
   while carrying no idle platform worker of either shape. The naming was
   calibrated against this stack's own boots, not guessed: on a platform
   pool, 4.1.1's Tomcat lists its guard workers as
   `http-nio-<port>-exec-N` (API socket *and* management socket
   alike, `http-nio-auto-N-exec-N`) and lists no `tomcat-handler-` name
   at all, because Tomcat's `VirtualThreadExecutor` - which owns that
   prefix when the switch is on - runs its workers as virtual threads,
   which the dump endpoint never lists. The test therefore asserts both
   spellings absent (`http-nio-...-exec-` and platform `tomcat-handler-`)
   and reads `spring.threads.virtual.enabled` back off the boot's
   `Environment` as `"true"`.
2. `aWireBlipIsRetriedThroughAndCounted` - the Copy shelved, the
   availability service answering `CONNECTION_RESET_BY_PEER` on the first
   ask in a WireMock scenario and recovered the next moment, and the
   borrow landing `201` in the authenticated Member's name: the retry
   carried it. Alongside, the Operator reads
   `/actuator/metrics/library.availability.retried` and its `COUNT`
   measurement is at least 1 - the `MethodRetryEvent` (a
   `MethodFailureEvent`, hence an `ApplicationEvent`) was counted where
   M09's listening shape can see it.
3. `exhaustedReplayAnswersAProblemDetailNotTheErrorPage` - the wire cut
   on every ask, the budget spent, and the borrow answered as a problem
   detail on the M08 surface: `Content-Type` starting
   `application/problem+json`, status `502`, a non-blank `title`. The
   status is deliberately pinned because the M11 doc itself says "the
   exact status is the diagnostics' line item, not the annotation's" -
   and pinned decisions a solution cannot dodge.

`ShutdownDiagnosticsTest` states the third move as one drain diagnostic
that boots and stops its own `LibraryApplication`
(`SpringApplicationBuilder`, servlet web type, random API and management
ports, fresh H2, WireMock answering the availability ask after a 1500 ms
fixed delay): the first assertion reads
`spring.lifecycle.timeout-per-shutdown-phase` back off the booted
`Environment` and refuses `null`/blank; the borrow is then issued
asynchronously, the test waits until WireMock has actually received the
availability request, refuses silently passing if the borrow already
completed inside the 10 s window, and only then stops the application -
the borrow answers `201` in `ada`'s name (the drain carried a request
begun across the stop to its Loan row), and a request sent after the
stop is refused with a `ConnectException` (the server stopped accepting
before it let the last request go). The context must be booted by the
test: a `@SpringBootTest`-cached context `close()`d under the test left
the framework reaching for it again afterwards ("LifecycleProcessor not
initialized"), so the boot lives and dies in the one test that needs the
stop. Two boot facts the rewrite harvested:
`SpringApplicationBuilder.properties(...)` enters the environment as
*default* properties - lowest precedence, under the application's own
yml - so the drain boot passes every override as a command-line argument
instead; and H2 in-memory sharing keys on the exact URL string the boot
itself carries, so the Copy rows are written through the URL read back
off the booted `Environment`.

One framework behaviour worth a permanent note for the Solution's
author, read off the 7.0.9 bytecode: the retry interceptor
(`AbstractRetryInterceptor.invoke`) publishes the retry-aborted event on
a spent budget and then **rethrows the cause** - the last underlying
failure (`ResourceAccessException`) - not the `RetryException` itself. A
solution's `@ExceptionHandler` keyed on `RetryException` alone would
never see the exhausted borrow. The diagnostics were written to assert
wire observables only (media type, status, title - all things a Refunknown
Implementation can answer regardless of which type reaches the advice),
and the referenced solution shape was validated green against them in a
scratch run before landing.

**Verification:** landed in one commit on `master` (`feat(ticket 54)`).
`./mvnw -f labs
-pl lab-11-resilience-and-concurrency-start verify` - **BUILD
SUCCESS**, 27 unit tests + `PackagedApplicationIT` green, Spotless/PMD/
SpotBugs/Checkstyle clean (only the coverage floor skipped, as the Start
module contract allows). `./mvnw -f labs
-pl lab-11-resilience-and-concurrency-start verify -Pdiagnostics` - red
exactly 4/4 (`ResilienceDiagnosticsTest` 3 failures,
`ShutdownDiagnosticsTest` 1), each message naming the missing behaviour:
the `404` threaddump two-property fix, the platform worker still listed,
the blip arriving as `500` because the replay lives nowhere, and the
window `null`. The same 4 diagnostics ran **green** against a scratch
validation copy of the solution (checker around the client with the
retry crossing the proxy, a `MethodRetryEvent` counter, an advice
translating the spent call's `ResourceAccessException` to the titled
`502`, and the two-line yml additions) before the ticket closed.

## Comments

- **2026-10-24 · agent** — Implemented per the M11 doc's Lab section and
  the labs authoring conventions: sources carried forward byte for byte
  from the lab-10 solution (verified with a full-tree diff), two new
  diagnostics classes under `src/test/java/.../diagnostics/`, module
  added to the aggregator after `lab-10-security7-solution`, the
  Solution module deliberately absent (ticket 55). Reactor runs: default
  build green on the module; `-Pdiagnostics` red 4/4 with each failure
  naming what is missing; the diagnostics proven achievable green in a
  scratch solution run.
- **2026-10-24 · agent** — Review fixes (code-review, two axes, HEAD
  base): the Standards axis found the drain test's 10 s wait returning
  silently when the borrow never reaches the availability stub - it now
  fails loudly, in the house style, naming the wire that was spent
  elsewhere (the change re-validated both ways after the edit: default
  build still green, diagnostics still red 4/4, scratch solution still
  green 4/4). The Spec axis raised two limits worth carrying on the
  record: the `@Retryable` geometry (`includes=ResourceAccessException`,
  `maxRetries`/`delay`/`jitter`/`multiplier`/`maxDelay`) is stated in
  the failure messages but cannot be observed by wire diagnostics - a
  solution leaving defaults quietly would still pass the blip; and the
  `timeout-per-shutdown-phase` window is only asserted non-blank - the
  doc's "what the in-flight work can promise" is a judgement the change
  can't grade. Both are inherent to black-box diagnostics and are
  catalogued in the messages the Solution's author reads.
