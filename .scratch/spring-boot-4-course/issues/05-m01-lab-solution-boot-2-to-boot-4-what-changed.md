# 05: M01 lab solution: Boot 2 to Boot 4: what changed

**What to build:** The Solution module for M01: the reference implementation of the catalogue API brought up to Boot 4 conventions: typed record payloads, RFC 9457 problem details, and the modularised starters, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 04

**Status:** resolved

- [ ] the Solution module exists and is listed in the labs aggregator
- [ ] it is written from scratch, not patched from the Start module
- [ ] every diagnostic of the Start module passes against it
- [ ] it meets the coverage floor
- [ ] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [ ] CI asserts its diagnostics through the single diagnostics command

## Answer

Built in commit `301b721`.

- **The Solution module**, written from scratch rather than patched from the
  Start module: a typed `ReservationRequest` with `jakarta.validation`
  constraints validated by the framework, a typed `Reservation` record in the
  response with no envelope, and a problem detail for a rejected request that
  carries the field errors.
- **The POM** uses the modularised web starter and adds the validation starter
  the constraints need.
- **All five diagnostics pass**, and the reactor is green with every gate
  enabled - including the coverage floor, which the unused `Member` record
  did not break.
- The Start module's five diagnostics still fail, each naming what is
  missing. The two halves are in sync.
