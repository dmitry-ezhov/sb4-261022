# 09: M02 lab start: The Boot 4 module map and a multi-module build

**What to build:** The Start module for M02: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers splitting the catalogue application into a parent and a child module, moving the shared code across, and keeping the whole build green.

**Blocked by:** 08

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Answer

Built in commit `f487e84`.

- **The Start module** is the application M01 left - same code, same
  behaviour, same tests - with a build written the way a Boot 2 build was
  written: every dependency pins a literal version, the plugin pins its
  version, the test starter and the null-safety annotations are declared
  again although the parent already gives them to every child, and the web
  starter is the deprecated `spring-boot-starter-web`.
- **Five diagnostics** read the module's own POM and name what has to
  change: "no dependency may pin a version - the parent's dependencyManagement
  owns the versions ==> Expecting empty but was: [Entry[artifactId=
  spring-boot-starter-web, version=4.1.1], ...]".
- **The POM is read as text, not parsed.** XML comments are stripped first,
  so a comment that talks about a version cannot be mistaken for one. The
  JDK's XML parser was avoided on purpose: it lives in `javax.xml`, and this
  course spends a Module telling you that a `javax.*` import is a smell.
  `${project.version}` is a reference, not a pin, and is allowed.
- **The application gains one thing**: an integration test that inspects the
  packaged jar - the launcher in the manifest, the classes under
  `BOOT-INF/classes`, the dependencies under `BOOT-INF/lib`. A Module about
  the build has to test the build's output. It runs in the default build and
  is green in both halves.
- **Verified**: the default build is green; the diagnostics run is red with
  five named failures.
