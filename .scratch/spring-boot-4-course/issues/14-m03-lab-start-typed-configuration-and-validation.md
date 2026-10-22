# 14: M03 lab start: Typed configuration and validation

**What to build:** The Start module for M03: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers binding the loan period and the loan limit to typed configuration, failing startup on invalid values, and covering both the valid and the invalid case with tests.

**Blocked by:** 13

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Answer

Built in commit `9f00425`.

- **The Start module** is the application M02 left, plus the Loan feature:
  a `Loan` record, a `LoanRequest` with Jakarta constraints, a
  `POST /api/loans` endpoint, and a `Clock` the application injects rather
  than calling `LocalDate.now()` on. The course's vocabulary is now
  complete - Title, Copy, Member, Loan, Reservation.
- **The defect is the configuration**: the loan period is a constant in the
  code (`LOAN_PERIOD_DAYS = 14`), and there is no loan limit at all.
- **Two diagnostics**:
  - with `library.loan.period=30D`, a loan created today must be due in
    thirty days - the start module says fourteen, and fails with
    `JSON path "$.dueOn" expected:<2026-02-04> but was:<2026-01-19>`;
  - with `library.loan.limit=0`, the application must not start, and the
    failure must name the property.
- **The first lab to consume the shared fixture module**: the tests import
  `labs-support`'s test-jar and run on `TestClocks.fixed()`, so "today" is
  the same on every machine.
- **Verified**: the default build is green; the diagnostics run is red with
  two named failures.

## Answer

Built in commit `68cc1a4`.

- **The Start module** is the application M02 left, plus the Loan feature:
  a `Loan` record, a `LoanRequest` with Jakarta constraints, a
  `POST /api/loans` endpoint, and a `Clock` the application injects rather
  than calling `LocalDate.now()` on. The course's vocabulary is now
  complete - Title, Copy, Member, Loan, Reservation.
- **The defect is the configuration**: the loan period is a constant in the
  code (`LOAN_PERIOD_DAYS = 14`), and there is no loan limit at all.
- **Two diagnostics**:
  - with `library.loan.period=30D`, a loan created today must be due in
    thirty days - the start module says fourteen, and fails with
    `JSON path "$.dueOn" expected:<2026-02-04> but was:<2026-01-19>`;
  - with `library.loan.limit=0`, the application must not start, and the
    failure must name the property.
- **The first lab to consume the shared fixture module**: the tests import
  `labs-support`'s test-jar and run on `TestClocks.fixed()`, so "today" is
  the same on every machine.
- **Verified**: the default build is green; the diagnostics run is red with
  two named failures.
