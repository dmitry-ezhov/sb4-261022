# 15: M03 lab solution: Typed configuration and validation

**What to build:** The Solution module for M03: the reference implementation of binding the loan period and the loan limit to typed configuration, failing startup on invalid values, and covering both the valid and the invalid case with tests, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 14

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Answer

Built in commit `4d9d539`.

- **The Solution module** binds the rules:
  - a `@ConfigurationProperties(prefix = "library.loan")` record with the
    period and the limit;
  - `@Validated` on it, `@NotNull` on the period, `@Min(1)` on the limit;
  - `@ConfigurationPropertiesScan` on the application class, so the record is
    registered without being listed anywhere;
  - the defaults in `application.yml`, where whoever deploys the application
    can see them - `period: 14d`, `limit: 3`.
- **The loan controller** reads the period from the bound properties. A loan
  period is a number of days and a due date is a date, so the bound
  `Duration` is converted to days - which is what the glossary says a loan
  period is.
- **Both diagnostics pass**, the reactor is green with every gate enabled -
  including the coverage floor - and the CI command asserts it.
- **The Start module's two diagnostics still fail**, each naming what is
  missing.

## Answer

Built in commit `4d9d539`.

- **The Solution module** binds the rules:
  - a `@ConfigurationProperties(prefix = "library.loan")` record with the
    period and the limit;
  - `@Validated` on it, `@NotNull` on the period, `@Min(1)` on the limit;
  - `@ConfigurationPropertiesScan` on the application class, so the record is
    registered without being listed anywhere;
  - the defaults in `application.yml`, where whoever deploys the application
    can see them - `period: 14d`, `limit: 3`.
- **The loan controller** reads the period from the bound properties. A loan
  period is a number of days and a due date is a date, so the bound
  `Duration` is converted to days - which is what the glossary says a loan
  period is.
- **Both diagnostics pass**, the reactor is green with every gate enabled -
  including the coverage floor - and the CI command asserts it.
- **The Start module's two diagnostics still fail**, each naming what is
  missing.
