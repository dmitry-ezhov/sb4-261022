# T-00 — Environment check and version verification

- **Phase:** P0 — Environment & Foundations
- **Status:** done
- **Depends on:** —
- **Artifacts:** `mise.toml`, `labs/README.md` (verified versions table)
- **Spec refs:** §6.2

## Goal

Establish exactly which versions the course builds against, so that no version
in the repository is a guess.

## Context

The versions have to be *verified*, not assumed: a tool that does not support
JDK 25 fails at the worst possible moment, in a learner's lab, on a laptop.

## What was verified (2026-10-22)

| Fact | How it was established |
|---|---|
| Latest stable Spring Boot 4 is 4.1.1 | Maven Central metadata: `4.2.0-M2` is the latest release, but it is a milestone. |
| Spring Framework 7.0.9, Jackson 3.1.5, Testcontainers 2.0.5 | `spring-boot-dependencies:4.1.1` properties. |
| JaCoCo ≥ 0.8.14 supports Java 25 | JaCoCo release notes: 0.8.14 "officially supports Java 25". |
| PMD ≥ 7.16 supports Java 25 | PMD 7.16.0 release notes: "brings support for Java 25". |
| SpotBugs ≥ 4.10.1 works on JDK 25 | SpotBugs changelog: "Fix IllegalArgumentException initializing spotbugs when inside a fat jar on Java 25". |
| Palantir Java Format ≥ 2.80 supports JDK 25 | SLING-12947: "Update to palantir-java-format 2.80.0 to support builds with Java 25". |
| Maven 3.10.0 is the latest 3.x | Maven Central metadata for `org.apache.maven:apache-maven`. |
| `AutoConfigureMockMvc` moved to `org.springframework.boot.webmvc.test.autoconfigure` | The `v4.1.1` source tree of spring-projects/spring-boot. |
| `spring-boot-starter-web` still exists but is deprecated | The starter's POM in Boot 4.1.1. |
| `spring-boot-starter-webmvc-test` is a separate starter | The starter's POM in Boot 4.1.1. |

## Acceptance criteria

- [x] Every version in the build is pinned to a verified release.
- [x] The versions table in `labs/README.md` matches the POMs.
- [x] `mise.toml` pins JDK 25 and Maven 3.10.0.

## Verification

```bash
./mvnw -f labs verify
```
