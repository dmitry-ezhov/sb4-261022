# 17: M03 checklist: Typed configuration and validation

**What to build:** The checklist for M03: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 16

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers no raw value injection anywhere
- [x] it covers configuration is typed and validated
- [x] it covers defaults are documented where they are declared
- [x] it covers the invalid case is covered by a test
- [x] no item restates a Quality gate that the build already enforces

## Answer

Built in commit `19278a9` (together with the quiz, ticket 16).

Eight observable properties: the rules are configuration rather than
constants, the configuration class is a record and is registered exactly
once, the defaults live in the configuration file, the constraints have an
implementation behind them, an invalid value stops the application at
startup with a message that names the property, the vocabulary is the
glossary's, the clock is injected rather than called statically, and the
failures are still problem details.

None repeats a gate the build enforces.
