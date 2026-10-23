# M07 - The testing strategy: slices, containers and what survives refactoring

M06 made the library answer its own failures: the `400` for a bad request, the
`409` when the [Loan limit](../GLOSSARY.md) says no. Every one of those answers
was proven by a test - and those tests are this Module's subject. By the end of
it you can look at any test in the suite and say two honest things about it:
what it asserts and what that costs, and what happens to it when the
implementation is rewritten beneath it. The Lab is built around both, because
a suite that proves the right thing in the wrong context is as expensive as a
suite that proves nothing at all.

## What you knew

In Boot 2, one starter did all of it: `spring-boot-starter-test` brought JUnit
Jupiter, AssertJ, Hamcrest, Mockito, `spring-test` and the annotations the
framework grew around them, and almost no test needed a second dependency. The
shape you knew well: a controller test was `@SpringBootTest` with
`@AutoConfigureMockMvc` and a field of `MockMvc`; a bean the controller needed
was stubbed with `@MockBean`; the narrower context - `@WebMvcTest` for the web
layer, `@DataJpaTest` for the repository - came with the same starter, so
choosing between a slice and the full context was a choice of annotations, never
a choice of dependencies. The database under you in tests was H2 in memory,
because that is what the starter brought and what every example used. If you
needed a real database server, Testcontainers was a library you managed
yourself - imported the BOM, added `org.testcontainers:postgresql` and
`@Testcontainers`/`@Container` on the class - Boot's dependency management
never mentioned it (2.7.18's published `spring-boot-dependencies` POM manages
a JUnit 4 version for the vintage side and no Testcontainers at all).

You are assumed to know all of that fluently. You are **not** assumed to know
that Boot 4's test starter stopped shipping the slice annotations and which
artifacts they moved to, that `@MockBean` was removed rather than deprecated,
where the annotation that replaced it lives - in Framework, not in Boot - that
JUnit's major is 6 now though the programming model you know is unchanged, or
what Testcontainers 2 renamed, re-homed and kept.

## What changed

The Boot 4 column is the one this course's build resolves - Spring Boot 4.1.1
over Spring Framework 7.0.9, every version below read off the published
starter POMs of 2.7.18, 3.5.16 and 4.1.1 and off the jars the build resolves
(see the verified-versions table in the [lab index](../labs/README.md)). The
Boot 2 column is the last Boot 2 (2.7), the Boot 3 column the last Boot 3
(3.5).

|  | Spring Boot 2 (2.7.18) | Spring Boot 3 (3.5.16) | Spring Boot 4 (4.1.1) |
|---|---|---|---|
| The test starter | `spring-boot-starter-test`: `spring-boot-test`, `spring-boot-test-autoconfigure`, JUnit Jupiter **5.8.2**, Mockito **4.5.1**, `spring-test` **5.3.31** | the same starter and shape: JUnit Jupiter **5.12.2**, Mockito **5.17.0**, `spring-test` **6.2.19** | the same name, still with `spring-boot-test-autoconfigure` inside it: JUnit Jupiter **6.0.3**, Mockito **5.23.0**, `spring-test` **7.0.9** |
| Where the slices live | `spring-boot-test-autoconfigure` - `@WebMvcTest`, `@DataJpaTest` and friends came through the starter | the same | out of it: one artifact per technology, and the per-slice starters. `test-autoconfigure` itself is reduced to `@JsonTest` and the jdbc-initialisation periphery - we listed the jar |
| The webmvc slice | `@WebMvcTest` at `org.springframework.boot.test.autoconfigure.web.servlet`, inside `spring-boot-test-autoconfigure` and the starter-test we just listed | the same | its own starter, `spring-boot-starter-webmvc-test`; the lab pair of M06 already carries it. The annotation lives in the artifact's own package, `org.springframework.boot.webmvc.test.autoconfigure` |
| The mock-bean annotation | `@MockBean` and `@SpyBean` at `org.springframework.boot.test.mock.mockito` | `@Deprecated(since="3.4.0", forRemoval=true)` - we read the class file | removed from Boot's jars. The replacements are Framework's: `@MockitoBean` and `@MockitoSpyBean` at `org.springframework.test.context.bean.override.mockito`, in `spring-test` |
| Testcontainers | not managed by Boot - your own BOM and `org.testcontainers:postgresql`; the annotations at `org.testcontainers.junit.jupiter`, the container types at `org.testcontainers.containers` | managed, **1.21.4**; `@ServiceConnection` (Boot 3.1+) grows into `spring-boot-testcontainers` | managed, **2.0.5**. The artifacts are renamed - `testcontainers-postgresql`, `testcontainers-junit-jupiter` - and the annotation package did **not** move; `PostgreSQLContainer` moved to `org.testcontainers.postgresql` (the old package survives as a deprecated bridge) and lost its self-typing |

Three rows are worth a second look. **The mock-bean row** is the one that is
relieved rather than caused by this table: the replacement, `@MockitoBean`,
already existed in the `spring-test` of Boot 3.4 and 3.5 - we verified
6.2.19's jar, the one 3.5.16's starter resolves, carries it - so a
disciplined Boot 3 codebase could do this migration *before* the Boot 4
upgrade, and the upgrade itself broke nothing.
**The slice row** is the one that breaks a POM at first `mvn test`: the
annotations did not stop working, they stopped being in the starter, which is
the same story M01 told about the webmvc slice and M05 told about the data
slice. And **the Testcontainers row** divides cleanly into a POM change
(renamed artifacts, which a `mvn dependency:analyze` or a failed build names
directly) and a code change (the moved container type, caught by the
compiler in the import line).

Notice what the table has no row for: *which test deserves to exist*. Boot
brings the contexts and the annotations, and it has no opinion about how many
of each you should have, what each should assert, or what a review should ask
of them. That is the strategy part of this Module's title, and it is the part
no migration tool can do for you. The Lab is built around tests that prove
the right behaviour in the wrong context - correct assertions, expensive
boot, and nothing in the build that notices.

## How it works now

### The starter kept its name; the slices left it

The test starter is still `spring-boot-starter-test`, and reading its 4.1.1
POM shows how little the core changed: the AssertJ, Hamcrest, JsonPath,
jsonassert and xmlunit list you knew, Mockito 5.23.0, and - the one quiet
major - `org.junit.jupiter:junit-jupiter` **6.0.3**. JUnit 6 is still Jupiter:
the package, `org.junit.jupiter.api`, and the programming model of `@Test` and
the lifecycles are the ones you know, so a Boot 2 test's JUnit code needs no
edit: the version number changed, the imports did not. What did not come
along: the technology slices. One at a time they left
`spring-boot-test-autoconfigure` for artifacts named after the technology:

- the webmvc slice is `spring-boot-webmvc-test`, presented by the starter
  `spring-boot-starter-webmvc-test` - whose 4.1.1 POM is the new shape in one
  list: `spring-boot-starter` for the core, `spring-boot-starter-test`,
  `spring-boot-starter-webmvc`, the `spring-boot-webmvc-test` module, the
  jackson test starters, and `spring-boot-resttestclient` - the artifact that
  now carries the client-side test fixtures: `TestRestTemplate` moved package
  into it (`org.springframework.boot.resttestclient`), and
  `@AutoConfigureRestTestClient` sits beside it;
- the data slice is `spring-boot-data-jpa-test` (M05's row) - a starter
  `spring-boot-starter-data-jpa-test` exists beside it for 4.1.1, but the
  labs carry the module directly.

So a web layer test declares one test starter, and it is scoped
to the test classpath as ever:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webmvc-test</artifactId>
  <scope>test</scope>
</dependency>
```

and the annotation itself moved package - the import is the tell that you are
on Boot 4's shape:

```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(LoanController.class)
class LoanControllerSliceTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LoanService loanService;

    @Test
    void aWellFormedBorrowIsAccepted() throws Exception {
        when(loanService.borrow("m-001", "c-001")).thenReturn(new LoanService.Loan("m-001", "c-001", "2026-01-19"));

        mockMvc.perform(post("/api/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"m-001\",\"copyId\":\"c-001\"}"))
            .andExpect(status().isCreated());
    }
}
```

That test compiles against 4.1.1 as written, and boots the webmvc slice only:
the controller, its parameter validation, the Jackson 3 serialization M04
taught, and the stub above. The domain service is absent on purpose:
`@MockitoBean` puts a Mockito mock in its place, and nothing else of the
application is started.

### Which context a test deserves

The delta table moved where the annotations live; this is the part it cannot
move for you, because it is not a migration question but a design one. A test
boots a context, and the context is the test's cost. The two contexts this
Module's Lab wants set against each other:

- **A slice test** boots exactly the tier it names. A
  `@WebMvcTest(LoanController.class)` boots the controller and the web
  machinery, and stubs everything the controller's constructor asks for. What
  it proves: the request's route, the parameter validation, the status, the
  media type, the payload's shape - the boundary. What it cannot catch: a
  wiring miss between features, a mis-key in configuration, a query that
  would not run against the real engine.
- **A full-context test** - `@SpringBootTest` - boots the application's own
  wiring as `main` would. The service is the real one, the configuration
  binding is the real one, the HTTP Service client M04 wired is the real one.
  What it proves is the seam level: that the configuration the application
  starts with and the features wired together do agree. What it costs is
  everything above. We measured the same controller in the M06 lab's
  application - the real classes, the real context - booting the full
  context in **2.7 s** and the webmvc slice in **1.4 s** from cold starts on
  the same machine, and the full context's figure is the honest minimum: the
  M06 lab's full-context test also brings up the WireMock fixture for the
  availability client, which the slice never saw.

There is a shape that fits the library's suite: an application's controllers
asserted at the slice; the seams - configuration binding, availability client,
persistence wiring - asserted in one or two full-context tests rather than in
every test class; and the repository asserted against the engine you ship.
The repository half is the next section, and the Lab is the whole thing
practised.

For the full context, note which import changed when the slices left the
starter: `@AutoConfigureMockMvc` now comes from the webmvc test artifact, not
from `spring-boot-test-autoconfigure`:

```java
@SpringBootTest
@AutoConfigureMockMvc
class LoanWiringTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void theLoanFeatureAnswersThroughTheRealService() throws Exception {
        // No stub field here: the service is the real LoanService, the limit
        // is the bound configuration, and the assertion observes what they
        // answer together through the boundary.
    }
}
```

The empty body makes the point the section is about: a wiring test whose
subject is the wiring has no field of substitutes - it asserts that the real
beans were wired and answered, so it leaves the context with everything in
it. The slice above stubbed `LoanService` because its subject was the
controller; the same `@MockitoBean` annotation in this class would be the
defect: a context booted at full cost, with the assertions it can honestly
make deleted. Which context is worth its cost is measured in the Lab, and
the diagnostics name the difference.

### The annotation that replaced the removed mock-bean annotation

`@MockBean` and `@SpyBean` were Boot's annotations and Boot removed them -
we listed `spring-boot-test` 4.1.1's jar and the `org.springframework.boot.test.mock.mockito`
package is not in it. What is left is the Framework's pair, and they are
better annotations, not poorer ones:

- `@MockitoBean` replaces `@MockBean`: one Mockito mock, registered in the
  test context in place of the real bean;
- `@MockitoSpyBean` replaces `@SpyBean`: the real bean, wrapped.

The package is Framework's, `org.springframework.test.context.bean.override.mockito`,
and the artifact is the `spring-test` that has been under the starter since
Boot 2 - no dependency was added by this change, only the owner. This is the
row to be exact about with colleagues: nothing about Mockito changed - it is
still the same Mockito 5.23.0 the course's starter manages - what changed is
the fixture *between* the test and the framework: Boot's annotation put
Mockito mocks into Boot's context machinery, and Framework 6.2 replaced that
with its own override machinery the same Mockito mocks register into. Boot 3.4
deprecated the old names with
`@Deprecated(since="3.4.0", forRemoval=true)` and a `@MockitoBean` next to
them, so on a Boot 3.4+ codebase the migration to the new package can be
done, compiled, and shipped entirely as a Boot 3 change - the only part this
course's Boot 4 upgrade actually does is the removal, which the migration
silenced.

```java
@WebMvcTest(LoanController.class)
class LoanSubstitutionTests {

    @MockitoBean
    AvailabilityClient availabilityClient;

    @MockitoSpyBean // the real LoanService, wrapped: its behaviour intact, its calls observable
    LoanService loanService;
}
```

### The engine the test deserves: Testcontainers 2

The last row of the table, and the one the repository part of the strategy
stands on. Testcontainers 2.0.5 is what Boot 4.1.1 manages (the
verified-versions table in the [lab index](../labs/README.md)), and it is
Testcontainers with a few deliberate moves:

- the **artifacts are renamed** so the coordinates say what they carry:
  `org.testcontainers:postgresql` 1.x is now
  `org.testcontainers:testcontainers-postgresql` 2.0.5, and `junit-jupiter`
  is `testcontainers-junit-jupiter`, `jdbc` is `testcontainers-jdbc`. We
  verified the rename on the published artifact matrix: the old coordinates
  publish versions only up to 1.x, the new ones only from 2.0.
- the **annotations did not move**: `@Testcontainers`, `@Container` are still
  `org.testcontainers.junit.jupiter` (we listed the 2.0.5 jar), so a 1.x
  test's imports compile on 2.0.5 unchanged. It is the artifact they sit in
  that was renamed to `testcontainers-junit-jupiter`, and that artifact also
  brings `@EnabledIfDockerAvailable`, the guard the course's labs use for a
  machine without a Docker daemon - a skip is a skip, never a green tick.
- the **container types re-homed and lost the self-typing**: `PostgreSQLContainer`
  moved from `org.testcontainers.containers` to `org.testcontainers.postgresql`
  - we read both class files: the old one survives as a deprecated bridge -
  and, the small API delta the compiler reports, 1.x's
  `PostgreSQLContainer<SELF>` became a plain class in 2.0.5:
  `new PostgreSQLContainer("postgres:17.11")`, with no diamond.

`@ServiceConnection` takes care of the part you used to do by hand. A
WireMock stub like the one M06's full-context test wires still needs
`@DynamicPropertySource` writing `library.availability.base-url` into the
registry - a URL is an application property, not a database connection. For
the database the job is Boot's: `@ServiceConnection`, in
`spring-boot-testcontainers` since Boot 3.1, reads the container's mapped
port and credentials and configures the datasource, so the same shape the
M05 diagnostics ran is one annotation deep:

```java
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfDockerAvailable
class LoanRepositoryOnPostgresqlTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.11");

    @Test
    void theBorrowSurvivesARestart() {
        // the assertion observes the Loan the container's engine still holds
    }
}
```

That test compiles against 4.1.1 as written - the imports are the ones this
Module verified in the 2.0.5 jars - and runs the repository against the same
engine the persistence layer offers as an option in M05, M07 and M08:
PostgreSQL. The H2 of the default rig is the part you take everywhere; the
container is the part that shows what H2 would forgive.

### What makes a test survive refactoring

Everything above is machinery; this part is the discipline the machinery
serves. A test survives refactoring when what it asserts is what a user of
the application can observe - a status code, a media type, a payload's
shape, the words of a problem detail - and not how the code behind the
boundary is arranged. Reorganise the classes below the boundary - rename a
service method, split a feature, replace a derived query with a `JdbcClient`
query - and such a test stays green, because the observable contract did not
move. It goes red only when the behaviour changed, which is the red worth
seeing.

The tests that do not survive assert the arrangement itself: an argument
pinned word-for-word on a stub, a `verify()` that two service calls happened
in a sequence the client cannot distinguish, a package-local helper poked to
set up state a request could have set. The moment a name is renamed, that
test breaks - and its red signals nothing about behaviour, only that the
fixture needs retouching. Which of the two a test is, is stated in its
assertions, not its annotation: a "green" is only as real as the claim rated
above it, and an assertion of the arrangement half is not a claim about the
library at all.

Two properties carry the surviving half, and the M04-M06 suites already show
them:

- **the assertion names what a client can see** - the observable list
  above - so it stands upstream of every file the refactor might touch;
- **the names it uses are the [glossary's](../GLOSSARY.md)** -
  [Title](../GLOSSARY.md), [Copy](../GLOSSARY.md),
  [Member](../GLOSSARY.md), [Loan](../GLOSSARY.md),
  [Active loan](../GLOSSARY.md) - so a reader, or the learner who wrote the
  test last week, weighs its claim in the same words the docs and the code
  speak.

You have read tests like that already: M06's `LoanControllerTests` asserted
the `409`'s status, the problem media type, the `title` "Loan limit reached"
and the `detail` in the rule's own words, and not one of those assertions
mentions `LoanService`, `borrow` or a package name. The strategy part is
therefore mostly already built - the Lab's work is where each of those
assertions lives: two tests that look alike at the boundary, "a borrow is
accepted" in the slice and the same request observed in the full context,
are worth different prices and prove different things, and the Lab makes
that decision explicit for each layer.

## The Lab

The Lab is the library application you last touched in M06 - loans and
reservations against the [Loan limit](../GLOSSARY.md), the boundary refusing
in its own words. Every behaviour it proves is under test already, and not
one test class is shaped for its own job: everything the suite asserts goes
through a `@SpringBootTest` full context, so a status code a slice could
prove brings up the whole application, M05's PostgreSQL container test lives
behind the diagnostics profile instead of in the suite, and nothing in the
file says why a test earns the context it boots.

Your job on the Start module is reshaping the suite, layer by layer:

1. a **slice test for the web layer** - the controller under `@WebMvcTest`
   with `@MockitoBean` substitutes, asserting what the boundary alone can
   answer;
2. a **full-context test for the wiring** - the seam level: the
   configuration bound in M03, the availability client, the persistence
   wiring, asserted once in their proper home rather than carried by every
   class;
3. a **container test for the repository** - PostgreSQL through Testcontainers
   2 and `@ServiceConnection`, the assertions the H2 rig cannot see, part of
   the suite rather than a profile-only artifact;

and evidence that each choice pays off - the measured boots and the
assertions each context is uniquely able to make, so "which test gets which
context" is a stated decision, reviewable like the rest.

The diagnostic tests state it precisely - one per layer - and fail with
messages naming what is missing:

```bash
./mvnw -f labs -pl lab-07-testing-start verify -Pdiagnostics
```

PostgreSQL diagnostics want a Docker daemon; skipped is no daemon, not a
pass. The Solution module is written from scratch, not patched from the
Start module - read it to see how little code it takes to move a test tier
once the context is the decision, rather than the annotation that was always
in the file.

## Quiz

Five questions about the delta table and the strategy it does not decide.
Answer first; the answers say why, not just what.

1. **About the slice row: a colleague's Boot 3 controller test imports
   `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`,
   the POM lists `spring-boot-starter-test` only, and on 4.1.1 it fails to
   compile. Name both fixes, and say why neither can be dropped.**

   Two things moved at once. The annotation's artifact: 4.1.1's
   `spring-boot-test-autoconfigure` no longer carries it - we listed the jar,
   and what remains there is `@JsonTest` and the jdbc-initialisation
   periphery, so the webmvc slice lives in `spring-boot-webmvc-test`. The
   dependency: `spring-boot-starter-test` did not lose testing ability,
   exactly the way M05 put it for the data slice - it stopped shipping the
   other technologies' slices, so the webmvc test starter is now
   `spring-boot-starter-webmvc-test`, test-scoped. Neither fix can be
   dropped, because each undoes half the move: on its own the new starter
   still compiles nothing, because the test still imports the old package -
   and the old package is empty in 4.1.1's jar - while on its own the new
   import resolves nothing, because its package lives in no artifact the
   old starter brings. Do the POM first, then the import; both are one-line
   changes, and the second one is the version-controlled half of the
   migration.

2. **About the mock-bean row: the same codebase on Boot 3.5 has forty
   `@MockBean`s and the team is upgrading straight to 4.1.1 in one weekend.
   What does the row say the safe order is?**

   Migrate on Boot 3 first - that is what the deprecation's shape records:
   `@Deprecated(since="3.4.0", forRemoval=true)` on `@MockBean`, and the
   replacement already present. `@MockitoBean` lives in
   `org.springframework.test.context.bean.override.mockito`, and we verified
   it is in the `spring-test` of 6.2.19 - the exact artifact Boot 3.5's
   starter already brings. So on 3.5 the team imports the new annotation,
   replaces the old ones (the two are annotation-identical in usage: value
   names, where to put it, disposal between tests), runs the suite, ships
   it - and the 4.1.1 weekend then changes nothing about the tests. Ordering
   it the other way, doing it inside the upgrade, mixes two causes in every
   red test: a removed annotation naming a compile error that *looks* like a
   Boot 4 regression, with nowhere to go - the `@MockBean` package is gone
   from 4.1.1's jars, not deprecated-and-aliased.

3. **About which context a test deserves: a
   `@WebMvcTest(LoanController.class)` slice is green, and the reviewer says
   the same assertions pass in the full-context version, so why pay for the
   slice at all?**

   The context is the *cost* of the test, not its assertion, so the question
   is reversible: the reviewer's full-context test asserts what the slice
   asserts, and additionally boots everything the library has. Measured on
   the lab application - full context 2.7 s, slice 1.4 s, same classes,
   same machine - the slice is the cheap one for the same observable
   outcome, the one the reviewer can check in the slice's assertions. The
   full context earns its cost when an *assertion about the wiring* exists:
   that the configuration binding plugged the limit into the service, that
   the availability client reads `library.availability.base-url`. When none
   of that is asserted, the extra seconds pay nothing, and forty of them in
   a suite say it over and over: the next edit that does not touch the web
   tier at all sits behind a context that cannot fail in any way the edit
   could hurt.

4. **About the Testcontainers row: a 1.21.4 test imports
   `org.testcontainers.containers.PostgreSQLContainer` from
   `org.testcontainers:postgresql` and
   `new PostgreSQLContainer<>("postgres:17")`. Name everything that has to
   change for 2.0.5.**

   Three changes, in two tiers. The POM tier: the artifact names changed -
   `postgresql` is now `testcontainers-postgresql`, and the annotations'
   module is `testcontainers-junit-jupiter`; we checked the published
   matrix, and the old coordinates stop at 1.x. The code tier, in the
   import and one line: the type moved to
   `org.testcontainers.postgresql` (the old one survives as a deprecated
   bridge, so the old import may still compile - with a deprecation
   warning - but the canonical type is the new one), and the diamond goes
   away: `PostgreSQLContainer` stopped self-typing, so
   `new PostgreSQLContainer("postgres:17")` is written with no `<>`. What
   does *not* change: the annotation imports - `@Testcontainers`,
   `@Container` are still `org.testcontainers.junit.jupiter` - and anything
   wired through the Spring fixture, like `@ServiceConnection`, which
   locates the container by type and not by package. We verified by
   compiling exactly this shape against 4.1.1's dependency tree.

5. **About what survives refactoring: a test calls
   `when(loanService.borrow("m-001", "c-001"))` and then asserts the status
   and the payload of the response. Another test verifies that
   `loanService.borrow` was called *before* `availabilityService.check`
   was, and that both were called exactly once. Which of these survives
   renaming `borrow` to `take`?**

   The rename breaks both files - the `when` line and the `verify` line are
   compile errors the moment `borrow` is really gone - so "which survives"
   has to be asked about what the test claims, not its raw source. The
   first test survives in the sense that matters: its claims were about the
   status and the payload, observed through the boundary, and they are
   untouched; the only edit is the stub line, mechanical and silent. The
   second test has nothing *but* the call ledger: every claim it makes is
   an arrangement claim - "borrow was called first", "both exactly once" -
   so after the fix its green asserts exactly what it asserted before,
   which was nothing the library's clients can observe. It wrapped an
   ordering no rename was needed to show, and no honest failure of the
   system would have made it red. Leaving it in the suite buys the next
   refactorer nothing but one more broken reference to patch, so the
   second test is the one to retire.

## Checklist

What a reviewer applies to a solution of this Lab to judge it production-shaped
rather than merely working. Every item is observable - checkable by reading
the change or running a command - and none of them repeats a gate the build
already enforces.

- **The web layer is asserted at a slice.** The controller's tests are
  `@WebMvcTest`-scoped, on `spring-boot-starter-webmvc-test`, with
  `@MockitoBean` where the constructor asks for a substitute, and the
  assertions are the boundary's: status, media type, payload shape, the
  problem detail's words. Collateral beans the API does not consult are not
  in the slice at all.
- **The wiring has one full-context home.** `@SpringBootTest` appears where
  the seam is the subject - the configuration bound in M03, the availability
  client, the persistence wiring - and the same suite does not repeat the
  full-context boot in a class whose assertions the slice already answers.
  A wiring test that stubs every bean it claims to wire is, in the review's
  honest words, a slice wearing the full context's annotations.
- **The repository is tested against the engine it ships on.** The
  persistence assertions run inside a container test: `@Container`,
  `@ServiceConnection`, the PostgreSQL image, `@EnabledIfDockerAvailable`
  guarding the class so a no-Docker machine is skipped rather than failed -
  and the skipped test is understood as a skip, never as green. Whatever is
  asserted on H2 and *not* asserted on PostgreSQL is stated as the
  difference, not left as an invisible assumption.
- **The stubs are the fixture's, not the test's inventions.** Where a bean is
  substituted, the annotations that carry it are `@MockitoBean` and
  `@MockitoSpyBean`; the typed stub's name is a `@MockitoBean` field rather
  than a hidden subclass, and no test reaches into a private field or a
  package-local method to "set the state" that a request could set.
- **Every test states a user-observable outcome.** The names say what a
  client of the library sees - a borrow accepted, a refusal told in the
  glossary's words, a [Loan](../GLOSSARY.md) surviving a restart - and the
  bodies assert status, media type, payload, problem `title`/`detail`, in
  that vocabulary. No assertion names a method it calls or a bean it
  crosses: rename-safe by construction, not by hope.
- **The costs are counted, not assumed.** The slice / full-context /
  container division was run, not merely written: the measured boots - the
  M06 lab application's 2.7 s full context against the 1.4 s slice - are
  recorded where the reader can find them, every test class is placed by
  the rule that evidence justified rather than by the neighbouring file's
  example, and a suite that claims "faster" says with what numbers.
- **The suite's red means "a claim is false", never "a stub is stale".** Run
  the whole module's own build
  (`./mvnw -f labs -pl lab-07-testing-solution verify`), then the
  diagnostics with the profile the Lab names
  (`./mvnw -f labs -pl lab-07-testing-solution verify -Pdiagnostics`); both
  are green, and the PostgreSQL diagnostics were actually run - or actually
  skipped because no daemon - and the run that was made is the one
  reported.
