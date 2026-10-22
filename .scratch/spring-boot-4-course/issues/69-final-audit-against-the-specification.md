# 69: Final audit against the specification

**What to build:** Every acceptance criterion in the specification is checked against the repository as it stands, and the verified-versions table is re-confirmed against a clean build, so that the course's claims are the course's facts.

**Blocked by:** 68, 70

**Status:** ready-for-agent

- [ ] every acceptance criterion is checked and the result recorded
- [x] the reactor build is green from a clean checkout
- [x] the diagnostics of every Solution module are green
- [x] the verified-versions table is re-confirmed and dated

## Comments

- **2026-10-22 · partial audit** — Three of the four criteria were checked
  today, against the repository as it stands:
  - `./mvnw -f labs clean verify` is green from a clean tree, with every
    gate the build runs enabled - Spotless, PMD, SpotBugs, JaCoCo and the
    Enforcer.
  - `./mvnw -f labs verify -Pdiagnostics-solutions` is green: every
    Solution module's diagnostics pass, and the Start modules are skipped.
  - The verified-versions table in `labs/README.md` was re-confirmed against
    the resolved classpath and the wrapper: Boot 4.1.1, Framework 7.0.9,
    Jackson 3.1.5 (`tools.jackson`), `jackson-annotations` 2.21,
    Testcontainers 2.0.5, JDK 25.0.4.1 (Temurin), Maven 3.10.0, and every
    plugin version in the table.
- **Still open**: the audit itself - every acceptance criterion in the
  specification checked and the result recorded - cannot be written until the
  Modules exist. M05 onwards are unwritten, and M05 to M13 are the bulk of
  the specification's content.
- **Found during the audit**: the Checkstyle gate is configured but never
  bound to a phase, so `verify` does not run it - and run by hand it reports
  21 violations in the course's own code. Tracked as ticket 70; it should
  be closed before this audit is written.
