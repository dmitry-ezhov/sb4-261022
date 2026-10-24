# 64: M13 lab start: The production capstone

**What to build:** The Start module for M13: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers the whole lending application, hardened end to end: authenticated Members, transactional borrowing, observable, packaged.

**Blocked by:** 63

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Answer

`labs/lab-13-the-production-capstone-start` is the M13 lab's Start
module, added to `labs/pom.xml` after `lab-12-native-images-and-packaging-solution`
(whose lab-index row 13 in `labs/README.md` the same change added, Infra
"none"). It is the M12 lab's application carried forward: every main
source, the migrations, the `application.yml`, the `process-aot`
packaging binding and every test class is the lab-12 solution's byte for
byte, with the M12 diagnostics IT deleted and the diagnostics package
holding two new surefire classes instead.

The two diagnostics state the capstone's gap the M13 doc names (the one
promised state the twelve Modules taught and no module built, plus the
bounds the doc's point 3 asks for), each as one story against a real
boot, and each failure message names the move that is missing:

1. `ReadinessDiagnosticsTest.theOutageIsPublishedAsStateAndTheRecoveryMovesItBack`
   - a `@SpringBootTest` boot on its own H2 over a WireMock availability
     service. The arc: the probe reads `UP` before anything is asked; the
     wire is cut, the borrow answers its `502` problem detail, the
     `library.availability.retried` counter answers for the spent budget -
     and then the read the Lab exists for: readiness must answer
     `OUT_OF_SERVICE` (red 15s of `UP` patience on the Start module). From
     the publish the story keeps walking: liveness stays `UP` (a restart
     is never the repair for a broken dependency), the catalogue stays
     public, the recovery borrow lands `201` - and readiness must move
     back (red on the Start module in the same never-published way). The
     message names `AvailabilityChangeEvent.publish(context,
     ReadinessState.REFUSING_TRAFFIC)` by name, names the spent budget's
     `ResourceAccessException` as the moment, and names the published-not-
     toggled shape (the probe reads the published state; it does not
     re-ask the dependency). The metric reads (`retried` mid-outage,
     `issued` after the recovery borrow) prove the counters keep counting
     through all of it, and the unauthenticated reads prove the probe is
     the M10 public surface it must stay.
2. `BoundsDiagnosticsTest.theBoundsTheDeploymentSuppliesAreTheOnesTheProcessKeeps`
   - one self-booted `SpringApplicationBuilder` run with all three bounds
     the M13 doc's point 3 names spelled on its command line (the property
     source that outranks the jar's yml): `library.availability.max-retries=0`,
     a budget of one attempt; `spring.http.clients.connect-timeout=500ms` and
     `spring.http.clients.read-timeout=5s`, the client timeouts every built
     client inherits; and `spring.lifecycle.timeout-per-shutdown-phase=3s`.
     Every override is first read back off the booted environment (green:
     the values reached it), then each bound is kept on a wire read of its
     own. The wire blip the default geometry carries a borrow through
     proves the geometry (red on the Start module: the borrow answers
     `201`, so the compiled `@Retryable` numbers, not the environment,
     decided), and the counter read bounds what a budget of one attempt
     may publish: two `MethodRetryEvent`s - the failed execution's own and
     the interceptor's exhausted mark - against the four of the compiled
     geometry of three attempts. The read timeout is kept on its own wire
     read, where the Start module's story differs in kind: the client
     timeouts are already environment outcomes in the carried application,
     so this half answers green on the Start module and proves the third
     bound on every boot the Lab ships - an availability answer spending
     2.5s on the wire, past the jar's own two-second read-timeout default,
     lands as a `201` under the deployment's named five (where the jar's
     own default would have timed the read dead and answered the spent
     budget's `502`); the connect timeout has no wire slow enough to bite
     on a localhost boot, so its override is carried by the environment
     read alone. The drain half holds a borrow across a stop beneath it:
     the deployment-named window is the one the drain keeps, the row
     landing inside it. The message names the mechanism the Lab's move
     rides: the annotation's String variants (`maxRetriesString` and kin)
     carry `${...}` placeholders, so the geometry binds as configuration
     properties beside `library.availability.base-url` and the jar keeps
     its geometry in the placeholder's default while a deployment names
     another without a rebuild.

The diagnostics were validated green against a scratch prototype before
being pinned red (the ticket-59 pattern): a copy of the Start module with
the publish in the boundary's handler (`AvailabilityChangeEvent.publish(publisher,
this, REFUSING_TRAFFIC)` on the spent budget's `ResourceAccessException`)
and publish-back in the checker on every successful availability read,
plus `maxRetriesString = "${library.availability.max-retries:2}"` on the
checker's annotation - both diagnostics pass 2/2 there, so the Solution
ticket's shape is achievable. The scratch module was deleted; nothing of
it remains. One correction the prototype taught: the counter read on one
spent attempt is *two* events (the failed execution plus the exhausted
mark), not zero - the bounds diagnostic asserts the ceiling of two, and
its message says where the two come from. The client-timeout half's green
chain was proven on its own temporary validation boot on the Start module
- the geometry assertion cuts the main diagnostic before that half is
reached, so it cannot prove the half's green by itself - and the boot was
removed once the read (a 2.5s availability answer landing `201` under the
named five seconds) had passed.

Note the diagnostics ride surefire (`*Test`), not failsafe: unlike M12's
packaged forms, the capstone's evidences are probe states, counters and a
drain - all observable in the exploded form, and the custom bounds are
command-line overrides no packaged boot could take.

**Verification:** `./mvnw -f labs -pl lab-13-the-production-capstone-start
-am verify` - **BUILD SUCCESS**, 29 unit tests + `PackagedApplicationIT`
green (the M12 app's own tests carried forward unchanged), Spotless/PMD/
SpotBugs/Checkstyle clean, JaCoCo skipped as the Start-module contract
allows. `./mvnw -f labs -pl lab-13-the-production-capstone-start verify
-Pdiagnostics` (after `./mvnw -f labs -pl labs-support install`, see
Comments) - red exactly 2/2, failures at the geometry read (201 against a
budget of one attempt) and at the readiness read (a full 15s patience of
`UP` through the outage), each message naming the missing move.
`./mvnw -f labs verify` - the full default reactor green, 27/27 modules.
`./mvnw -f labs verify -Pdiagnostics-solutions` - green, the Start module
skipped by its own `<diagnostics.skip>`.

## Comments

- **2026-10-24 · agent** — Implemented per the M13 doc's Lab section and
  the labs authoring conventions: sources carried forward byte for byte
  from the lab-12 solution (the application the previous Module left,
  `process-aot` included), two surefire diagnostics under
  `src/test/java/.../diagnostics/`, module added to the aggregator after
  `lab-12-native-images-and-packaging-solution`, row 13 in the lab index,
  the Solution module deliberately absent (ticket 65). The green chain
  behind each diagnostic was proven on a scratch prototype of the
  solution shape before the reds were pinned.
- **2026-10-24 · agent** — The Fog bullet about the sibling checklists'
  `-pl <module> verify -Pdiagnostics` runs resolving on a fresh clone is
  now answered for this lab: without `labs-support`'s test jar in the
  local repository, the per-module command fails at dependency resolution
  ("NOT allowed from central") before any test runs; once
  `./mvnw -f labs -pl labs-support install` has run once, the documented
  command form works exactly as spelled - tested both ways on this
  module. Left to the index-doc pass (ticket 68) to say once for all
  labs.
- **2026-10-24 · agent** — Two review findings applied to the diagnostics
  after the initial Answer. The bounds diagnostic now holds the third
  bound the M13 doc's point 3 names - the client timeouts - on the same
  one-boot-one-command-line shape as the other two:
  `spring.http.clients.connect-timeout=500ms` (carried, read back off the
  booted environment; no localhost connect is slow enough to bite) and
  `spring.http.clients.read-timeout=5s` (with teeth on the wire - an
  availability answer spending 2.5s on the wire lands its `201` under the
  named five, where the jar's own two-second default would have timed the
  read dead and answered the spent budget's `502`; green on the Start
  module too, because the carried application already binds the client
  timeouts, proven by a temporary stand-alone validation boot since the
  geometry assertion cuts the main diagnostic). And the two diagnostics'
  `countOf` metric readers now answer the missing-metric case the same
  way - non-200 or no COUNT measurement reads as the zero it is - so a
  red on a counter read always says what was counted, never renders the
  guidance as "counted null". No red changed: `-Pdiagnostics` still fails
  exactly at the geometry read and the readiness publish, both messages
  unchanged in kind.
