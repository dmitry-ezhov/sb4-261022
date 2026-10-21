# 42: M08 checklist: Transactions and consistency

**What to build:** The checklist for M08: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 41

**Status:** ready-for-agent

- [ ] the checklist names observable properties, not implementation steps
- [ ] every item is verifiable by reading the change or running a command
- [ ] it covers the boundary wraps the use case, not the repository
- [ ] it covers the concurrency guarantee is proved by a failing-then-passing test
- [ ] it covers no transaction is opened on a read-only path
- [ ] it covers the isolation level is a deliberate choice, stated in the doc
- [ ] no item restates a Quality gate that the build already enforces
