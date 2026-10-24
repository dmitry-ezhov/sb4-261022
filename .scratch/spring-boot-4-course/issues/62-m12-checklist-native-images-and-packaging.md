# 62: M12 checklist: Native images and packaging

**What to build:** The checklist for M12: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 61

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers the packaged form is tested, not only the exploded one
- [x] it covers the native build is reproducible
- [x] it covers the image carries nothing it does not need
- [x] it covers the build time cost is acknowledged in the doc
- [x] no item restates a Quality gate that the build already enforces

## Answer

The checklist was drafted fresh against the doc (ticket 58) and graded
against the ticket's seven criteria before landing, the way ticket 61
graded its quiz. No draft existed to grade - ticket 58's map entry names
this ticket (62) and the quiz (61) as the doc-pair that owns the
checklist - so the answer text is the checklist's, drafted item by item
and then executed against the resolved lab-12 code, its diagnostics and
its packaged artifacts.

- **The two meta-criteria graded first.** Every item leads on a
  property a reviewer can observe - a boot that answers under the
  switch but names no playback, a diff carrying a removed regime back
  in, an image with cargo the run never reads, a memo arguing from
  startup numbers with the build minutes unnamed - and every "Verified
  by" line is a read (of the POM, the diff, the hint files, the doc) or
  a run (the diagnostics, `list-layers`, a grep, a tree diff). No item
  names an implementation step as its subject: process-aot's binding is
  described for what it freezes and where its output lands, never as
  "the step to perform".
- **The named properties each became or shaped an item.** Packaged
  forms over exploded ones is the first item and its subject: the
  diagnostics are failsafe's, run after package, and each boots its
  form as a process - the jar plainly and under the switch, the
  extracted form the same two ways, the cache trained and served - with
  the switched boots additionally read for the line a reflective boot
  never prints (`Starting AOT-processed`). Native-build reproducibility
  is one item of build arithmetic: the starter-parent's own `native`
  profile, the versionless plugin declaration whose managed pin answers
  it (1.1.8, from 0.10.6 - read off the dependencies POM the doc
  cites), the version-25 distribution floor named where the build is
  taught, and the structural claim that the image is compiled
  whole-program with no half-state - a changed input rebuilds, an
  unchanged one rebuilds the same. Image cargo is the negative-claims
  item: no JVM beside a native executable, no Dockerfile beside the
  buildpacks, test-scoped jars riding no runtime form, and
  `layers.idx` (listed by the tools jarmode's `list-layers` verb) as
  what the Paketo buildpack honours. The build-time cost is the
  penultimate item, anchored to the doc's own cost paragraph as the
  measure.
- **One wording was corrected off the artifact before landing.** The
  hint-file verification was first written to name `reflect-config.json`
  - the spelling the M12 doc teaches, "the exact five files the docs
  enumerate". Reading the actual 4.1.1 repackage contradicts that
  spelling: the jar carries one consolidated
  `reachability-metadata.json` (337 KB) beside a two-line
  `native-image.properties` under
  `META-INF/native-image/{groupId}/{artifactId}/`, and
  `target/spring-aot/main/resources` holds the same two files - the
  consolidation is `process-aot`'s own output, not the repackage's
  work. The item now names the file the artifact carries. The doc's
  five-file sentences (and the quiz answer's `reflect-config.json`
  mention, ticket 61) may want the same artifact read - recorded here
  for their owners rather than fixed in this ticket's scope.
- **Confirmed with runs, both halves of the asymmetry.** The Start
  module's diagnostics run (`./mvnw -f labs -pl
  lab-12-native-images-and-packaging-start -am verify -Pdiagnostics`)
  failed the build on 3 tests, 3 failures - the packaged jar under the
  switch, the extracted form under the switch, and the cached run all
  answered "AOT initializer ... could not be found" in the analyzer's
  own words, each failure message naming the one-move bind. The
  Solution module's run passed 3/3 after one environmental re-run: the
  first attempt's plain extracted boot lost a port race ("Port 64082
  was already in use" - the picked port taken between the probe and
  the bind), not a solution defect. The item claims were also
  executed: `java -Djarmode=tools -jar <solution>.jar list-layers`
  reads exactly `dependencies`, `spring-boot-loader`,
  `snapshot-dependencies`, `application`; the greps for
  `layertools`/`loaderImplementation`/`loader-classic` answer no
  configuration spelling in either module (only prose explaining the
  3.3 rename); `diff -r` of the two modules' `src/main` trees is empty,
  the Solution written from scratch across identical bytes.
- **No item restates a gate.** The gates speak formatting, defects and
  coverage; the items speak boots, bindings, greps, pins, cargo,
  budgets and byte-identity - every verification is a run or a read no
  Spotless, PMD, SpotBugs, Checkstyle or JaCoCo pass delivers, and the
  one gate-adjacent mention (the coverage floor) appears nowhere; the
  domain-identity item rests on the tree diff, not on the floor.

## Comments

- **2026-10-24 · agent** - Drafted the checklist fresh against the doc
  (ticket 58) and graded it against the seven criteria; the
  packaged-forms item became the first and largest, the reproducibility
  and cargo items were built from the delta table's jar reads, and the
  hint-file item was corrected mid-grading to name the consolidated
  `reachability-metadata.json` the actual 4.1.1 jar carries (finding
  recorded in the Answer for tickets 58/61). Diagnostics executed both
  ways - red 3/3 on the Start module, green on the Solution module
  after one port-race re-run - and `list-layers`, the regime greps and
  the `src/main` tree diff all answered as the items claim. Doc edit is
  the resolution; no code changed.
