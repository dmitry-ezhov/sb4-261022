# 12: M02 checklist: The Boot 4 module map and a multi-module build

**What to build:** The checklist for M02: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 11

**Status:** resolved

- [ ] the checklist names observable properties, not implementation steps
- [ ] every item is verifiable by reading the change or running a command
- [ ] it covers one parent owns every version
- [ ] it covers no child repeats a version or a plugin configuration
- [ ] it covers the child declares only what it uses
- [ ] it covers the reactor still builds green from the root
- [ ] no item restates a Quality gate that the build already enforces

## Answer

Built in commit `eb33ba7`.

Eight observable properties: the module declares only what it uses, no
dependency pins a version, no plugin pins a version, nothing the parent
already provides is declared again, the starters are the Boot 4 names, the
aggregator lists the module and configures nothing, the vocabulary is the
glossary's, and the behaviour did not change.

None repeats a gate the build enforces, and the last item is the judgement
call the diagnostics cannot make: a build fix that quietly changes behaviour
is a build fix with a bug in it.
