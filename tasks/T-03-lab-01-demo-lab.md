# T-03 — Lab 01: the demo pair

- **Phase:** P0 — Environment & Foundations
- **Status:** done
- **Depends on:** T-01, T-02
- **Artifacts:** `labs/lab-01-boot2-to-boot4-start/`, `labs/lab-01-boot2-to-boot4-solution/`
- **Spec refs:** §7, acceptance criterion 3 and 4

## Goal

Ship one complete lab pair so that the whole course mechanic — start module,
diagnostics, solution module — is proven before twelve modules depend on it.

## Context

The lab teaches the Boot 2 → Boot 4 delta in the smallest possible code: a
catalogue API for the library domain.

**Start module** — written the way a Boot 2 application would have written it:

- `Map<String, Object>` payloads instead of records;
- a 404 with an empty body instead of an RFC 9457 problem detail;
- `ResponseEntity.notFound().build()` instead of a domain exception.

**Solution module** — the same API in Boot 4 shape:

- `Title` and `Copy` records, returned as JSON arrays;
- `TitleNotFoundException` and an `@RestControllerAdvice` that returns a
  `ProblemDetail` served as `application/problem+json`.

**Diagnostics** — three tests in
`src/test/java/com/example/library/diagnostics/Boot4ConventionsDiagnosticsTest.java`,
excluded from the default build by the parent POM's surefire configuration.

## Deliverables

1. **`labs/lab-01-boot2-to-boot4-start/`** — the defective API, its tests and
   its diagnostics. Sets `<jacoco.skip>true</jacoco.skip>`; every other gate
   applies to it.
2. **`labs/lab-01-boot2-to-boot4-solution/`** — the reference implementation,
   with the same diagnostics, all green.
3. **Both POMs** — declare `spring-boot-starter-webmvc` and
   `spring-boot-starter-webmvc-test`, and nothing else. The starter-test
   dependency comes from the parent.

## Acceptance criteria

- [x] `./mvnw -f labs verify` is green for the whole reactor.
- [x] `./mvnw -f labs -pl lab-01-boot2-to-boot4-start verify -Pdiagnostics` is
      red, with three failures that name what is missing.
- [x] `./mvnw -f labs -pl lab-01-boot2-to-boot4-solution verify -Pdiagnostics`
      is green.
- [x] The solution module meets the coverage floor; the start module opts out.

## Verification

```bash
./mvnw -f labs verify
./mvnw -f labs -pl lab-01-boot2-to-boot4-start verify -Pdiagnostics   # fails
./mvnw -f labs -pl lab-01-boot2-to-boot4-solution verify -Pdiagnostics # passes
```

Observed on 2026-10-22:

```
start:    Tests run: 3, Failures: 3  (Content type not set / No value at JSON path "$[0].name")
solution: Tests run: 3, Failures: 0
```
