# 43: M09 doc: Actuator and observability

**What to build:** The Module doc for M09: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers the actuator starter, liveness against readiness, Micrometer metrics, structured logging, OpenTelemetry, and what must never be exposed, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 42

**Status:** ready-for-agent

- [ ] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering actuator endpoints, probes and metrics
- [ ] every claim in the delta table is checked against the version in use, not remembered
- [ ] the terms it introduces are the glossary's terms, and are linked
- [ ] it names the Lab, and the command that runs the Lab's diagnostics
- [ ] every snippet in it compiles against the version in use
- [ ] it states what the reader is assumed to know and what they are not
