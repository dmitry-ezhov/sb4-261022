# 47: M09 checklist: Actuator and observability

**What to build:** The checklist for M09: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 46

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers each probe answers the question it is named for
- [x] it covers no sensitive data reaches the metrics
- [x] it covers management endpoints are protected or not exposed
- [x] it covers the metric is worth having - it answers a question someone asks
- [x] no item restates a Quality gate that the build already enforces

## Answer

The checklist was drafted with the doc (ticket 43); this ticket graded it
against its criteria and closed the three that were open.

- **Two criteria the draft already met:** the management-surface criterion,
  held by two items that already existed (the separate
  `management.server.port` - merging the ports only over a decision
  *written down in the change* - and the never-list: no `include=*`, no
  `management.endpoint.shutdown.enabled`, no `show-details=always` outside
  a test's own configuration, the inspection endpoints behind the default
  exposure); and the two meta-criteria, observable properties and
  verifiability, which every item already met by naming what a read of the
  change answers or the command that decides (a boot read of the two probe
  paths, a GET of `/actuator/metrics/library.loans.issued`, diff searches
  for the alias prefixes and the never-list strings, the two-way
  diagnostics run). No item restates a gate: the gates speak in
  formatting, defects and coverage; the checklist speaks in probes,
  metrics and exposure.
- **Each probe answers the question it is named for - criterion the draft
  half-met, retitled and completed.** The draft's first item verified only
  liveness's half: no database check, no availability client in the
  liveness path. Readiness was covered by a separate item - but that item
  verified the *publishing mechanism* (`AvailabilityChangeEvent.publish`,
  not a boolean), not that readiness answers *its* named question. The
  item is now titled "Each probe answers the question it is named for"
  and verifies both questions against their own halves: liveness answers
  "restart me?" about the process (the original search of the diff), and
  readiness answers "route me?" - so the dependency that can be
  temporarily unreachable (the availability service above all) appears in
  no liveness group, and what its outage moves is readiness's
  `OUT_OF_SERVICE`, the publish the next item verifies in the diagnostics.
  The two items now carry the question and the mechanism between them.
- **The metric is worth having - new item, placed before the
  published-where-it-happens item.** Checked against the resolved
  solution before it was written: `LoanService.borrow`'s comment already
  states the fact the counter exists for and no other, and
  `library.loans.issued` answers the question the Module's intro named -
  "how many [Loan](../GLOSSARY.md)s did it issue this morning" - so the
  item takes that shape as its passing case: name the question and who
  reads the answer, one sentence where the change justifies each
  increment; a counter whose question nobody can state (a vanity number,
  a second count of `http.server.requests`) is deleted rather than kept,
  tagged and tuned. Verifiable by reading the change.
- **No sensitive data reaches the metrics - new item.** The draft's tag
  item bounded *cardinality* (never a per-request id, never a Copy
  identifier) but never said the metrics are readable by everyone the
  management port reaches, checked as a stranger would. The new item
  bans Member identity from every tag name and value - privacy and, being
  unbounded, cardinality in the same line, which is why the diagnostics'
  own message names `a Member id` beside `a Copy identifier` among the
  tags never to write - and adds the one place a diff can hide a leak: a
  `management.metrics.tags.*` global tag, applied to every metric at
  once. The `withDetail` half (a computed number, never a fetched
  string) stays with the custom-indicator item, and that item also
  gained the conditional its placement needs - the Lab does not ask for a
  custom indicator, so the criterion binds one *where the change adds
  one*.
- **Confirmed with a run:** the checklist's own last item executed both
  ways - `./mvnw -f labs -pl lab-09-observability-start -am verify
  -Pdiagnostics` (red: 3 tests, 3 failures, each message naming the
  missing behaviour - no actuator in the build, no management socket), then
  the same against `lab-09-observability-solution` (green: 3 tests, 0
  failures, and the module's own tests all pass). Red on the Start module,
  green on the Solution module - the asymmetry the item describes.

## Comments

- **2026-10-24 · agent** — Graded the ticket-43 checklist against the
  acceptance criteria; three criteria were open or half-met (readiness as
  its own question, the metric's stated question, sensitive data in the
  metrics) and each became or reshaped an item, with the new ones checked
  against the resolved lab code. Doc edit is the resolution; no code
  changed.
