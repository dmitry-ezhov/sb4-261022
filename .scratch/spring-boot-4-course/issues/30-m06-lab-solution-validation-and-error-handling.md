# 30: M06 lab solution: Validation and error handling

**What to build:** The Solution module for M06: the reference implementation of validating the borrow request, rejecting it with a problem detail, and keeping the loan limit as a domain rule rather than an input constraint, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 29

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Answer

Built in this ticket's commit:
[`labs/lab-06-validation-and-errors-solution`](../../../labs/lab-06-validation-and-errors-solution),
listed in `labs/pom.xml` after the M06 Start module.

- **The rule is the service's, exactly as the doc shows it.** `LoanService`
  counts the Member's Active loans against `properties.limit()` where
  borrowing happens, and throws `LoanLimitExceededException` with the
  glossary's wording (`"A member may hold 3 Active loans at once"`). The
  store is `LoanRepository` - in memory for now, `countByMemberId` and a
  `save` that mints the library's identifier - the shape the transaction lab
  replaces with the database without the rule noticing. The controller keeps
  the gate it already had (the request's shape by `@Valid`, the Copy by the
  availability service) and passes borrowing itself to the service: it does
  not count, and no constraint on `LoanRequest` owns the limit.
- **A borrow request's failure speaks the loan's words.** The shared
  `@ExceptionHandler` cannot have two titles for one exception type, so the
  title is the request's own: `LoanRequest` and `ReservationRequest`
  implement a small `api.InvalidRequestTitle` and the handler asks the
  failing body for its title (`"Invalid loan request"` /
  `"Invalid reservation request"`), falling back to a plain
  `"Invalid request"` for a body that names no feature. The field errors stay
  in the problem detail under `errors`, unchanged from M04. The handler also
  gained the doc's `HandlerMethodValidationException` method - the
  Framework 6.1 parameter failure, `400`, parameter named in `errors` -
  dormant until a Loan endpoint carries a parameter constraint, per the
  checklist's "covers like the body exceptions".
- **The 409 is the handler's translation.** One `@ExceptionHandler` per
  domain refusal: `LoanLimitExceededException` answers `409`
  `application/problem+json` titled `"Loan limit reached"`; the import of
  `ProblemDetail` stays in the `api` package, exactly one layer.
- **The diagnostics pass 2/2 here and stay red 2/2 on the Start module**,
  each failure naming its requirement. The default build is green on both:
  formatting, PMD, SpotBugs, Checkstyle, Enforcer, and - on the Solution
  only - the JaCoCo floor. `./mvnw -f labs verify -Pdiagnostics-solutions`
  is green, so CI's single diagnostics command asserts the reference
  implementation without a hand-maintained module list.
- **The own tests assert what a client observes**, not the arrangement:
  `LoanControllerTests` runs the borrow loop against a studded availability
  service and checks the status, media type, problem detail's title and
  detail, and the field error naming `member` - and that the first three
  loans of a still-otherwise-empty member are granted (`201`) before the
  fourth is refused. `ReservationControllerTests` gains the mirror image:
  an invalid reservation still speaks the reservation's words, so the shared
  handler's fix is visible from both directions. The M05 tests carried over
  unchanged; `ProblemDetail` appears in no feature package.
