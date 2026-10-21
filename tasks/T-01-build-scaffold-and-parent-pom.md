# T-01 — Build scaffold and root parent POM

- **Phase:** P0 — Environment & Foundations
- **Status:** done
- **Depends on:** T-00
- **Artifacts:** `pom.xml`, `labs/pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/`
- **Spec refs:** §6.1, §6.2, §6.3

## Goal

Create the build that every later task builds on: a root parent POM that owns
all configuration, an aggregator for the labs, and a Maven wrapper so that no
learner needs Maven installed.

## Context

The parent must do three things well:

1. **Own the configuration.** Every child inherits it; no child repeats it.
2. **Stay out of the way.** A child module should be able to declare its
   dependencies and its application class and nothing else.
3. **Be honest about the toolchain.** The Enforcer fails the build on the wrong
   JDK or Maven, rather than producing a confusing error later.

### Decisions taken here

- The root POM is the parent **and** the aggregator; `labs/pom.xml` is an
  aggregator that configures nothing (see
  [ADR-0001](../docs/adr/0001-root-pom-owns-all-build-configuration.md)).
- The parent inherits `spring-boot-starter-test` (test) and `jspecify`
  (compile) to every module, and manages — but does not inherit — the whole
  rest of the catalogue.
- `maven.multiModuleProjectDirectory` anchors `config/` so that the shared
  rulesets resolve identically from the root, from `labs/`, and from inside a
  lab module.

## Deliverables

1. **`pom.xml`** — parent (`spring-boot-starter-parent:4.1.1`), aggregator,
   properties for every pinned version, the dependency catalogue, the
   `pluginManagement` block with the full configuration of every plugin, the
   `<plugins>` block that activates the always-on gates, and the three
   profiles (`quality-lenient`, `diagnostics`, `sonar`).
2. **`labs/pom.xml`** — aggregator, lists the lab modules, configures nothing.
3. **Maven Wrapper** — `mvn -N wrapper:wrapper -Dmaven=3.10.0 -Dtype=script`.

## Acceptance criteria

- [x] `./mvnw -f labs validate` succeeds.
- [x] The root POM contains no course content — only configuration.
- [x] A child module that declares nothing but its dependencies inherits every
      gate.
- [x] The Enforcer fails on a JDK older than 25.

## Verification

```bash
./mvnw -f labs validate
./mvnw -f labs help:evaluate -Dexpression=maven.multiModuleProjectDirectory -q -DforceStdout
```
