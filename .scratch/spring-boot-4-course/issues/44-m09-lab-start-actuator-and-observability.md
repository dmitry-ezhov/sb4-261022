# 44: M09 lab start: Actuator and observability

**What to build:** The Start module for M09: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers exposing a liveness and a readiness probe, publishing a metric about loans, and making sure the management endpoints are not reachable from the public API.

**Blocked by:** 43

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Answer

`labs/lab-09-observability-start` is the library application exactly as
M08's Solution left it: every main source, the migration, the
`application.yml` and every test class are byte-for-byte the lab-08
solution's (verified with a full-tree diff excluding the two deliberate
differences), with three deliberate changes - the POM (artifactId,
description, and the two Start-module skips `<jacoco.skip>` and
`<diagnostics.skip>`), the reactor entry in `labs/pom.xml`, and the
diagnostics package, whose M08 classes are replaced by the Lab's own one.
Nothing was added to make the Lab doable: the observability upgrade is the
work itself. No application code was rewritten.

**The diagnostics** (`ObservabilityDiagnosticsTest` - infra `none`, one
class, no PostgreSQL diagnostics) state the doc's three-part upgrade, each
red message naming the piece that is missing:

- `theProbesAnswer` - the liveness and readiness probes answer
  `/actuator/health/liveness` and `/actuator/health/readiness` with `UP` on
  the management socket. On the Start module it fails where nothing is
  reachable: no actuator starter, no `local.management.port` property, and
  the message names the POM line, the `management.server.port` line, and
  the fact the Lab leans on - on 4.1.1 the probes are
  `@ConditionalOnBooleanProperty(..., matchIfMissing = true)`, so once the
  starter is in, no probe property is written anywhere.
- `aLoanIssuedIsAMetricTheRegistryHolds` - borrows a Copy the availability
  service says is on the shelf (the M08 rig: WireMock, a fresh H2 the
  diagnostics own) through the M08 boundary and demands the borrow still
  answers `201`; then reads `/actuator/metrics/library.loans.issued` off
  the management socket and demands the name read back, a `title` tag with
  values, and a `COUNT` measurement carrying the issuance. On the Start
  module every step fails with one message naming the four pieces: the
  counter published in the Loan feature's use case (not the controller, not
  a stats gatherer), the Title as the bounded tag dimension, `health,metrics`
  on the exposure list, and the management port.
- `theManagementSurfaceIsAwayFromThePublicApi` - the public API's socket
  must answer `/actuator/health` with `404` while the management socket
  answers `UP`. On the Start module the first assertion holds for the wrong
  reason (nothing is exposed at all) and the second fails, naming the
  starter and the port line; the message states the decision that belongs
  to the deployment (which socket the ingress reaches) and the one that
  belongs to the application (two sockets, not one).

**Not a MockMvc rig, on purpose:** the requirement is about sockets - the
management surface must answer on a port the public API does not serve,
which no MockMvc harness can ask. The class boots
`@SpringBootTest(webEnvironment = RANDOM_PORT)`, discovers the two ports
the way a monitor discovers them (`local.server.port`,
`local.management.port` from the `Environment`), and talks over the real
network with the JDK's HTTP client.

**Verification.** The default build is green through every gate the start
module opts out of nothing but the coverage floor
(`./mvnw -f labs -pl lab-09-observability-start -am verify`): 22 tests and
the packaged-jar IT green, Spotless, PMD, SpotBugs and Checkstyle applied
(`-am` is needed from the repo root or the labs aggregator; the
`labs-support` tests jar is a reactor artifact). The diagnostics run is red
3/3 with the messages above
(`./mvnw -f labs -pl lab-09-observability-start -am verify -Pdiagnostics`).
The whole labs reactor (19 modules) builds green, and
`./mvnw -f labs verify -Pdiagnostics-solutions` is green with the Start
module skipped through its `diagnostics.skip=true`.

**The contract's other half was proved** ticket-39 style, with a throwaway
prototype of the Lab's asks inside the module (commit first, prototype
after, prototype `git restore`d afterward and now living nowhere): the
actuator starter in the POM, `management.server.port: 0` plus
`exposure.include: "health,metrics"` in the `application.yml`, and the
counter in `LoanService.borrow` -
`registry.counter("library.loans.issued", "title", Long.toString(copy.titleId())).increment()`
- passed the diagnostics green 3/3. The prototype surfaced exactly one
defect in the diagnostics themselves (the `COUNT` measurement's value is
one JSON path deeper than the assertion assumed) and that fix is the only
code the green run added to the committed start state.

**For ticket 45 (the Solution module).** The diagnostics are fixed
statements, so the reference implementation must satisfy exactly what the
three tests assert, and three facts the prototype surfaced are worth
carrying:

1. *The tag's value is the Copy's Title, and the Copy already carries it.*
   The lab-08 `Copy` payload has `titleId`, so the use case can tag
   `title` with `copy.titleId()` at the increment site - no extra read, no
   repository trip. The diagnostics never assert the value, only that the
   tag named `title` carries at least one value and that `COUNT >= 1`.
2. *`local.management.port` is visible to a `@SpringBootTest`
   RANDOM_PORT main-context test on 4.1.1* - the management server
   publishes it into the environment the diagnostics read, even bound at
   port 0. `management.server.port: 0` in the prototype was enough for
   every diagnostic; the solution may pin a fixed port or leave 0, the
   diagnostics pin the two-socket shape, not the number.
3. *The lab's probes need no code and no property.* Once the starter is in
   the POM and the management server rides its own port, `theProbesAnswer`
   is green with nothing written beyond the two configuration lines - the
   4.1.1 claim the M09 doc verified by bytecode and boot is exactly what
   the red-then-green run stated.
