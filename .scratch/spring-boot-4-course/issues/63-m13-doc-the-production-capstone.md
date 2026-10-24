# 63: M13 doc: The production capstone

**What to build:** The Module doc for M13: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers pulling the earlier Modules together - persistence, security, observability, resilience - into one deployable application, and the operational checklist that goes with it, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 62

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering the whole application, assembled and hardened
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

The Module doc (`modules/13-the-production-capstone.md`, through "The Lab"
as this ticket scoped it - the quiz and checklist are tickets 66 and 67)
follows the authoring guide's skeleton and the M09-M12 voice. Its eight
delta rows are capstone rows: half of each row's 4.1.1 cell is a fresh
read against 4.1.1, the other half points back at the Module whose jar
read decided it, so the assembled table does not re-derive what the
twelve earlier tables proved.

- **The preamble separates provenance, per the summary's framing.** The
  doc names this Module's own fresh reads in one paragraph
  (`spring-boot-health`'s `AvailabilityProbesAutoConfiguration` by
  `javap -v` on the annotation's `matchIfMissing`; the
  `spring-configuration-metadata.json` of `spring-boot-web-server` for
  `server.shutdown`'s default `graceful`, of `spring-boot-webmvc` for
  `problemdetails`' default-false, of `spring-boot-health` for
  `show-details`; `spring-boot-http-client` for the plural spellings
  live / singular deprecation-tagged `since: 4.0.0` with replacements
  named; the actuator starter's five-dependency POM; the formatter class
  listing inside the core jar for the three structured-logging ids) and
  says cells carried forward keep their originating Modules' provenance.
- **The one behaviour the capstone adds is the Lab's gap, and it is a
  taught-but-never-built one.** M09 taught
  `AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC)`
  and no lab ever asked the application to do it - so the Lab's
  behavioural gap is the readiness publish when the availability service
  stops answering, with the state moving back on recovery. The doc names
  this explicitly as the one promised state the twelve Modules taught and
  no module built.
- **The one Java snippet is compile-proof followed to the classpath.**
  The `AvailabilityChangeEvent.publish` snippet was compiled with
  `javac` against the lab-12-solution runtime classpath (the
  production compile surface this course actually resolves):
  COMPILED_OK, unchanged. Every shell/YAML snippet is a command or
  config from the verified artifacts and lab files.
- **The Lab is named with its diagnostics command.**
  `lab-13-the-production-capstone-start`/`-solution`, and
  `./mvnw -f labs -pl lab-13-the-production-capstone-start verify
  -Pdiagnostics` (with the `-Pdiagnostics-solutions` CI form) - the Lab
  index's row 13 is left to the lab-authoring tickets (64/65), per ticket
  60's decision that the row belongs to the ticket that creates the
  modules.
- **Assumed / not assumed is stated in the What-you-knew section** in
  the standing pattern: the reader brings the twelve Modules' mechanism
  knowledge (the second lock, the retry geometry, the drain window,
  probe semantics) and is not assumed to know which flips happened in
  Boot 4 versus one generation earlier, or that the defaults arrive
  configured on 4.1.1 before any line the reader writes.
- **The scope boundary is drawn in the intro and again in a closing
  section**: orchestrator, load balancer, database cluster, alerting -
  deployment infrastructure, out of scope, the sibling course's word,
  with the *depth* fence (native-image internals, chaos) repeated.
- **Glossary discipline**: only the five canon words are used, linked
  to `../GLOSSARY.md` at every use; no fourth new vocabulary is
  introduced; the doc never asserts a total course count (the
  glossary's own "twelve units" correction belongs to ticket 68's index
  pass).
