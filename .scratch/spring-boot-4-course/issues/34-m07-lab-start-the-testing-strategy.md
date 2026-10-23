# 34: M07 lab start: The testing strategy

**What to build:** The Start module for M07: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers reshaping the existing tests into the right form for each layer - a slice test for the web layer, a full context test for the wiring, a container test for the repository - and proving each choice pays off.

**Blocked by:** 33

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Answer

The module is `labs/lab-07-testing-start`, at the artifact name the labs README
index and the M07 doc's pointer already promise (`lab-07-testing-start` -
the ticket slug's "the-testing-strategy" loses the "strategy" to keep one name
across README, doc and POM, the same way lab-08 carries
`lab-08-transactions-start`). It is listed in the labs aggregator directly
after lab 06.

**The application.** Main sources, resources and the migration are the
`lab-06-validation-and-errors-solution` state, copied unchanged: the
application is right - the loan feature owns its words, the Loan limit is
read, the availability client is wired - and nothing in `src/main` needed an
edit. What the module carries that is the defect is the suite: M06's two
controller test classes and the reservation one, each still
`@SpringBootTest` + `@AutoConfigureMockMvc`, plus the bare context-loads
class, exactly the one-context shape the M07 doc describes. Three Testcontainers
dependencies are `test`-scoped into the POM (`testcontainers-junit-jupiter`,
`testcontainers-postgresql`, `spring-boot-testcontainers`) - the container
test the Lab asks into the suite is the learner's work, but its annotations
compile from the start, and Boot 4 manages the 2.0.5 versions.

**The diagnostics are suite introspectors.** The lab's subject is the shape of
the suite, and a shaped test class holds its shape in exactly the place the
build reads it from: the annotations on the compiled test classes under
`target/test-classes`. The diagnostics therefore open no Spring context at
all. A package-private reader in the diagnostics package (`SuiteShape`)
reflects over the compiled test classes - never initialising one, excluding
the `diagnostics` package and inner classes, keeping only classes that carry
an `@Test` method - and answers four shape questions:

1. which classes are `@WebMvcTest` slices naming a feature controller (the
   annotation's `value`/`controllers` attributes carry `TitleController`,
   `LoanController` or `ReservationController`), and whether such a slice
   substitutes a collaborator through `@MockitoBean` (or
   `@MockitoSpyBean`);
2. which classes are full-context MockMvc homes (`@SpringBootTest` +
   `@AutoConfigureMockMvc`) without a container of their own;
3. which classes hold a container test in the suite: `@SpringBootTest`,
   `@EnabledIfDockerAvailable`, and a static field annotated `@Container` +
   `@ServiceConnection` whose type is the canonical
   `org.testcontainers.postgresql.PostgreSQLContainer`.

This is not an assertion about how a test's code is arranged - it reads the
annotations the framework and the profile both act on, and it deliberately
leaves what a test class asserts inside itself to the class (the M07
checklist judges that).

**Three diagnostics, one per layer** (`TestingStrategyDiagnosticsTest`), each
a plain JUnit test and each failure naming what is missing in the doc's
vocabulary:

- `theWebLayerIsAssertedAtASlice` - asserts at least one `@WebMvcTest` slice
  naming a feature controller with `@MockitoBean` substitutes; its message
  names the starter, the annotation's moved package and the removed
  `@MockBean`.
- `theWiringHasOneFullContextHome` - asserts exactly one full-context MockMvc
  home; its message names the seam the home's subject is (the M03-bound Loan
  limit read by the real `LoanService`, the real availability client, the
  real repositories) and the "slice wearing the full context's annotations"
  trap. On the start module it fails with the three class names it found.
- `theRepositoryIsTestedAgainstTheEngineItShips` - asserts at least one
  container test in the suite; its message names `@Container`, the moved
  `PostgreSQLContainer` type, `@ServiceConnection`'s zero-configuration
  wiring, and that `@EnabledIfDockerAvailable` makes a no-daemon machine a
  skip, never a pass.

**Verification.** The module's own build is green through every gate except
the coverage floor, which `jacoco.skip` opts out of as start modules do; the
whole labs reactor builds green (15/15), and `-Pdiagnostics-solutions` skips
the module through its `diagnostics.skip=true`. The diagnostics run (`./mvnw
-f labs -pl lab-07-testing-start verify -Pdiagnostics`) is red 3/3 with the
messages above. The contract's other half was probed with a throwaway suite
(slice test naming a controller + `@MockitoBean`, one full-context MockMvc
home, a container-test stub under the three annotations): the same three
diagnostics went green 3/3, proving the introspectors detect a correct
reshape and not merely any test class; the probe classes were deleted and
the start state was restored and re-verified green in the default build and
red in the diagnostics run.

**For ticket 35 (the Solution module).** The diagnostics are fixed statements,
so the reference implementation must satisfy exactly: at least one
`@WebMvcTest` slice naming a feature controller and carrying a `@MockitoBean`
substitute field; exactly one `@SpringBootTest` + `@AutoConfigureMockMvc`
home that carries no container (the container test is excluded from that
count precisely so the two tiers can coexist); at least one `@SpringBootTest`
+ `@EnabledIfDockerAvailable` class with a static `@Container` +
`@ServiceConnection` field typed `org.testcontainers.postgresql.PostgreSQLContainer`.
One caveat the solution author should keep: the introspectors read compiled
classes, so stale `target/test-classes` would be read stale - the
diagnostics-profile command always runs `test-compile` first, so running the
diagnostics through `verify` (as every message and the README say) never
sees a stale suite.
