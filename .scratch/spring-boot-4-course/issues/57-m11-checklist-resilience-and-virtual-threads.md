# 57: M11 checklist: Resilience and virtual threads

**What to build:** The checklist for M11: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 56

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers no blocking call runs on a carrier thread
- [x] it covers every outbound call has a timeout
- [x] it covers shutdown drains in-flight work instead of dropping it
- [x] it covers saturation is visible in a metric
- [x] no item restates a Quality gate that the build already enforces

## Answer

The checklist was drafted with the doc (ticket 53); this ticket graded it
against its criteria and rebuilt five of its items (four to their
criteria, one tightened), added one, and kept two. Every rebuilt claim
was checked against the resolved lab-11 solution and its diagnostics,
the way ticket 52 graded its checklist.

- **The two meta-criteria graded first, and where the draft fell.** The
  draft's items mostly named observable properties and every "Verified
  by" line was a read or a run - but two of the ticket's named
  properties went uncovered outright. *No blocking call runs on a
  carrier thread* had no item: the draft's first item proved the
  requests are virtual and said nothing about what their waits must
  still not do, and the doc's own JEP 491 boundary ("native frames
  still pin") never reached the checklist. *Saturation is visible in a
  metric* was the wrong item's burden: the draft's retry-observability
  item covered the counter but never named the gauge the flag emptied,
  never said which metric answers "is the upstream straining the
  library", and left the checklist without the saturation reading the
  quiz's fourth question (rebuilt in ticket 56) already gives the
  answer to.
- **The switch item rebuilt: the draft's observation named half the
  refusals and forgot that the surface itself is the change's to
  expose.** The draft said the dump "lists no idle `tomcat-handler-`
  workers" - but the diagnostics refuse two spellings, no idle
  `http-nio-...-exec-` guard (the pattern this course's own platform
  boots print) *and* no platform thread wearing the `tomcat-handler-`
  prefix (a virtual name on a platform spelling is a hand-built
  executor, the state the second refusal catches); and the draft never
  said the dump is only reachable if the change adds `threaddump` to
  the exposure line - the Start module carries `health,metrics`, and
  the red run's first failure is the `404` the unexposed read earns.
  The rebuilt item also makes the state the grep refuses explicit (no
  `Executors.newVirtual...`, no thread factory, no hand-built
  executor) and states the connector caps as what arriving work is
  still bounded by.
- **The carrier item added.** Verified by reading, not running - on
  this JDK no dump command separates all the pinned-and-parked cases
  - so the item is the audit the criterion asks for: the one shape
  still occupying a carrier is a wait bottoming out in a native frame
  (JEP 491 released the carrier at every `synchronized` wait, retiring
  the Boot 3-era avoidance reflex), the change's blocking surfaces are
  named and short (the availability wait and the transaction's JDBC
  work, both pure Java to the bottom), and the answer the read wants
  is "none, and here is each blocking surface". The JEP's claim itself
  is read off OpenJDK's documentation, the same doc-read the M11 doc
  marks.
- **The timeout item rebuilt: its groundedness was wrong in both
  directions.** The draft cited the diagnostics for the bound as
  configured - but the timeout diagnostic does not exist: the
  `spring.http.clients.*` lines are a state the Start module already
  carries, M04's property migration having been "made here before it
  was taught here" - so the rebuilt item says the lines ride along and
  the review is that they survive the change intact. And the draft's
  "the diagnostics go at an upstream that does not answer" is only
  true of the *failure the bound lets surface*: the rebuilt item keeps
  the exhaustion read (every attempt dropped by a cut wire, answered a
  `application/problem+json` `502` in the boundary's words, checked
  off the solution's `handleAvailabilityServiceUnreachable`), names
  the never-a-hang claim's real seat (the diagnostics fail on an
  unanswered socket, so a hang is a red run, not a silent wait), and
  makes "every outbound call" the literal search it must be - the diff
  searched for the provenances a bounded call base must not show
  (`RestTemplate`, `RestClient.create`, a second `RestClient.Builder`
  on the side), run against the resolved solution and answering
  nothing.
- **The retry item restated property-first, and one claim dropped.**
  The rebuilt item leads on what the diagnostics hold (one connection
  reset answered `201`, the Loan issued to the Member the wire
  carried) and makes the proxy-crossing the property the read checks -
  a `this.`-side call is a state no reading proves, so the
  diagnostic's own pass is the proof - with the two declarations
  (`@EnableResilientMethods` once, `@Retryable` on the method the
  borrow calls) and the checkable arithmetic (1 + `maxRetries` total
  attempts, growth bounded by `maxDelay`, per-attempt slack by
  `jitter`) kept after it. The draft's "the budget exhaustion carries
  its record (`getRetryCount()` / `getLastException()`) to the
  boundary" was dropped: those accessors belong to the spring-core
  `RetryException`/`RetryTemplate` surface, which the lab's change
  does not use - the bytecode-backed shape is 7.0.9's interceptor
  rethrowing the last attempt's `ResourceAccessException`, the
  boundary handler beneath it, and a checklist item explaining
  behaviour through a class the change never instantiates is a
  teaching error dressed as verification.
- **The saturation item rebuilt to the criterion.** The rebuilt item
  opens with what died - the busy-worker gauge: the executor is a
  spawner with no stable thread count, the dump lists no idle worker
  to graph - and names what the operator watches instead:
  `library.availability.retried` on the `MethodRetryEvent` listener
  (labelled by method, one listener per every resilient method),
  readable on `/actuator/metrics` beside M09's `library.loans.issued`
  - both confirmed in the resolved solution (`RetryMeter`,
  `LoanService`'s counter) - with the budget's end as the same
  condition answered to the caller, a `502` problem detail. A spike in
  asked-again is the upstream outage, seen from the Member's side:
  the saturation is visible in *that* metric.
- **The shutdown item tightened around the diagnostic's actual shape,**
  which the draft half-described: the diagnostic first asserts the
  window non-blank (the red run's shutdown failure is exactly the
  `null` read), then holds both halves - the in-flight borrow lands
  under the drain, a request after is refused - and names the
  fail-state the run itself names: a `server.shutdown=immediate`
  reintroduced, or a window the in-flight work outlives. The
  mechanism-is-the-default sentence stayed: the change adds the
  number, not the drain.
- **The vocabulary and no-built-twice items kept, their claims
  re-checked.** The glossary item was met and kept; the no-built-twice
  item's searches were run against the resolved solution and start
  module - no `spring-retry` or Resilience4j in any POM, no
  `Executors.`, `Semaphore` or `Thread.sleep` in either module's main
  sources - and kept as written.
- **No item restates a gate.** The gates speak formatting, defects and
  coverage; the items speak switches, bounds, replays, counters,
  drains and vocabulary. Every grep an item asks for searches *the
  diff* for a state a build gate never inspects - unused
  provenances, second retry mechanisms, hand-built executors - and no
  item's pass or fail is delivered by Spotless, PMD, SpotBugs,
  Checkstyle or JaCoCo.
- **Confirmed with a run:** the checklist's own verifications executed
  both ways - `./mvnw -f labs -pl lab-11-resilience-and-concurrency-start
  -am verify -Pdiagnostics` (red: 4 tests, 4 failures - the `404` on
  the unexposed dump read, the blip answered `500` retried nowhere,
  the exhaustion answered `application/json`, and the shutdown window
  read `null` - each message naming the missing behaviour), then
  `./mvnw -f labs -pl lab-11-resilience-and-concurrency-solution -am
  verify -Pdiagnostics-solutions` (green: 4 tests, 0 failures, every
  gate on the solution passing). Red on the Start module, green on the
  Solution module - the asymmetry the items describe.

## Comments

- **2026-10-24 · agent** — Graded the ticket-53 checklist against the
  acceptance criteria; the carrier-thread and saturation-as-metric
  properties were uncovered and the timeout item grounded itself on a
  property the Start module already carries; each became or reshaped
  an item, with every rebuilt claim checked off the resolved lab-11
  code and the diagnostics run red and green. Doc edit is the
  resolution; no code changed.
