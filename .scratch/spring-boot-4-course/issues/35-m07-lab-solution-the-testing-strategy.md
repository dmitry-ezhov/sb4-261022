# 35: M07 lab solution: The testing strategy

**What to build:** The Solution module for M07: the reference implementation of reshaping the existing tests into the right form for each layer - a slice test for the web layer, a full context test for the wiring, a container test for the repository - and proving each choice pays off, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 34

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Answer

The module is `labs/lab-07-testing-solution`, listed in the labs aggregator
directly after the start module, at the name the labs README index and the
M07 doc's commands promise. Main sources, resources and the migration are the
application that M06 left, copied unchanged - the lab's work is the suite,
and `src/main` needed no edit. Everything test-side is written from scratch,
not patched.

**The suite's shape - one context per tier, each earning it in its
Javadoc.** Because the start module's defect was that nothing in any file
said why a test earns the context it boots, every test class opens by stating
its tier's claim:

- `loan/LoanControllerTests`, `reservation/ReservationControllerTests` and
  `book/TitleControllerTests` are `@WebMvcTest(FeatureController.class)`
  slices on `spring-boot-starter-webmvc-test`, with `@MockitoBean` (the
  Framework annotation that replaced `@MockBean`) substituting the
  collaborators the controller's constructor asks for - `LoanService` and
  `AvailabilityClient` for the loan slice, `TitleCatalogue` and
  `CopyRepository` for the catalogue's. The reservation slice has no
  collaborator and no substitute field, which is the point: it names its
  controller and boots the webmvc machinery alone. The assertions are the
  boundary's - status, media type, payload fields, the problem detail's
  words in the feature's own vocabulary - and nothing else of the
  application boots.
- `loan/LoanWiringTests` is the suite's one full-context MockMvc home (the
  first diagnostic's `hasSize(1)`), with no substitute field: the bound Loan
  limit read by the real service and real store, the availability client the
  real HTTP Service client pointed at a WireMock server through the real
  `library.availability.base-url`, and the limit's refusal reached through
  the boundary alone - an assertion the slice deliberately cannot make,
  which is why the full context is earned exactly once. The old
  `LibraryApplicationTests` (a bare `contextLoads`) is gone: its claim is
  included in this class's and it would be a second full-context boot.
- `book/TitleRepositoryTests` is the container test in the suite: a static
  `@Container` + `@ServiceConnection` field of
  `org.testcontainers.postgresql.PostgreSQLContainer` (Testcontainers 2's
  moved type, no self-typing), the class guarded by
  `@EnabledIfDockerAvailable` so a no-daemon machine is a skip, never a
  pass. Its Javadoc states the difference as the M07 checklist asks: what
  the suite asserts on H2 (the boundary at the slices, the loan seams at the
  wiring home) versus what is asserted only here - the migration shaping the
  second engine, the `JdbcClient` catalogue reads mapping its rows into the
  API's record, the derived Copy query running real SQL against it, and the
  migration's unique constraint holding there.

**Verification.** `./mvnw -f labs -pl lab-07-testing-solution verify` is
green through every gate including the coverage floor - 82% line / 83%
branch against the 60/50 floors - with the container tests actually run
(Docker present, 16 tests green). The diagnostics-only command
(`./mvnw -f labs -pl lab-07-testing-solution verify -Pdiagnostics`) is green
3/3, while the same command on the start module stays red 3/3 - the
asymmetry the lab is about. `./mvnw -f labs verify -Pdiagnostics-solutions`
asserts the diagnostics of every solution module, twice working: lab 07's
diagnostics are the shape introspectors that open no context at all, so the
root POM's `diagnostics` and `diagnostics-solutions` profiles now skip
JaCoCo - the coverage floor measured over a diagnostics-only run would be
0% on exactly the modules whose diagnostics read rather than boot; the root
parent POM comment explains why, and the ordinary `verify` still enforces
the floor on every module). `./mvnw -f labs verify` is green for the whole
reactor, 16 modules. `labs/README.md` records the diagnostics-profiles JaCoCo
nuance beside the commands learners copy it from.
