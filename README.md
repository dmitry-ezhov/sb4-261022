# Spring Boot 4 Course — Foundations & the Boot 2 → 4 Bridge

A self-paced **text course with code labs** for senior Java developers whose
Spring Boot knowledge has rusted at Boot 2 and never reached Boot 3. The course
is written for the developer who remembers `@Autowired` and `application.yml`,
and who has never seen `jakarta.*`, Jackson 3, modularised starters or Spring
Framework 7.

Everything is built on **Spring Boot 4.1.1** / **Spring Framework 7.0.9** /
**JDK 25**, and every module ends in a lab you can run: a *start module* with the
defects already in place, and a *solution module* with the reference
implementation.

> **Verified 2026-10-22** against Boot 4.1.1, Framework 7.0.9, Jackson 3.1.5,
> JDK 25.0.4, Maven 3.10.0, Testcontainers 2.0.5. See
> [`labs/README.md`](labs/README.md#verified-versions) for the full table.

## Start here

- **New to the repo?** Read [`modules/00-welcome.md`](modules/00-welcome.md) —
  environment setup, repo orientation, and how a lab, its diagnostics and its
  solution relate to each other.
- **Prerequisites:** JDK 25 and a Maven wrapper (both provided). Docker is
  optional and only needed for the Testcontainers labs.

## Tracks

| Track | Modules | Theme |
|---|---|---|
| **A** | M01–M04 | The bridge: what changed between Boot 2, Boot 3 and Boot 4 |
| **B** | M05–M08 | Data, validation, testing and transactions |
| **C** | M09–M13 | The platform: observability, security, resilience, native images and production |

Work through them in order; later modules build on the same library application
that the earlier ones extend.

## Module table

| Module | Title | Doc | Lab (start / solution) | Time | Infra |
|---|---|---|---|---|---|
| M01 | Boot 2 to Boot 4: what changed | [`modules/01-boot2-to-boot4.md`](modules/01-boot2-to-boot4.md) | `labs/lab-01-boot2-to-boot4-start` / `-solution` | ~2.5h | none |
| M02 | The module map and a multi-module build | [`modules/02-module-map-and-build.md`](modules/02-module-map-and-build.md) | `labs/lab-02-module-map-and-build-*` | ~2h | none |
| M03 | Typed configuration and validation | [`modules/03-typed-configuration.md`](modules/03-typed-configuration.md) | `labs/lab-03-typed-configuration-*` | ~2.5h | none |
| M04 | The web stack: MVC, Jackson 3 and problem details | [`modules/04-web-stack-jackson3.md`](modules/04-web-stack-jackson3.md) | `labs/lab-04-web-stack-jackson3-*` | ~3h | none |
| M05 | Persistence: JDBC, JPA and migrations | [`modules/05-persistence.md`](modules/05-persistence.md) | `labs/lab-05-persistence-*` | ~3.5h | Postgres (opt.) |
| M06 | Validation and error handling | [`modules/06-validation-and-errors.md`](modules/06-validation-and-errors.md) | `labs/lab-06-validation-and-errors-*` | ~2.5h | none |
| M07 | The testing strategy: slices, containers and what survives refactoring | [`modules/07-testing-strategy.md`](modules/07-testing-strategy.md) | `labs/lab-07-testing-*` | ~3h | Postgres (opt.) |
| M08 | Transactions and consistency | [`modules/08-transactions-and-consistency.md`](modules/08-transactions-and-consistency.md) | `labs/lab-08-transactions-*` | ~3.5h | Postgres (opt.) |
| M09 | Actuator and observability | [`modules/09-actuator-and-observability.md`](modules/09-actuator-and-observability.md) | `labs/lab-09-observability-*` | ~2.5h | none |
| M10 | Spring Security 7 | [`modules/10-security7.md`](modules/10-security7.md) | `labs/lab-10-security7-*` | ~3.5h | none |
| M11 | Resilience and virtual threads | [`modules/11-resilience-and-virtual-threads.md`](modules/11-resilience-and-virtual-threads.md) | `labs/lab-11-resilience-and-concurrency-*` | ~3h | none |
| M12 | Native images and packaging | [`modules/12-native-images-and-packaging.md`](modules/12-native-images-and-packaging.md) | `labs/lab-12-native-images-and-packaging-*` | ~4h | GraalVM (opt.) |
| M13 | The production capstone | [`modules/13-the-production-capstone.md`](modules/13-the-production-capstone.md) | `labs/lab-13-the-production-capstone-*` | ~3h | Postgres (opt.) |

Total: roughly 38.5 hours of learner time.

## The build

The repository is a Maven multi-module project. The **root `pom.xml` is the
parent and the aggregator** and owns every piece of build configuration; child
modules inherit it and configure nothing but their own dependencies.

```
pom.xml                      parent + aggregator: dependency catalogue, plugin
│                            versions, quality gates
└── labs/pom.xml             aggregator only - lists the lab modules
    ├── lab-01-...-start/    the deliberately imperfect code
    └── lab-01-...-solution/ the reference implementation
```

### Commands

```bash
./mvnw -f labs verify                  # build everything with every gate on
./mvnw -f labs -pl <module> verify -Pdiagnostics  # run that module's diagnostic tests
./mvnw -f labs verify -Pdiagnostics-solutions     # every Solution module's diagnostics
./mvnw -f labs verify -Pquality-lenient  # same checks, warnings instead of failures
./mvnw -f labs -pl lab-01-boot2-to-boot4-solution verify   # one module
./mvnw -f labs spotless:apply          # format the code for you
./mvnw -f labs verify -Psonar -Dsonar.host.url=... -Dsonar.organization=... -Dsonar.token=...
```

### Quality gates

Every gate is configured once, in the root parent POM, and inherited by every
module.

| Gate | Tool | When |
|---|---|---|
| Formatting | Spotless 3.10.4 (Palantir Java Format 2.102.0) | `process-sources` |
| Static analysis | PMD 7.17.0 (curated ruleset) | `verify` |
| Bug patterns | SpotBugs 4.10.5.0 | `verify` |
| Style & correctness | Checkstyle 14.3.0 (curated ruleset) | `verify` |
| Coverage | JaCoCo 0.8.15 (60% line / 50% branch) | `verify` |
| Environment | Maven Enforcer 3.6.3 (JDK 25, Maven 3.9.11+) | `validate` |
| Analysis | Sonar (SonarQube Cloud or self-hosted) | `sonar` profile only |

The curated rulesets live in [`config/`](config/) and are shared by every
module. See [`labs/README.md`](labs/README.md#quality-gates) for what each tool
does and why the rulesets are curated rather than stock.

## Repository layout

```
├── pom.xml                 parent + aggregator (all build configuration)
├── labs/                   the lab modules
├── modules/                module docs, one per module, plus this welcome module
├── config/                 shared PMD / Checkstyle / SpotBugs configuration
├── tasks/                  the task backlog this course is built from
├── docs/adr/               decisions worth remembering
├── GLOSSARY.md             the vocabulary of the course and of the sample app
└── .scratch/               the local issue tracker
```

## Related course

This course covers the foundations. The sibling course `sb4-robust-and-secure`
(in `../sb4-robust-and-secure`) picks up where this one leaves off with
robustness, fault tolerance, resilience and security in depth.
