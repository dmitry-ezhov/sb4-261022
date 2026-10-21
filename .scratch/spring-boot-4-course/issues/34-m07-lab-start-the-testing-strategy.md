# 34: M07 lab start: The testing strategy

**What to build:** The Start module for M07: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers reshaping the existing tests into the right form for each layer - a slice test for the web layer, a full context test for the wiring, a container test for the repository - and proving each choice pays off.

**Blocked by:** 33

**Status:** ready-for-agent

- [ ] the Start module exists and is listed in the labs aggregator
- [ ] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [ ] its diagnostics fail, and each failure message names what is missing
- [ ] the default build stays green; the diagnostics run is red
- [ ] the application is the one the previous Module left, extended rather than rewritten
