# T-02 — Quality gates and curated rulesets

- **Phase:** P0 — Environment & Foundations
- **Status:** done
- **Depends on:** T-01
- **Artifacts:** `config/pmd/ruleset.xml`, `config/checkstyle/checkstyle.xml`, `config/spotbugs/exclude.xml`, the plugin blocks in `pom.xml`
- **Spec refs:** §6.4, §6.5

## Goal

Wire the five required gates — Spotless (Palantir), PMD, SpotBugs, JaCoCo,
Sonar — plus Checkstyle and the Enforcer, so that they run on every module and
fail the build when they are not satisfied.

## Context

The hard part was not enabling the plugins; it was making them *usable*:

- **Spotless** with Palantir Java Format runs on JDK 25 with no extra JVM
  flags (the formatter uses a shaded compiler). Import order is configured with
  the `\#` group marker so that static imports come first, as Palantir style
  requires.
- **PMD** needs `targetJdk`, `rulesets` and `excludeRoots` on the `pmd` goal,
  but the `check` goal does not accept them. The `pmd` execution is therefore
  bound to phase `none` — it still hands its configuration to the `pmd` goal
  that `check` forks, but it does not run twice.
- **SpotBugs** takes its configuration from user properties
  (`spotbugs.effort`, `spotbugs.threshold`, `spotbugs.excludeFilterFile`), which
  both goals understand, so no goal warns about an unknown parameter.
- **Checkstyle** takes its configuration from `checkstyle.*` properties; only
  `failsOnError` and `includeTestSourceDirectory` have to live in the plugin
  block.
- **JaCoCo** needs an empty `argLine` property as its default, so that
  `@{argLine}` still resolves in modules that skip the agent.

Every exclude in the curated rulesets was checked against PMD 7.17.0: an exclude
that matches no rule is reported by PMD as a warning, so the lists are honest.

## The curated rulesets

- **PMD** — `bestpractices`, `codestyle`, `errorprone`, `design`,
  `multithreading`, `performance`, with the noisy rules excluded
  (`LawOfDemeter`, `UseUtilityClass`, `AtLeastOneConstructor`,
  `LoosePackageCoupling`, `CloseResource`, …). Each exclusion carries a comment
  saying why.
- **Checkstyle** — naming, correctness and structure only. Formatting is
  Spotless's job, so no whitespace rules.
- **SpotBugs** — only what is intentional here: data classes handing out their
  own state, and null-analysis noise around Spring proxies.

## Deliverables

1. **`config/pmd/ruleset.xml`** — the curated PMD ruleset.
2. **`config/checkstyle/checkstyle.xml`** — the curated Checkstyle ruleset.
3. **`config/spotbugs/exclude.xml`** — the SpotBugs exclusion filter.
4. **The plugin blocks in `pom.xml`** — configuration in `pluginManagement`,
   activation in `<plugins>`, the three profiles.

## Acceptance criteria

- [x] Every gate runs on every module.
- [x] The build produces no plugin-parameter warnings.
- [x] PMD reports no unknown-exclude warnings.
- [x] A deliberate violation fails the build and names the gate.
- [x] `-Pquality-lenient` turns every gate into a warning.

## Verification

```bash
./mvnw -f labs verify
./mvnw -f labs verify -Pquality-lenient
```
