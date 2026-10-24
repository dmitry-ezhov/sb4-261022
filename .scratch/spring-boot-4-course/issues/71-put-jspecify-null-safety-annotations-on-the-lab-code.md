# 71: Put JSpecify null-safety annotations on the lab code

**What to build:** The specification says "the code uses JSpecify
null-safety annotations throughout" and M01 teaches the learner that "the
course code is too" — the evidence is in ticket 69's Findings section. This
sweep makes that claim a fact a learner can see in the editor.

**Blocked by:** None

**Status:** ready-for-agent

- [ ] the lab sources (`src/main` of every Start and Solution module) use
  JSpecify annotations — `@NullMarked` at the package seam or `@NonNull` /
  `@Nullable` where nullability is a real choice — so the M01 claim becomes a
  fact a learner can see in the editor
- [ ] `labs-support`'s test sources get the same treatment, or the claim is
  scoped in M01 to the application modules
- [ ] M01's wording still holds after the sweep (it says the annotations make
  "the nullability of a framework method visible in the editor" — the sweep
  should keep that story true, not fight it)
- [ ] both seams re-run green after the sweep

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
