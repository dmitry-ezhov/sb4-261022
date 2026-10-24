# Spring Boot 4 Course — Implementation Plan

> As delivered, the platform phase is M09–M13, not M09–M12: the native images
> and packaging subject this plan drafted as a single M12 arrives as M12
> (native images and packaging) and M13 (the production capstone). See
> [`docs/adr/0006-thirteen-modules-not-twelve.md`](docs/adr/0006-thirteen-modules-not-twelve.md).
> The doc paths and estimates below are the plan's, predating the authored
> modules; the authoritative index is the README's module table.

## Phase 0 — Environment and scaffold (done)

| Task | Deliverable | Status |
|---|---|---|
| T-00 | Environment check, version verification | done |
| T-01 | Root parent POM + `labs/` aggregator + Maven Wrapper | done |
| T-02 | Quality gates and the curated rulesets | done |
| T-03 | Lab 01 pair, proving the gates run on JDK 25 | done |

## Phase 1 — The bridge (M01–M04)

| Task | Module | Deliverable | Estimate |
|---|---|---|---|
| T-04 | M01 | Doc `modules/01-boot2-to-boot4.md`, lab 01 pair, quiz, checklist | 3–4h |
| T-05 | M02 | Doc `modules/02-module-map-and-build.md`, lab 02 pair | 3–4h |
| T-06 | M03 | Doc `modules/03-typed-configuration.md`, lab 03 pair | 3–4h |
| T-07 | M04 | Doc `modules/04-web-stack-jackson3.md`, lab 04 pair | 4–5h |

### What each module must contain

Every module doc follows the same shape:

1. **What you knew** — the Boot 2 version of the concept, in one paragraph.
2. **What changed** — the Boot 3 change and the Boot 4 change, as a table.
3. **How it works now** — the concept, with the Boot 4 API, at the level of a
   senior developer who needs to reason about it, not memorise it.
4. **The lab** — what to fix, and the diagnostics that will tell you when you
   have.
5. **Quiz** — five questions with answers.
6. **Checklist** — what a reviewer should look for.

### Lab 01 — what it teaches

M01's lab is the course's first contact with the code. The start module is a
catalogue API written the way a Boot 2 application would have written it:

- untyped `Map<String, Object>` payloads instead of records;
- a 404 with no body instead of an RFC 9457 problem detail;
- `ResponseEntity.notFound().build()` instead of a domain exception.

The diagnostics ask for a JSON array of typed records, a JSON array of copies,
and a 404 served as `application/problem+json`. The solution introduces a
`TitleNotFoundException` and a `@RestControllerAdvice` that returns a
`ProblemDetail`.

## Phase 2 — Data and testing (M05–M08)

| Task | Module | Deliverable | Estimate |
|---|---|---|---|
| T-08 | M05 | Doc `modules/05-persistence.md`, lab 05 pair (H2 + optional Postgres) | 5–6h |
| T-09 | M06 | Doc `modules/06-validation-and-errors.md`, lab 06 pair | 4–5h |
| T-10 | M07 | Doc `modules/07-testing.md`, lab 07 pair (slices, Testcontainers) | 5–6h |
| T-11 | M08 | Doc `modules/08-transactions.md`, lab 08 pair (Postgres optional) | 5–6h |

Notes:

- M05 is where the in-memory `TitleRepository` is replaced by a real repository.
- M05 also introduces `labs-support` — a shared module that publishes a
  `test-jar` with the shared fixtures (a fixed `Clock` for due dates).
- M07 is where `spring-boot-starter-webmvc-test` and the slice-test strategy
  become explicit, and where `@MockitoBean` replaces the `@MockBean` that Boot
  3 removed.
- M08 covers `@Transactional` propagation, isolation, and optimistic locking.
  It needs a real database to be meaningful, so it ships with a Testcontainers
  profile and an H2 fallback that is explicitly less realistic.

## Phase 3 — The platform (M09–M12)

| Task | Module | Deliverable | Estimate |
|---|---|---|---|
| T-12 | M09 | Doc `modules/09-observability.md`, lab 09 pair (Actuator, Micrometer, OTLP) | 4–5h |
| T-13 | M10 | Doc `modules/10-security7.md`, lab 10 pair (Security 7 filter chains) | 5–6h |
| T-14 | M11 | Doc `modules/11-resilience-and-concurrency.md`, lab 11 pair (virtual threads, resilience) | 5–6h |
| T-15 | M12 | Doc `modules/12-native-and-production.md`, lab 12 pair (native image, capstone) | 6–8h |

Notes:

- M10 and M11 overlap with the `sb4-robust-and-secure` course. This course
  teaches the mechanism and the Boot 4 API; that course teaches depth. The
  module docs say so explicitly and link across.
- M12 is the capstone: the whole application, packaged, hardened and observed.

## Phase 4 — Polish

| Task | Deliverable |
|---|---|
| T-16 | Course index page, per-track reading lists, the final README pass |
| T-17 | A final verified-versions pass against a clean build |
| T-18 | The closing audit: every acceptance criterion in the spec, checked |

## Ordering rules

1. A module's lab module is added to `labs/pom.xml` by the task that authors
   it — never pre-created.
2. A module doc is not merged before its lab pair builds green on JDK 25.
3. The CI workflow must stay green at every commit.
4. Any version change lands in the root POM alone, and `labs/README.md` is
   updated in the same commit.
