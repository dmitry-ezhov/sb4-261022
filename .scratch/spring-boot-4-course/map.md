# Map — Spring Boot 4 course

The implementation map for the feature described in
[`spec.md`](spec.md). One child file per ticket, under
[`issues/`](issues/), numbered from `01`, blockers first.

## Notes

- The course's build lives in the root parent POM; every Module inherits it.
  Nothing in a Lab Module configures a plugin.
- The two seams are the reactor build (`./mvnw -f labs verify`) and the
  diagnostics profile (`-Pdiagnostics`). Every ticket is verified against
  those two commands and nothing else.
- The domain vocabulary is fixed in [`GLOSSARY.md`](../../../GLOSSARY.md):
  Title, Copy, Member, Loan, Reservation. A fixture may not mention the
  domain, because every Lab carries its own copy of the domain classes.
- Each Module is delivered as five tickets - doc, lab start, lab solution,
  quiz, checklist - and the chain is strictly linear, so the frontier is
  always a single ticket.

## Decisions so far

- **2026-10-22 · ticket 01** — The authoring guide fixes one structure for
  every Module: the doc skeleton with its seven sections, the delta table
  rules, the quiz format and the checklist format. Shared fixtures live in a
  separate module that publishes a test-jar, because a fixture cannot
  mention the domain. [Answer](issues/01-module-authoring-template-and-shared-test-support.md)
- **2026-10-22 · ticket 02** — Two diagnostics commands, not one:
  `-Pdiagnostics` is the learner's and runs the module you select, red on a
  Start module; `-Pdiagnostics-solutions` is CI's and runs every Solution
  module's diagnostics while skipping the Start modules. A Start module
  declares that with `diagnostics.skip` in its own POM. This refines the
  specification's acceptance criterion 3, which assumed a single command.
  [Answer](issues/02-one-diagnostics-command-that-selects-solution-modules.md)

- **2026-10-22 · ticket 03** — The M01 delta table is checked, not
  remembered: every cell was read off the published POMs and source trees of
  Boot 2.7, 3.0, 3.5 and 4.1.1. Two claims changed as a result - Boot 4
  renamed the JSON starter, and Jackson 3's annotations never moved.
  [Answer](issues/03-m01-doc-boot-2-to-boot-4-what-changed.md)
- **2026-10-22 · tickets 04-05** — The M01 Lab teaches the whole Boot 4
  treatment, so the start module carries all four Boot 2 habits: the hand-
  written validation check, the envelope around a created object, the empty
  error body, and the deprecated starter. Five diagnostics, five named
  failures.
  [Answer](issues/04-m01-lab-start-boot-2-to-boot-4-what-changed.md)
- **2026-10-22 · ticket 02 follow-up** — PMD's `OnlyOneReturn` was curated
  out of the shared ruleset: it is a style rule that fights the guard clause
  and catches no defects.

- **2026-10-22 · ticket 08** — The starter map is read off the published
  POMs, not remembered. Boot 4's starters are per-responsibility, and the
  test starter pulls the whole web stack in at compile scope. Maven's three
  mechanisms - parent, aggregator, BOM import - are taught as three things,
  because they are three things. The M02 Lab is the one Lab whose subject is
  the build, so its Diagnostics read the module's POM.
  [Answer](issues/08-m02-doc-the-boot-4-module-map-and-a-multi-module-build.md)

- **2026-10-22 · tickets 09-10** — M02's Lab is the one Lab whose subject
  is the build, so its Diagnostics read the module's POM rather than an HTTP
  response. The POM is read as text, with XML comments stripped, because the
  JDK's XML parser lives in `javax.xml` and this course teaches that a
  `javax.*` import is a smell. The application also gained an integration
  test that inspects the packaged jar.
- **2026-10-22 · ticket 08 follow-up** — `spring-boot-starter-parent`
  *manages* the `repackage` goal; it never activates it, in Boot 2, Boot 3
  or Boot 4. A module declares the plugin to get an executable jar.
  [Answer](issues/10-m02-lab-solution-the-boot-4-module-map-and-a-multi-module-build.md)

- **2026-10-22 · tickets 11-12** — M02's quiz and checklist close the
  Module. Two claims were checked rather than believed: an imported BOM
  contributes managed versions only (not properties, not plugins), and
  `spring-boot-starter-parent` manages the `repackage` goal without
  activating it.
  [Answer](issues/11-m02-quiz-the-boot-4-module-map-and-a-multi-module-build.md)

- **2026-10-22 · ticket 13** — Configuration changed less than the web
  stack, and the delta table says so: the real change is Boot 3's default
  constructor binding, the Jakarta move in the constraint annotations, and
  Boot 4's `@ConfigurationPropertiesSource`. Two beliefs were corrected by
  checking - `@DefaultValue` is not new, and Boot 4's configuration API
  carries no JSpecify annotations.
  [Answer](issues/13-m03-doc-typed-configuration-and-validation.md)

## Fog

- Whether the M08 lab's due-date tests will want a mutable clock rather than a
  fixed one. If they do, the fixture grows a second factory; the guide
  already lists what the module holds.
