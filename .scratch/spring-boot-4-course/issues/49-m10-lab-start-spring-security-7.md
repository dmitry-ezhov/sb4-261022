# 49: M10 lab start: Spring Security 7

**What to build:** The Start module for M10: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers protecting the lending endpoints, authenticating a Member, and applying the loan limit per Member rather than globally.

**Blocked by:** 48

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Comments

### 2026-10-24 — implemented

`labs/lab-10-security7-start` is `lab-09-observability-solution`'s application
carried forward untouched - the probes, the metric and the separate management
socket all kept - with nothing of the security upgrade in it: the POM carries
no security starter, so no chain exists, the lending endpoints answer any
caller, the borrow trusts the body's `memberId`, and the Loan limit counts
that body value. The module sets the two start-module properties
(`jacoco.skip`, `diagnostics.skip`) and is listed in `labs/pom.xml`.

The requirement is stated by
`src/test/java/com/example/library/diagnostics/SecurityDiagnosticsTest.java`,
five diagnostics over the real sockets (the same rig M09's diagnostics use:
whole application on a random port, the management socket included, a fresh
H2 per run, the availability service stubbed):

1. `anUnauthenticatedBorrowIsRefusedWith401` - the catalogue read still
   answers publicly, the unauthenticated borrow is 401, and a second assert
   states the chain's last rule: an unauthenticated call on the reservation
   surface (the application's other open endpoint) answers 401, so every
   path the rules do not name closes.
2. `aPrincipalWithoutTheMemberRoleIsRefusedWith403` - the URL rule answers
   the role, before the use case runs.
3. `aLoanBelongsToTheAuthenticatedMemberNotTheBody` - a body that names
   somebody else is ignored and the row's owner is the caller; a body that
   names no Member at all works as well as the one that did.
4. `theLoanLimitCountsTheAuthenticatedMembersLoans` - a Member at the limit
   hears the domain's own 409 ("Loan limit reached"), a Member under it
   borrows through: the count is the principal's, not the body field's and
   not a global total.
5. `theManagementSurfaceAnswersOnlyToAuthentication` - the health probe
   stays public, the metrics read stops being a stranger's read.

Verified both ways:

- `./mvnw -f labs -pl lab-10-security7-start -am verify` - green (own 23
  tests and the packaged-jar IT, every gate except the coverage floor, which
  the start module opts out of); the full `./mvnw -f labs verify` reactor
  run is green too.
- the same with `-Pdiagnostics` - red, 5 failures, each message naming the
  behaviour and the pieces it is missing (starter and chain bean, the
  `hasRole("MEMBER")` rule, `authentication.getName()` plus dropping
  `memberId` from the request record, `@EnableMethodSecurity` and the
  `#memberId == authentication.name` expression, and the management chain's
  ordering and matcher).

Useful facts the solution author can rely on (checked, not assumed): the
POM the build resolves carries Jackson 3.1.5's
`DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` **disabled by
default** (`iconst_0` in the 3.1.5 jar's constant construction), so
a `LoanRequest` without `memberId` ignores a body that still carries the
old field - the ignore-don't-refuse behaviour the third diagnostic asks
for needs no configuration line.

The diagnostics name the principals a solution must define so the two
modules can be checked against one contract: Members `ada`, `bruno` and
`cecil` (`member-password`, `MEMBER` role) and an `operator` principal
(`operator-password`, `OPERATOR` role), passwords encoded by a
`PasswordEncoder`. Tests 3 and 4 borrow as different Members (`ada`,
`cecil`) so their Active-loan counts cannot depend on the order JUnit runs
the diagnostics in.
