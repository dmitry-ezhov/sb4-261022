# 45: M09 lab solution: Actuator and observability

**What to build:** The Solution module for M09: the reference implementation of exposing a liveness and a readiness probe, publishing a metric about loans, and making sure the management endpoints are not reachable from the public API, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 44

**Status:** ready-for-agent

- [ ] the Solution module exists and is listed in the labs aggregator
- [ ] it is written from scratch, not patched from the Start module
- [ ] every diagnostic of the Start module passes against it
- [ ] it meets the coverage floor
- [ ] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [ ] CI asserts its diagnostics through the single diagnostics command
