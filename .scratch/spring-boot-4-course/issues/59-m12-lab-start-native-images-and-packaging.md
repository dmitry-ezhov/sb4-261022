# 59: M12 lab start: Native images and packaging

**What to build:** The Start module for M12: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers packaging the application, producing a native image, and making the Lab's diagnostics pass in the packaged form rather than only in the exploded one.

**Blocked by:** 58

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Answer

`labs/lab-12-native-images-and-packaging-start` is the M12 lab's Start
module, added to `labs/pom.xml` after `lab-11-resilience-and-concurrency-solution`
(with the stale `lab-12-capstone-*` row 12 in `labs/README.md` corrected in
the same change). It is the resilience lab's application carried forward:
every main source, the migrations, the `application.yml` and every test
class is the lab-11 solution's byte for byte, with the M11 diagnostics
tests deleted and one deliberate replacement - the diagnostics package
holds one new class.

Because an M12 diagnostic must boot the **packaged** jar - the form is the
subject, and the archived forms only exist after the `package` phase -
the diagnostics are failsafe ITs, not surefire tests:
`DeploymentFormsDiagnosticsIT` (in `com.example.library.diagnostics`)
stages the packaged jar from `target/` and runs it as the deployment page
serves it. That inverted its relationship with the default build, so the
parent POM grew one line of convention: failsafe's default build now
excludes `**/diagnostics/*IT.java` exactly as surefire's has always
excluded `**/diagnostics/*Test.java`, and both diagnostics profiles lift
that exclusion - narrowed to the diagnostics package with
`<include>**/diagnostics/*IT.java</include>` (the surefire mirrors the
shape already there), plus `failIfNoSpecifiedTests=false` because lab 12
has no surefire diagnostics while lab 11 has no diagnostics ITs. The
`diagnostics-solutions` profile joins failsafe into that run the way the
surefire half always worked: its `skipTests` rides the per-module
`<diagnostics.skip>` switch a Start module sets `true`. A scratch
prototype validated the whole chain before landing (process-aot bound →
package → the AOT jar boots under the switch naming its playback; extract
→ training run exits 0 with the cache written → the cached run under the
switch serves the API), and the scratch module was deleted after the real
module was cut from lab-11's copy.

The three diagnostics state the Lab's four deployment forms as three reds
whose every message names the missing move - one `<execution>` of the
`process-aot` goal on the module's spring-boot-maven-plugin:

1. `thePackagedJarServesTheApiBothPlainlyAndUnderItsAotSwitch` - the jar
   boots plainly and answers `GET /api/titles` `200`; under
   `-Dspring.aot.enabled=true` the boot dies with Boot's own failure
   analyzer spelling ("AOT initializer ... could not be found"), and the
   log assert `contains("Starting AOT-processed")` additionally refuses
   the switch that boots without honouring anything.
2. `theExtractedFormServesTheApiBothPlainlyAndUnderItsAotSwitch` - the
   same for the extracted form, staged by the packaging page's own
   `java -Djarmode=tools -jar extract` (the `tools` verb, not Boot 2's
   `layertools`), its layout asserted (`my-app.jar` beside `lib/`), booted
   from the exploded structure the page recommends for production.
3. `theAotCacheTrainedOnTheExtractedFormServesTheApiUnderItsAotSwitch` -
   the two commands of the packaging page's AOT-cache pair as one arc: a
   training run on the extracted form with the 7.0.9/6.1-era
   `-Dspring.context.exit=onRefresh` (the context ends itself at refresh,
   the JDK assembles `app.aot` on the way out - verified working on
   Temurin 25/macOS), then the run that reads it
   (`-XX:AOTCache=app.aot`) combined with the AOT switch per the page's
   "AOT cache and Spring's AOT can be combined" sentence. The native
   image itself is deliberately not exercised: the diagnostics run on the
   JDK the build itself runs, no GraalVM required (noted in the class
   javadoc, with `native:compile-no-fork` named for the reader).

Three notes the diagnostics earned the hard way, each worth a permanent
note for the Solution's author:

- The `extract` verb refuses a destination that already holds anything, so
  a diagnostic that redirects its extract log *into* the destination never
  extracts at all - the log lives in its own scratch directory. (Caught
  red-on-first-run, not by review.)
- Every scratch path under `target/m12-deployment-forms/` is absolute:
  the training JVM resolves `-XX:AOTCacheOutput` against the child
  process's own working directory, and a relative path silently became an
  unresolvable one three levels down.
- `spring.context.exit=onRefresh` is the packaging page's *cache-training*
  switch, not a diagnostic assertion: the training run is expected to exit
  0 with the cache written, and the diagnostic asserts the artifact, not
  the exit.

**Verification:** `./mvnw -f labs
-pl lab-12-native-images-and-packaging-start verify` - **BUILD SUCCESS**, 29
unit tests + `PackagedApplicationIT` green, Spotless/PMD/SpotBugs/
Checkstyle clean, JaCoCo skipped as the Start-module contract allows.
`./mvnw -f labs -pl lab-12-native-images-and-packaging-start verify
-Pdiagnostics` - red exactly 3/3, each message naming the unbound
`process-aot` goal and quoting the boot log whose failure analyzer names
the missing initializer. `./mvnw -f labs verify` - the full default
reactor green across all 23 modules. Spot-checks with the new parent-POM
wiring: lab-01's `-Pdiagnostics` run still fails loudly in its own
diagnostics package, and lab-11's solution is green under
`-Pdiagnostics-solutions`.

## Comments

- **2026-10-24 · agent** — Implemented per the M12 doc's Lab section and
  the labs authoring conventions: sources carried forward byte for byte
  from the lab-11 solution, one diagnostics IT under
  `src/test/java/.../diagnostics/`, module added to the aggregator after
  `lab-11-resilience-and-concurrency-solution`, the parent POM gaining the
  failsafe mirror of the surefire diagnostics convention, the Solution
  module deliberately absent (ticket 60). Reactor runs: default build
  green on the module and the whole labs reactor; `-Pdiagnostics` red 3/3
  with each failure naming the missing `process-aot` bind; the full
  solution-side chain (AOT generation, extraction, cache training, the
  combined cached+AOT run) was proven green on a scratch module before
  the diagnostics were pinned red.
- **2026-10-24 · agent** — Review fixes (code-review, two axes, HEAD
  base): the build itself caught two diagnostics-infrastructure bugs red
  on the first `-Pdiagnostics` run - the extract log living *inside* the
  extract destination (the verb refuses a non-empty destination) and
  relative scratch paths the training JVM resolves against the child
  process's working directory - and the Spec axis caught two more: a
  literal `%n` surviving in the log-tail composition (it was being passed
  as a printf argument, so it printed verbatim) and the diagnostics
  profiles running every IT beside the diagnostics, now narrowed to the
  diagnostics package. The Standards axis flagged the repeated failsafe
  block across the two diagnostics POM profiles and the always-`"java"`
  JDK binary helper; both left as judgement calls in house style. All
  edits re-validated: default build green, `-Pdiagnostics` red 3/3,
  lab-01 still red, lab-11 solution still green under
  `diagnostics-solutions`.
