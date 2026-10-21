# 32: M06 checklist: Validation and error handling

**What to build:** The checklist for M06: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 31

**Status:** ready-for-agent

- [ ] the checklist names observable properties, not implementation steps
- [ ] every item is verifiable by reading the change or running a command
- [ ] it covers invalid input is rejected at the boundary
- [ ] it covers domain failures are mapped in exactly one place
- [ ] it covers no framework exception escapes to the client untyped
- [ ] it covers both failure kinds are covered by tests
- [ ] no item restates a Quality gate that the build already enforces
