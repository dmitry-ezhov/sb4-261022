# 55: M11 lab solution: Resilience and virtual threads

**What to build:** The Solution module for M11: the reference implementation of running the application on virtual threads, adding a timeout and a retry with backoff to an outbound call, and making shutdown drain in-flight work, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 54

**Status:** ready-for-agent

- [ ] the Solution module exists and is listed in the labs aggregator
- [ ] it is written from scratch, not patched from the Start module
- [ ] every diagnostic of the Start module passes against it
- [ ] it meets the coverage floor
- [ ] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [ ] CI asserts its diagnostics through the single diagnostics command
