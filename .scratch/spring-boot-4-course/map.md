# Map — Spring Boot 4 course

The implementation map for the feature described in
[`spec.md`](spec.md). One child file per ticket, under
[`issues/`](issues/), numbered from `01`, blockers first.

## Notes

- The course's build lives in the root parent POM; every Module inherits it.
  Nothing in a Lab Module configures a plugin.
- The two seams are the reactor build (`./mvnw -f labs verify`) and the
  diagnostics profile (`-Pdiagnostics`). Every ticket is verified against
  those two commands and nothing else.
- The domain vocabulary is fixed in [`GLOSSARY.md`](../../../GLOSSARY.md):
  Title, Copy, Member, Loan, Reservation. A fixture may not mention the
  domain, because every Lab carries its own copy of the domain classes.
- Each Module is delivered as five tickets - doc, lab start, lab solution,
  quiz, checklist - and the chain is strictly linear, so the frontier is
  always a single ticket.

## Decisions so far

- **2026-10-22 · ticket 01** — The authoring guide fixes one structure for
  every Module: the doc skeleton with its seven sections, the delta table
  rules, the quiz format and the checklist format. Shared fixtures live in a
  separate module that publishes a test-jar, because a fixture cannot
  mention the domain. [Answer](issues/01-module-authoring-template-and-shared-test-support.md)

## Fog

- Whether the M08 lab's due-date tests will want a mutable clock rather than a
  fixed one. If they do, the fixture grows a second factory; the guide
  already lists what the module holds.
