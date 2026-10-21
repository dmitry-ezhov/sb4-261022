# 57: M11 checklist: Resilience and virtual threads

**What to build:** The checklist for M11: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 56

**Status:** ready-for-agent

- [ ] the checklist names observable properties, not implementation steps
- [ ] every item is verifiable by reading the change or running a command
- [ ] it covers no blocking call runs on a carrier thread
- [ ] it covers every outbound call has a timeout
- [ ] it covers shutdown drains in-flight work instead of dropping it
- [ ] it covers saturation is visible in a metric
- [ ] no item restates a Quality gate that the build already enforces
