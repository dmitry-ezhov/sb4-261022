# 70: Wire the Checkstyle gate and curate its ruleset

**What to build:** The Checkstyle gate the specification names, running on every
module and failing the build when it is not satisfied. Today the plugin is
declared, configured and activated in the parent POM, but it has no
`<executions>`, so no goal is bound to a phase and `verify` never runs it.
Run by hand it reports 21 violations in the course's own code, so wiring it
as it stands would turn the build red.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] the plugin binds `check` to the `verify` phase in the parent POM, next to the PMD and SpotBugs executions
- [x] the curated ruleset drops the rules that overlap with Spotless, as its own header comment says it must
- [x] `HideUtilityClassConstructor` is excluded for the `@SpringBootApplication` class, with a comment saying why
- [x] every module is green with the gate enabled, and the `quality-lenient` profile still softens it
- [x] the README, `labs/README.md` and `AGENTS.md` describe the gate that actually runs

## Answer

Found by the 2026-10-22 acceptance audit (ticket 69).

**The gate now runs.** The `pluginManagement` entry carries an execution that
binds `check` to `verify`, next to the PMD and SpotBugs executions:

```xml
<executions>
  <execution>
    <id>checkstyle-check</id>
    <phase>verify</phase>
    <goals>
      <goal>check</goal>
    </goals>
  </execution>
</executions>
```

The `check` goal's own default phase is `verify`, which is why the missing
execution was easy to miss: the goal *would* have run in the right phase if
it had been bound at all. A plugin declared in `<plugins>` without an
execution gets no lifecycle binding, so `maven-checkstyle-plugin:3.6.0:check`
never appeared in the build log.

**The test sources are linted too.** The parent POM set
`checkstyle.includeTestSourceDirectory=true` as a property, and the property
did nothing: the `check` goal exposes no user property for that setting, so
only main sources were scanned. The setting now lives in the plugin's
`<configuration>`, where the goal actually reads it - which is what the
specification's rule about user properties asks for: the properties are used
where the plugin exposes them, and the plugin block is used where it does
not. `failsOnError` moved with it, for the same reason.

**The ruleset is curated, and says what it dropped.** Five rules went, all of
them things Palantir decides: `WhitespaceAround`, `WhitespaceAfter`,
`LeftCurly`, `RightCurly` and `NewlineAtEndOfFile`. The header now lists them
and says why - a rule that disagrees with the formatter is a rule a learner
cannot satisfy, and the two gates would then argue about the same line. The
empty record body was the case that made the point: Palantir writes
`record Loan() {}`, and `WhitespaceAround` reported all twenty of them, and
the formatter was right.

A sixth went with them, `FileTabCharacter`, one step further on the same
principle: Palantir rewrites the indentation of every line it owns, so the
rule could only ever fire inside a string literal or a text block - where a
tab is legal Java and the formatter will not touch it. A rule that can only
report something the learner cannot change is a false positive waiting to
happen, and the specification asks for rules "that catch real defects
without drowning in false positives". It was the one drop beyond the five
the ticket named, and it is one line to reverse.

Two rules stayed, with a comment saying why: the import rules, because
Spotless removes unused imports and orders the rest but neither forbids a
star import nor removes a redundant one; and `LineLength`, because Palantir
wraps at 120 columns but cannot split a long string literal.

**The application class is exempt by annotation.** `HideUtilityClassConstructor`
is right about utility classes and wrong about an application class, which
Spring Boot instantiates and which carries the `main` method. The module now
sets `ignoreAnnotatedBy` to `SpringBootApplication` and to its fully
qualified form - the rule matches the name as the source writes it, and a
later Lab may well rename the class. The exemption is the annotation, not the
file name.

**Verified**:

- `./mvnw -f labs clean verify` - green, with `checkstyle:check` running on
  all eleven modules, and no new warnings.
- A deliberate violation - a class with only a static method - fails the
  build and names the gate: `Failed to execute goal
  org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0:check
  (checkstyle-check) ... 1 error reported by Checkstyle 14.3.0`.
- `./mvnw -f labs -pl <module> verify -Pquality-lenient` - green with the
  same deliberate violation in place, so the escape hatch still works.
- `./mvnw -f labs verify -Pdiagnostics-solutions` - green; a Start module's
  `-Pdiagnostics` run is still red with its five named failures.

**Docs**: the README, `labs/README.md` and `AGENTS.md` already described the
gate this ticket asked for - they were describing a gate that did not run.
`labs/README.md` now also says that the ruleset lints the test sources, and
`tasks/T-02` records the correction, because it had ticked "every gate runs
on every module" while this one was dormant.
