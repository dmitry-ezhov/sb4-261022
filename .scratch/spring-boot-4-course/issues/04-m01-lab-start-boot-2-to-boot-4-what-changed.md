# 04: M01 lab start: Boot 2 to Boot 4: what changed

**What to build:** The Start module for M01: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers the catalogue API brought up to Boot 4 conventions: typed record payloads, RFC 9457 problem details, and the modularised starters.

**Blocked by:** 03

**Status:** resolved

- [ ] the Start module exists and is listed in the labs aggregator
- [ ] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [ ] its diagnostics fail, and each failure message names what is missing
- [ ] the default build stays green; the diagnostics run is red
- [ ] the application is the one the previous Module left, extended rather than rewritten

## Answer

Built in commit `e64b7e9`.

- **The Start module now carries the whole Boot 2 habit set**, so its
  diagnostics can demand the whole treatment: the reservation request body is
  checked by hand rather than by `jakarta.validation`, the created reservation
  comes back in an envelope rather than as a typed record, a rejected request
  says nothing about what was wrong, and the POM still uses the deprecated
  `spring-boot-starter-web`.
- **Two new features**, so the vocabulary is real from the first Module:
  `Member` and `Reservation`, in their own packages.
- **Two new diagnostics**, five in total. Each fails with a message that names
  what is missing - "No value at JSON path `$.memberId`", "Content type not
  set" - never "expected true but was false".
- **The start module stays green.** Its own tests assert only what holds in
  both halves: the status code of a valid and of an invalid reservation.
- **PMD's `OnlyOneReturn` was curated out** of the ruleset: it is a style rule
  that fights the guard clause, and it catches no defects.
