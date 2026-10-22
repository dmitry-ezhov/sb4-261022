# 10: M02 lab solution: The Boot 4 module map and a multi-module build

**What to build:** The Solution module for M02: the reference implementation of splitting the catalogue application into a parent and a child module, moving the shared code across, and keeping the whole build green, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 09

**Status:** resolved

- [ ] the Solution module exists and is listed in the labs aggregator
- [ ] it is written from scratch, not patched from the Start module
- [ ] every diagnostic of the Start module passes against it
- [ ] it meets the coverage floor
- [ ] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [ ] CI asserts its diagnostics through the single diagnostics command

## Answer

Built in commit `d70319d`.

- **The Solution module** names the Boot 4 starters, pins no version
  anywhere, declares nothing the parent already provides, and declares the
  Boot plugin without a version so that the parent's plugin management
  supplies both the version and the `repackage` execution.
- **All five diagnostics pass**, the reactor is green with every gate
  enabled - including the coverage floor - and the CI command
  (`-Pdiagnostics-solutions`) asserts it.
- **The Start module's five diagnostics still fail**, each naming what is
  missing.
- **A claim in the M02 doc was corrected while building this**: the
  `repackage` goal is *managed* by `spring-boot-starter-parent`, never
  activated - in Boot 2.7, Boot 3.5 and Boot 4.1.1 alike, checked against
  the published POMs. A module has to declare the plugin to get an
  executable jar; leave it out and you get a plain jar and "no main manifest
  attribute" from `java -jar`. Confirmed against this repository's own jars:
  the module that declares the plugin produces `BOOT-INF/lib`, the one that
  does not produces a plain jar.
