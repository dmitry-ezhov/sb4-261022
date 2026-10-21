# 39: M08 lab start: Transactions and consistency

**What to build:** The Start module for M08: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers making the borrow operation transactional so two concurrent borrows cannot take the same Copy, proved by a test that fails without the lock.

**Blocked by:** 38

**Status:** ready-for-agent

- [ ] the Start module exists and is listed in the labs aggregator
- [ ] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [ ] its diagnostics fail, and each failure message names what is missing
- [ ] the default build stays green; the diagnostics run is red
- [ ] the application is the one the previous Module left, extended rather than rewritten
