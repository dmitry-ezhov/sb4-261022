# 65: M13 lab solution: The production capstone

**What to build:** The Solution module for M13: the reference implementation of the whole lending application, hardened end to end: authenticated Members, transactional borrowing, observable, packaged, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 64

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Answer

`labs/lab-13-the-production-capstone-solution` is the M13 lab's Solution
module, added to `labs/pom.xml` after
`lab-13-the-production-capstone-start` and carrying the lab index (row 13)
already added by ticket 64. It is written as its own module from the
agreements the twelve Modules established, not edited out of the Start
copy - and the capstone's two moves ride exactly the seams the
diagnostics name:

1. **The outage is state, and the recovery is state in the same words.**
   The boundary owns the moment: `ApiExceptionHandler` now takes
   `ApplicationEventPublisher` (the context itself, available in every
   context the handler rides, including the webmvc slice) and its
   `handleAvailabilityServiceUnreachable` - the handler that answers the
   spent budget's last `ResourceAccessException` with the `502` problem
   detail - publishes
   `AvailabilityChangeEvent.publish(publisher, this,
   ReadinessState.REFUSING_TRAFFIC)` before it answers. The location is
   the point: the spent budget is exactly the moment the dependency
   stopped answering, there is no unspent arrival of this type at the
   boundary, and readiness is told as an event - published, not toggled,
   never re-asked against the dependency, so the routing answer and the
   refusal answer cannot drift. The recovery publishes the same way on
   the other side of the wire: `AvailabilityChecker` takes the publisher
   too and, on every read that lands, publishes
   `ReadinessState.ACCEPTING_TRAFFIC` before returning the answer - a
   read that lands is the event the recovery publish needs, and nothing
   else about the borrow changed (the catalogue stays public, liveness
   stays `UP`, the `502`s and `201`s keep their statuses, the counters
   keep counting - the diagnostics walk all of it against this module
   and read exactly the states they demanded).
2. **The geometry is the environment's, not the annotation's constant.**
   The `@Retryable`'s numbers moved from compiled attributes to the
   String variants the annotation carries for exactly this -
   `maxRetriesString` and kin, each holding a `${...}` placeholder -
   resolved against the environment at proxy build time:
   `${library.availability.max-retries:2}`,
   `${library.availability.delay:100}`,
   `${library.availability.jitter:50}`,
   `${library.availability.multiplier:2}`,
   `${library.availability.max-delay:1000}`. The geometry's keys are
   bound as configuration properties beside
   `library.availability.base-url` in the `application.yml` - each value
   the jar's own reviewed geometry, written where a deployment reads the
   policy it inherits and overrides it on a command line that outranks
   the yml without a rebuild - and the placeholder's default keeps that
   same policy when the keys are absent, so a stripped yml boots shaped,
   never unbounded. `includes` stays the compiled `ResourceAccessException`
   reference: the one failure a replay may carry is a type, not a chain
   of property sources. The `BoundsDiagnosticsTest`'s single-boot story -
   four bounds spelled on one command line, the wire reads keeping each -
   now answers as a green chain: a budget of one attempt lets the blipped
   borrow die on its `502` with the counter-ceiling of two events, the
   deployment's five-second read timeout keeps a 2.5s answer alive into a
   `201` where the jar's own two-second default would have timed it dead,
   and the drain keeps the borrow across a stop inside the deployment's
   named `3s`.

Everything else in the module is the application the twelve Modules left,
written afresh as a new module: same compiled surface (the start module
carried it forward byte-for-byte from the lab-12 solution), same
`application.yml` except the geometry keys and the comments their
addition warrants, same migrations, same `process-aot` binding. The
carried suite touched nothing: the wiring home's and slices' claims are
about answers, and every answer this module gives matches what they
already assert - the two published states are asserted by the
diagnostics, which are the Lab's tests, not the implementation's.

## Verification

- Strict module build: `./mvnw -f labs -pl
  lab-13-the-production-capstone-solution verify` - **BUILD SUCCESS**, 29
  unit tests plus `PackagedApplicationIT` green, Spotless/PMD/SpotBugs/
  Checkstyle clean, coverage **90% line / 87% branch** as the floor
  requires. The carried suite exercises both publishes for coverage for
  free: the wiring home's
  `aSpentAvailabilityBudgetIsAnsweredAProblemDetailNotTheErrorPage` walks
  the `502` (and so the `REFUSING_TRAFFIC` publish), and every `201`
  borrow walks the checker's `ACCEPTING_TRAFFIC` publish.
- The diagnostics seam, both directions:
  `-pl lab-13-the-production-capstone-start verify -Pdiagnostics` red
  exactly 2/2, failing at the geometry read (`201` against a budget of
  one attempt) and the readiness read (a full 15s patience of `UP`
  through the outage), each message naming the missing move;
  `-pl lab-13-the-production-capstone-solution verify -Pdiagnostics`
  green 2/2 (Bounds 7.3s, Readiness 1.5s).
- The repository seams: `./mvnw -f labs verify` green, 28/28 modules (the
  Solution module listed in the labs aggregator behind the Start module);
  `./mvnw -f labs verify -Pdiagnostics-solutions` green across the
  reactor, the Start module skipped by its own `<diagnostics.skip>` and
  the Solution module's two diagnostics classes run green by the single
  command CI asserts (with `labs-support`'s test jar installed once, per
  ticket 64's Comments).
## Comments

- **2026-10-24 · agent** — Implemented per the M13 doc's Lab section: the
  publish in the boundary's handler on the spent budget's wire failure,
  the publish-back in the checker on every read that lands, and the
  geometry bound as configuration properties fed to the `@Retryable`
  through its String variants - the three moves the two diagnostics name,
  on the assembled application the twelve Modules left. Module added to
  `labs/pom.xml` after the Start module; the lab index's row 13 was
  already in place.
- **2026-10-24 · agent** — Two attribute-spelling facts worth the record,
  both read off the 7.0.9 annotation class before the first build: the
  String variants are `maxRetriesString`, `timeoutString`,
  `delayString`, `jitterString`, `multiplierString`, `maxDelayString` -
  the annotation names each long/double attribute's carrier, and
  `multiplier` is the one double (the String carrying a plain "2"); the
  plain-integer format of a delay String is milliseconds, which is why
  the yml carries `delay: 100` and `jitter: 50` bare. The `process-aot`
  walk accepted the placeholders on the first build - the AOT
  bean-definition sources carry the resilience machinery's own
  configuration, and the packaged IT boots the jar unchanged.
- **2026-10-24 · agent** — Code-review (two axes, HEAD base) findings
  applied or answered on the record. Applied: the rewritten
  `AvailabilityChecker` javadoc says *Copy* the way the glossary
  capitalises it, and the yml comment no longer implies the geometry keys
  are bound through the `AvailabilityProperties` record - the record still
  binds only `baseUrl`, the geometry resolves against the environment at
  every boot through the annotation's placeholders, and the comment now
  says exactly that. Answered, no change: (1) *the geometry numbers look
  duplicated between the yml and the placeholder defaults* - the annotation
  is forced to be constant, so the pair cannot be factored; the
  defaults are the jar's fallback for a stripped yml, and the bounds
  diagnostic already pins the override path on the wire, so a third
  assertion would restate it; (2) *the readiness ledger lives in two
  classes* (the checker's recovery publish, the advice's refusal publish) -
  that is the doc's own shape ("the recovery publishes the same way in the
  checker"), the two halves sit in the two places the two events are born,
  and the Module's move is deliberately small; (3) *a publish on every
  answered read is per-request event noise* - intended: the recovery has to
  be discovered by the one read whose landing is the event, and the
  payload is an enum, not a matter to aggregate. On the spec axis, the
  reviewer's substantive suggestion - a default-build test asserting the
  readiness move - was weighed and declined on the precedent of ticket 55:
  the Lab's behaviour is the diagnostics' subject, and a default-build
  boot of an outage would duplicate the readiness diagnostic the module
  already carries and CI runs through the single command.
- **2026-10-24 · agent** — Re-verified after the review edits: the strict
  module build green again, coverage 90/87, both diagnostics green
  (2/2), and `./mvnw -f labs verify` green across the reactor.

