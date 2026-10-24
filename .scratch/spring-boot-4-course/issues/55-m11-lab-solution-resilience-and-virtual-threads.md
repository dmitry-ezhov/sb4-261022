# 55: M11 lab solution: Resilience and virtual threads

**What to build:** The Solution module for M11: the reference implementation of running the application on virtual threads, adding a timeout and a retry with backoff to an outbound call, and making shutdown drain in-flight work, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 54

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Comments

- **2026-10-24 · agent** — Resolved. The Solution module
  `lab-11-resilience-and-concurrency-solution` is in the reactor, and the
  M11 move is written as the doc teaches it, on the M10 surface the
  Start module carried forward: a switch that is a property and nothing
  else (`spring.threads.virtual.enabled: true` in the application.yml,
  threaddump added to the management exposure list so the switch's absence
  of idle workers is observable on the M10-guarded socket), an
  `AvailabilityChecker` carrying the M04 client behind the replay policy -
  `@EnableResilientMethods` once on `ResilienceConfiguration`,
  `@Retryable` on the method the borrow calls across the proxy, includes
  naming `ResourceAccessException`, geometry stated with its sentence
  (three attempts total, 100ms then 200ms, 50ms jitter so
  synchronised replays do not amplify their own burst, maxDelay 1s never
  reached) - a `RetryMeter` listening on `MethodRetryEvent` and counting
  `library.availability.retried` labelled by the annotated method's name,
  one listener serving every resilient method in the application; the
  exhausted budget answered `502 application/problem+json` on the M08
  surface, titled "Availability service is not answering", never the
  container's error page and never a hang; and the drain window named -
  `spring.lifecycle.timeout-per-shutdown-phase: 10s`, what one bounded
  availability read plus one transaction can actually promise.
  `LoanController` holds the checker the container built, so the ask
  crosses the proxy; the slice tests and the wiring home substitute the
  checker rather than the client.
- **2026-10-24 · agent** — Verification. The strict module build
  (`./mvnw -f labs -pl lab-11-resilience-and-concurrency-solution verify`)
  is green: 29 own tests plus the packaged-jar IT, every gate 0, JaCoCo at
  89.1% line / 87.5% branch (floor 60/50), read straight off the module's
  report. The asymmetry, both directions, the way the diagnostics seam
  stands: `-Pdiagnostics` red 4/4 on the Start
  module with each failure naming what is missing, green 4/4 on the
  Solution. The two repository seams: `./mvnw -f labs
  verify -Pdiagnostics-solutions` green across the reactor - the single
  command CI asserts (`-Pdiagnostics-solutions` runs this module's two
  diagnostics classes because a Solution module sets no
  `diagnostics.skip` - the property the Start module sets true) - and
  plain `./mvnw -f labs verify` green.
- **2026-10-24 · agent** — Review (code-review, two axes, HEAD base): no
  hard violations; three notes worth carrying on the record, none
  build-blocking. (1) The `@ConcurrencyLimit` bulkhead is the Module's
  teaching that the Lab's three moves do not carry - the Lab section
  (virtual threads, timeout + retry, drain) is the Solution's scope, and
  the shape the doc says the Reservation endpoints inherit is the shape
  this module built; recorded here so its absence is not read as an undo
  or an oversight. (2) The budget's `getRetryCount()` does not reach the
  502 body: 7.0.9's annotation-driven interceptor rethrows the spent call's
  last `ResourceAccessException` (Start ticket 54's bytecode read), so what
  the boundary gets is the last failure only; the attempt count is
  observable through `library.availability.retried`, exactly the counter's
  job - the `getRetryCount()`/`getLastException()` pair travels whole only
  on the programmatic `RetryTemplate` path the doc teaches beside it. (3)
  The advice's two single-use validation helpers were inlined to keep the
  translation table within PMD's method ceiling (`TooManyMethods`, 11 > 10)
  - behaviour unchanged, both slice tests' assertions on the `errors`
  property read the same shape and stay green; a shared ruleset exclusion
  was not spent on a course's own taught pattern (one handler per refusal).

