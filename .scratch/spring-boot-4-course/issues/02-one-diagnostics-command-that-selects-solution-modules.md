# 02: One diagnostics command that selects Solution modules

**What to build:** A single command that runs the Diagnostic tests of every Solution module and none of the Start modules, so that CI can assert every reference implementation in one step instead of growing a list of module names.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] the diagnostics profile skips Start modules and runs Solution modules
- [ ] one command over the whole reactor asserts every Solution's diagnostics
- [ ] the command still runs a Start module's diagnostics when that module is selected explicitly
- [ ] CI uses that command in place of a hand-maintained module list
