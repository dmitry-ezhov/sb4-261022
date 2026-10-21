# 52: M10 checklist: Spring Security 7

**What to build:** The checklist for M10: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 51

**Status:** ready-for-agent

- [ ] the checklist names observable properties, not implementation steps
- [ ] every item is verifiable by reading the change or running a command
- [ ] it covers access is denied unless explicitly granted
- [ ] it covers no secret is hard-coded or logged
- [ ] it covers authorization is enforced at the boundary and in the domain
- [ ] it covers the unauthenticated and the forbidden case are both tested
- [ ] no item restates a Quality gate that the build already enforces
