# 35: M07 lab solution: The testing strategy

**What to build:** The Solution module for M07: the reference implementation of reshaping the existing tests into the right form for each layer - a slice test for the web layer, a full context test for the wiring, a container test for the repository - and proving each choice pays off, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 34

**Status:** ready-for-agent

- [ ] the Solution module exists and is listed in the labs aggregator
- [ ] it is written from scratch, not patched from the Start module
- [ ] every diagnostic of the Start module passes against it
- [ ] it meets the coverage floor
- [ ] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [ ] CI asserts its diagnostics through the single diagnostics command
