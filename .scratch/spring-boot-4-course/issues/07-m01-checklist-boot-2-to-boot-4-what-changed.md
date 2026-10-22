# 07: M01 checklist: Boot 2 to Boot 4: what changed

**What to build:** The checklist for M01: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 06

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers every javax import is now jakarta
- [x] it covers no Jackson 2 type appears in application code
- [x] it covers every starter is named the Boot 4 way
- [x] it covers errors are problem details with the right status and media type
- [x] no item restates a Quality gate that the build already enforces

## Answer

Built in commit `126a728`.

Eight observable properties: typed payloads, the glossary's vocabulary,
failures that say what happened, the Boot 4 starters, the Jakarta namespaces,
Jackson 3 databind, the framework's validation, and the relocated test slice.

Each is verifiable by reading the change. None repeats a gate the build
already enforces - the build owns formatting, the static analysis rules, the
coverage floor and the environment guard.

The Jackson item is precise on purpose: the annotations are still Jackson 2's,
and that is correct rather than an oversight.
