# AGENTS.md

Instructions for AI agents working in this repository.

## Repository

A self-paced Spring Boot 4 course: module docs in `modules/`, lab modules in
`labs/`, the course backlog in `tasks/`, and shared static-analysis
configuration in `config/`.

The **root `pom.xml` is the parent and the aggregator** and owns every build
decision — dependency versions, plugin versions, quality gates. `labs/pom.xml`
configures nothing; it only lists the lab modules. Change build configuration
in the root POM only, so every module follows.

Conventions that are not obvious from the code:

- A lab is a **pair** of modules: `lab-NN-<slug>-start` and `-solution`. A start
  module compiles and passes its own tests; the behaviour the lab asks for is
  missing on purpose and is described by **diagnostic tests** in
  `src/test/java/.../diagnostics/`, which the default build excludes.
- `./mvnw -f labs verify` runs every gate (Spotless/Palantir, PMD, SpotBugs,
  Checkstyle, JaCoCo). `./mvnw -f labs verify -Pdiagnostics` runs the
  diagnostics instead, and `-Pquality-lenient` downgrades the gates to
  warnings.
- Packages are feature-first: `com.example.library.<feature>`.
- Use the vocabulary in `GLOSSARY.md` in code, tests and prose. Do not
  introduce synonyms for `Title`, `Copy`, `Member`, `Loan` or `Reservation`.
- Version changes land in the root POM alone, and `labs/README.md` is updated in
  the same commit.

## Agent skills

### Issue tracker

Issues and specs are markdown files under `.scratch/<feature-slug>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical roles, used verbatim. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: root `GLOSSARY.md` and `docs/adr/`. See `docs/agents/domain.md`.
