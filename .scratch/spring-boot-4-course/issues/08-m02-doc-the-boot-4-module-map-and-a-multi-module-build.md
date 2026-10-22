# 08: M02 doc: The Boot 4 module map and a multi-module build

**What to build:** The Module doc for M02: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers which starter provides which responsibility, the difference between a parent POM and an aggregator, managed versus inherited dependencies, BOM imports, and how a Boot application is packaged, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 07

**Status:** resolved

- [ ] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering the starter map, and the parent versus aggregator split
- [ ] every claim in the delta table is checked against the version in use, not remembered
- [ ] the terms it introduces are the glossary's terms, and are linked
- [ ] it names the Lab, and the command that runs the Lab's diagnostics
- [ ] every snippet in it compiles against the version in use
- [ ] it states what the reader is assumed to know and what they are not

## Answer

Built in commit `cd32a1e`.

- **The Module doc** `modules/02-module-map-and-build.md` follows the
  authoring guide's skeleton: what the reader knew, the Boot 2 to Boot 3 to
  Boot 4 delta table, how it works now, the Lab, the quiz, the checklist.
- **The starter map** in "How it works now" is a table of five starters and
  what each actually pulls in, read off the published POMs of Boot 4.1.1:
  `spring-boot-starter-webmvc`, `spring-boot-starter-jackson`,
  `spring-boot-starter-webmvc-test`, `spring-boot-starter-restclient` and
  `spring-boot-starter-data-jpa`.
- **Parent, aggregator and BOM import** are set side by side in one
  comparison table, because they are three different mechanisms that are
  constantly confused with each other: inheritance, the module list, and
  version management respectively. The course's own repository is the worked
  example - the root POM is parent *and* aggregator, `labs/pom.xml` is an
  aggregator that configures nothing.
- **Managed versus inherited** is stated as the rule it is: managed means the
  version is known, inherited means the dependency is on your classpath. The
  parent inherits exactly two dependencies to every child, and a child that
  re-declares them is duplicating a decision.
- **Packaging** is explained from where it comes from: the `repackage` goal is
  bound in `spring-boot-starter-parent`'s plugin management, so a module that
  inherits it produces an executable jar without declaring a plugin.
- **Every claim was checked against the versions in use**, and two of them
  changed what the doc says:
  - the JSON starter was *renamed* in Boot 4 (`spring-boot-starter-json` ->
    `spring-boot-starter-jackson`), not deprecated;
  - `spring-boot-starter-restclient` does not exist in Boot 3.5 at all, so
    the declarative-client starter is new in Boot 4.
  The executable jar's manifest, and the launcher package that Boot 4 moved
  from `org.springframework.boot.loader` to
  `org.springframework.boot.loader.launch`, were read off a jar this
  repository builds.
- **The Lab** is described with its four Diagnostics - no pinned dependency
  version, no pinned plugin version, nothing re-declared that the parent
  already provides, and the Boot 4 starter names. M02 is the one Module whose
  subject is the build rather than the application, so its Diagnostics read
  the module's POM; the doc says so, so the learner knows what to expect.
