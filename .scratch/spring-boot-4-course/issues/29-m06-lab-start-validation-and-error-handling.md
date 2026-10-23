# 29: M06 lab start: Validation and error handling

**What to build:** The Start module for M06: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers validating the borrow request, rejecting it with a problem detail, and keeping the loan limit as a domain rule rather than an input constraint.

**Blocked by:** 28

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Answer

Built in this ticket's commit: [`labs/lab-06-validation-and-errors-start`](../../../labs/lab-06-validation-and-errors-start), listed in `labs/pom.xml` after the M05 pair.

- **The application is the one M05 left, verbatim.** Its `src` tree came off
  `lab-05-persistence-solution` and is byte-identical to it apart from the
  diagnostics: the catalogue is the migrated database, the availability
  service is consulted through the HTTP Service client, and the copied test
  suite (plus the packaged-jar IT) passes unchanged - 7 tests and the IT, no
  repository rewrite. The POM carries the application's own classpath
  (data-jpa, flyway, the second engine's modules) and drops M05's
  Testcontainers diaspora: M06's diagnostics run on H2, so no second engine is
  wired here. The coverage-floor opt-in and the `diagnostics.skip` for
  CI's solutions-only profile are the same properties every Start module sets.
- **One diagnostics class**, `ValidationAndErrorHandlingDiagnosticsTest`, two
  tests, one per requirement of the Lab:
  - `anInvalidBorrowSpeaksTheLoansWords` borrows with a blank member field and
    requires a `400` served as `application/problem+json` whose `errors` list
    the failed field, and whose title speaks the *loan's* words and not the
    reservation's. It fails today on exactly that wording: the shared
    handler's quote, `Invalid reservation request`, is printed in the
    failure.
  - `aMemberAtTheLoanLimitHasNoFurtherLoan` stubs every copy on the shelf,
    borrows three copies for a member the diagnostics own (a unique id, so no
    neighbouring context can fill them up first), and asks for a fourth. It
    requires a `409` problem detail whose title names the Loan limit
    (glossary's terms, never a number in a field error, and - the message
    says this out loud - never a constraint on the request record, because
    the request is well formed and no constant constraint can read the bound
    limit). It fails today because the fourth loan is granted: the
    `library.loan.limit` M03 bound is read by nobody.
- **Verified in both directions.** The default build
  (`./mvnw -f labs -pl lab-06-validation-and-errors-start verify`-style
  selection) is green under every gate; `... verify -Pdiagnostics` is red
  2/2 with each failure naming its requirement. The diagnostics were also
  graded against a throwaway implementation (a `LoanService` counting the
  member's Active loans against `properties.limit()`, the domain exception,
  and the two handler methods) to prove they are satisfiable - both green
  there; none of that scratch code is committed, and the module's sources
  were re-diffed against the M05 solution afterwards.
- **Not included** (belongs to the Solution module, ticket 30): the
  `LoanService` and its repository, `LoanLimitExceededException`, the
  loan-titled translation in the handler, and any parameter constraints the
  refinements of the Lab call for.
