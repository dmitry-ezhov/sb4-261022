# 02: One diagnostics command that selects Solution modules

**What to build:** A single command that runs the Diagnostic tests of every Solution module and none of the Start modules, so that CI can assert every reference implementation in one step instead of growing a list of module names.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] the diagnostics profile skips Start modules and runs Solution modules
- [x] one command over the whole reactor asserts every Solution's diagnostics
- [x] the command still runs a Start module's diagnostics when that module is selected explicitly
- [x] CI uses that command in place of a hand-maintained module list

## Answer

Built in commit `1b97d87`.

- **A new `diagnostics-solutions` profile** in the root parent POM. It runs the
  Diagnostic tests and skips every module that sets `diagnostics.skip`, which a
  Start module sets in its own POM. CI runs
  `./mvnw -f labs verify -Pdiagnostics-solutions` and now asserts every
  reference implementation with no module list to maintain.
- **The learner's command is unchanged.** `-Pdiagnostics` against a Start
  module still runs that module's diagnostics, and still fails - which is the
  point of a Start module. The skipping lives in a separate profile precisely
  so that selecting a Start module explicitly still runs them.
- **Verified**:
  - `./mvnw -f labs verify -Pdiagnostics-solutions` - green; the Solution
    module's three diagnostics run and pass, the Start module's are skipped.
  - `./mvnw -f labs -pl lab-01-boot2-to-boot4-start verify -Pdiagnostics` -
    red, three failures: the diagnostics ran.
  - `./mvnw -f labs -pl lab-01-boot2-to-boot4-solution verify -Pdiagnostics` -
    green.
  - `./mvnw -f labs verify` - green with every gate enabled.
- **Docs updated**: the CI workflow, the lab index, the authoring guide, the
  root README and AGENTS.md.

### A note on the specification

The specification's acceptance criteria say that
`./mvnw -f labs verify -Pdiagnostics` is green on every Solution module. Over
the whole reactor that command is red, because a Start module's diagnostics
fail by design. The two commands are now distinct: `-Pdiagnostics` is the
learner's, per module; `-Pdiagnostics-solutions` is CI's, over the whole
reactor. The final audit ticket checks this refinement rather than the
criterion's wording.
