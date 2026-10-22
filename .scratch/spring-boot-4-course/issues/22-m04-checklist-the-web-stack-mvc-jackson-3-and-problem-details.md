# 22: M04 checklist: The web stack: MVC, Jackson 3 and problem details

**What to build:** The checklist for M04: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 21

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers payloads are typed, not maps
- [x] it covers failures are problem details with the correct status
- [x] it covers no Jackson 2 type leaks into application code
- [x] it covers the outbound call has a timeout
- [x] no item restates a Quality gate that the build already enforces

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

## Comments

- **2026-10-22 · acceptance audit** — One criterion is still open: the M04
  checklist has no item about the outbound call carrying a timeout. The M04
  solution's `AvailabilityClientConfiguration` sets a base URL and nothing
  else, so the item belongs in the checklist *and* in the Lab's Lab section
  before the criterion can be ticked. The timeout is also the natural bridge
  to M11, which teaches timeouts as a first-class concern.
- **2026-10-22 · reviewer** — Done. The checklist gained
  "**The outbound call gives up in time.**" and the Lab section's summary of
  the Solution now names the timeout alongside the base URL. The new item
  says the call fails fast and loudly, not that the timeout failure is a
  problem detail - the M04 Solution has no handler for a timed-out call, and
  turning that failure into a problem detail is M11's work. The bridge to
  M11 is in the item's own words. The Solution was given the deadline it
  asks for: `spring.http.clients.connect-timeout: 1s` and
  `spring.http.clients.read-timeout: 2s` in its `application.yml` - the
  Boot 4 property root (`spring.http.client.*` is deprecated since 4.0),
  bound into the shared `RestClient.Builder` the client bean builds from.
  Binding was verified in the application context, and the module's build
  with every gate was run green after the change.
