# 20: M04 lab solution: The web stack: MVC, Jackson 3 and problem details

**What to build:** The Solution module for M04: the reference implementation of serving the catalogue as typed records through Jackson 3, returning problem details from a dedicated handler, and calling an outbound service through the declarative client, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 19

**Status:** resolved

- [ ] the Solution module exists and is listed in the labs aggregator
- [ ] it is written from scratch, not patched from the Start module
- [ ] every diagnostic of the Start module passes against it
- [ ] it meets the coverage floor
- [ ] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [ ] CI asserts its diagnostics through the single diagnostics command

## Answer

Built in commit `41dd5f7`.

- **The Solution module** consults the availability service:
  - `AvailabilityClient` is an `@HttpExchange` interface - the URL lives in
    the annotation, the body in the method signature, and the return type is
    the response body;
  - the proxy is built from the interface over a `RestClient` whose base URL
    comes from `library.availability.base-url`, so the service's address is
    configuration rather than code;
  - the service's 4xx becomes a typed exception, which the exception handler
    turns into a problem detail - so "the service says the copy is not on the
    shelf" (409) and "the service does not know the copy" (404) are two
    different failures with two different titles.
- **Both diagnostics pass**, the reactor is green with every gate enabled -
  including the coverage floor - and the CI command asserts it.
- **The Start module's two diagnostics still fail**, each naming what is
  missing.
- **PMD's `ImplicitFunctionalInterface` was curated out** of the shared
  ruleset: a client interface has one method because the service has one
  operation, not because it is meant to be a lambda. The exclusion went into
  the bestpractices category, where the rule actually lives - the first
  attempt put it in codestyle and did not take effect.
