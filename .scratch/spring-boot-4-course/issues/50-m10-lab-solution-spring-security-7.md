# 50: M10 lab solution: Spring Security 7

**What to build:** The Solution module for M10: the reference implementation of protecting the lending endpoints, authenticating a Member, and applying the loan limit per Member rather than globally, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 49

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Answer

The module is `labs/lab-10-security7-solution`, listed in the labs aggregator
directly after the start module (`labs/README.md`'s lab-index row already
carried both names). The application it changes is M09's answers - the probes,
the metric, the management socket - with the security upgrade written into it,
not bolted beside it: the chain is a bean, the Member is a principal, the body
no longer names a Member, and nothing of the M08 boundary or the M09 surface
moved.

**The chains are beans, in the lambda form 7.1.1 still accepts.**
`com.example.library.security` holds three classes: `SecurityConfiguration`
(two chains, both written under `@EnableWebSecurity`),
`MethodSecurityConfiguration` (`@EnableMethodSecurity` alone, with the
annotation-defaults reasoning) and `SecurityUsersConfiguration` (a
`UserDetailsService` bean and the `PasswordEncoder`). The management chain is
`@Order(1)`, selected with `securityMatcher` over
`PathPatternRequestMatcher.withDefaults().matcher("/actuator/**")` - the class
lives in `org.springframework.security.web.servlet.util.matcher` on 7.1.1 -
permitting `/actuator/health/**` and requiring `OPERATOR` of every other
exposed endpoint, Basic-authenticated. The API chain is `@Order(2)` with no
matcher: GET on `/api/titles/**` public, `hasRole("MEMBER")` on
`/api/loans/**`, `anyRequest().authenticated()` closing it; both chains are
`STATELESS` and disable CSRF in the same block, with the no-ambient-credential
argument written next to the lines. No adapter, no `authorizeRequests()`, no
`and()`, no Ant or MVC matcher, and - since Security 7.0's build lost its
checked exception - no `throws Exception` either; the lambdas are written so
the shared PMD ruleset stays satisfied (`Customizer.withDefaults()` and
`CsrfConfigurer::disable`).

**The principals are the diagnostics' names, stored with algorithm-prefixed
passwords.** `SecurityUsersConfiguration` declares the delegating
`PasswordEncoder` and an in-memory `UserDetailsService` whose entries -
Members `ada`, `bruno`, `cecil` at the `MEMBER` role, Operator `operator` at
`OPERATOR` - have their passwords produced by the encoder itself
(`.passwordEncoder(passwordEncoder::encode)`), so every stored value carries
the `{bcrypt}` prefix the factory writes. No plaintext sits in a property or a
migration, and no generated development principal is relied on.

**The borrow's Member is the principal, twice over.** `LoanRequest` keeps
only `copyId`; the controller injects `Authentication` and passes
`authentication.getName()` to the use case; `LoanService.borrow` carries
`@PreAuthorize("#memberId == authentication.name")` as the second lock, live
because `@EnableMethodSecurity`'s defaults run it. A request that still names
a memberId is accepted and ignored - the field is not in the record, so the
Loan lands on the caller however the body is written - and the M08 boundary
and the M09 counter run exactly where they did.

**The own suite asserts the Lab's behaviour, not the arrangement.** The
borrow's authorisation is asserted at the webmvc slice with the real chains
riding the slice: a new `LoanSecurityTests` - unauthenticated POST answered
401, an `OPERATOR` principal answered 403, a Member's borrow created and the
use case verified to receive the principal; `LoanControllerTests` reworded
for the one-field body (including a test that a body still carrying memberId
is honoured in the principal's name); `ReservationControllerTests` and
`TitleControllerTests` call through the real chains (the title slice thereby
asserts the public rule, unauthenticated); `LoanWiringTests` borrows as
ada via `httpBasic` through the real chain, encoder and store, clearing that
Member's loan rows before each test so the limit is driven per Member rather
than against a residue. The repository and container tests are untouched -
they are HTTP-free, and HTTP-free classes need no credentials.

**Verification.** The strict module build
(`./mvnw -f labs -pl lab-10-security7-solution -am verify`) is green: 27 tests
plus the packaged-jar IT, JaCoCo at 87.6% line / 87.5% branch (floor 60/50),
Spotless/PMD/SpotBugs/Checkstyle 0. The diagnostics asymmetry: 5/5 red on the
start module with the messages naming every missing behaviour
(`-Pdiagnostics`), 5/5 green on the solution with the same profile. CI's
statement, `./mvnw -f labs verify -Pdiagnostics-solutions`, is green across
the reactor (42 modules), with lab-10's solution diagnostics run and the start
module skipped through `diagnostics.skip=true`; the plain
`./mvnw -f labs verify` is green as well. Attested the way M09's run was: red
on the start, then the green solution, then the whole reactor.

