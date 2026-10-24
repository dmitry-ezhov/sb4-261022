# 71: Put JSpecify null-safety annotations on the lab code

**What to build:** The specification says "the code uses JSpecify
null-safety annotations throughout" and M01 teaches the learner that "the
course code is too" — the evidence is in ticket 69's Findings section. This
sweep makes that claim a fact a learner can see in the editor.

**Blocked by:** None

**Status:** resolved

- [x] the lab sources (`src/main` of every Start and Solution module) use
  JSpecify annotations — `@NullMarked` at the package seam or `@NonNull` /
  `@Nullable` where nullability is a real choice — so the M01 claim becomes a
  fact a learner can see in the editor
- [x] `labs-support`'s test sources get the same treatment, or the claim is
  scoped in M01 to the application modules
- [x] M01's wording still holds after the sweep (it says the annotations make
  "the nullability of a framework method visible in the editor" — the sweep
  should keep that story true, not fight it)
- [x] both seams re-run green after the sweep

## Comments

- **2026-10-25 · found by ticket 69** — the final audit checked the
  specification claim "The code uses JSpecify null-safety annotations
  throughout" against the repository and found no annotation usage anywhere
  (the evidence, including the `ModulePom.java` exception, is recorded in
  ticket 69's Findings). Two remediation shapes were considered: a
  `@NullMarked` marker on the feature packages (cheap, one file per
  package) mirrors how the framework itself is annotated, and per-member
  annotations are only needed where the default is deliberately relaxed.
  Either way this is a sweep across the lab modules and belongs in its own
  ticket, not in the audit.

- **2026-10-25 · resolved** — took the first remediation shape: every package
  under `src/main/java` of all 26 lab modules (both halves of all 13 labs)
  and the `com.example.library.support` test package of `labs-support` now
  carries a `package-info.java` annotated `@NullMarked` — 201 files, one per
  package. No per-member `@NonNull`/`@Nullable` was needed: a grep across all
  lab main sources found no code with a genuinely-null choice (the only null
  comparisons live in the deliberately "Boot 2 way" lab-01-start controller,
  which the default of an unannotated `Map.get` still permits). Two notes for
  future sweeps: JLS requires the annotation *before* the `package`
  declaration and the import *after* it — the obvious-looking layout gets
  "reached end of file while parsing" from Palantir's formatter — and
  Palantir accepts the correct layout untouched. The module-side story of
  ticket 69's audit is now literal: `@NullMarked` makes the nullability of a
  framework method visible in the editor exactly as M01 claims. Both seams
  re-run green after the sweep (`./mvnw -f labs verify` and
  `./mvnw -f labs verify -Pdiagnostics-solutions`).
