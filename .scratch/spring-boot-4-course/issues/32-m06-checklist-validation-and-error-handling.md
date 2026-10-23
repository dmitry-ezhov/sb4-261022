# 32: M06 checklist: Validation and error handling

**What to build:** The checklist for M06: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 31

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers invalid input is rejected at the boundary
- [x] it covers domain failures are mapped in exactly one place
- [x] it covers no framework exception escapes to the client untyped
- [x] it covers both failure kinds are covered by tests
- [x] no item restates a Quality gate that the build already enforces

## Answer

The doc already carried a checklist draft, written under ticket 28, that met
five of the criteria. Two gaps remained and both are now said outright:

- **Domain failures are mapped in exactly one place.** The draft's closest
  item said the translation lived in one *layer* - `ProblemDetail` imported
  only in the `api` package - and the `409` item said nothing else answers
  for the [Loan limit](../../../GLOSSARY.md); between them, "exactly one
  place" was a conclusion the reviewer had to draw. The item now states it: a
  domain failure is mapped by exactly one `@ExceptionHandler`, one method per
  exception, all methods of the one handler class, `ProblemDetail` imported
  nowhere else - and a second mapping for an exception one method already
  answers, like a `ProblemDetail` built inside a service method, is the
  boundary leaking inward. Checked against the reference: `ApiExceptionHandler`
  holds one method per exception (`TitleNotFoundException`, the two validation
  exceptions, `LoanLimitExceededException`, `CopyNotAvailableException`, the
  outbound `404`), and a grep confirms `ProblemDetail` in no other main class
  of either lab half.
- **No framework exception escapes to the client untyped.** The named kinds
  were covered - the `400` item forbids the default error body, the
  parameter item forbids the Boot 2 trap of a `500` with a stack trace - but
  the escape routes a solution can invent were not. A new item forbids the
  catch-all: it enumerates the failures the handler answers, demands no
  `@ExceptionHandler(Exception.class)` deciding them instead, and says where
  an unmapped failure belongs - the status its type carries, and with the
  switch on a problem detail. The switch is for the failures nobody
  anticipated; a catch-all handler is a third layer the ownership table has
  no row for. Checked against the reference: `ApiExceptionHandler` has no
  catch-all method.

Everything else stood. The bad-shape item is the boundary claim verbatim of
the diagnostics; the service item confines the [Loan
limit](../../../GLOSSARY.md) to `LoanService` counting against
`properties.limit()`, no constraint on the request record, controller not
counting; the run item names the command
`./mvnw -f labs -pl lab-06-validation-and-errors-solution verify -Pdiagnostics`,
re-run green for this ticket (2/2 diagnostics, 10/10 module tests, every gate).
No item restates a gate: formatting, the analysis rules, the coverage floor
and the environment guard say nothing about where a mapping lives or what a
catch-all answers.
