# 70: Wire the Checkstyle gate and curate its ruleset

**What to build:** The Checkstyle gate the specification names, running on every
module and failing the build when it is not satisfied. Today the plugin is
declared, configured and activated in the parent POM, but it has no
`<executions>`, so no goal is bound to a phase and `verify` never runs it.
Run by hand it reports 21 violations in the course's own code, so wiring it
as it stands would turn the build red.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] the plugin binds `check` to the `verify` phase in the parent POM, next to the PMD and SpotBugs executions
- [ ] the curated ruleset drops the rules that overlap with Spotless, as its own header comment says it must
- [ ] `HideUtilityClassConstructor` is excluded for the `@SpringBootApplication` class, with a comment saying why
- [ ] every module is green with the gate enabled, and the `quality-lenient` profile still softens it
- [ ] the README, `labs/README.md` and `AGENTS.md` describe the gate that actually runs

## Answer

Not started. Found by the 2026-10-22 acceptance audit (ticket 69).

The specification's *Quality gates* decision says every gate is "configured
once, in the parent, and activated there so that every child inherits it",
and names Checkstyle among the gates. The parent POM activates the plugin at
line 462, but the `pluginManagement` entry has no `<executions>`, and a
plugin declared in `<plugins>` without an execution gets no lifecycle
binding - so `maven-checkstyle-plugin:3.6.0:check` never appears in the
build log.

The curated ruleset needs the same pass PMD's did:

- `WhitespaceAround` and `WhitespaceAfter` overlap with Spotless, and
  Palantir formats an empty record body as `record Loan() {}` - which
  `WhitespaceAround` reports as 20 violations. The ruleset's own header says
  "Spotless (Palantir style) already owns whitespace, indentation and import
  order, so nothing here may overlap with it."
- `HideUtilityClassConstructor` reports the `@SpringBootApplication` class,
  which is an application class, not a utility class.

The two skills the specification asks for are already in place: the rulesets
are curated rather than stock, and each exclusion carries a comment saying
why it is there.
