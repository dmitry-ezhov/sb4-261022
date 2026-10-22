# 22: M04 checklist: The web stack: MVC, Jackson 3 and problem details

**What to build:** The checklist for M04: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 21

**Status:** resolved

- [ ] the checklist names observable properties, not implementation steps
- [ ] every item is verifiable by reading the change or running a command
- [ ] it covers payloads are typed, not maps
- [ ] it covers failures are problem details with the correct status
- [ ] it covers no Jackson 2 type leaks into application code
- [ ] it covers the outbound call has a timeout
- [ ] no item restates a Quality gate that the build already enforces

## Answer

Built in commit `2e9dcc2` (together with the quiz, ticket 21).

Eight observable properties: every failure is a problem detail with the
right media type, status and title; the failures are the application's own
domain exceptions rather than caught `Exception`s; the payloads are typed
records; the JSON is Jackson 3's; the outbound call is an `@HttpExchange`
interface; the service's address is configuration; the vocabulary is the
glossary's; and the service is stubbed with a real server so the wiring is
exercised rather than assumed.

The last two are the ones a build cannot check.
