# 68: Course index and documentation pass

**What to build:** A learner can find any Module from the index, see the tracks, the pacing and the reading lists, and follow any link without hitting a dead end.

**Blocked by:** 67

**Status:** resolved

- [x] the index lists every Module with its track, its time and its lab
- [x] every link between docs resolves
- [x] the pacing table matches the modules that exist
- [x] the reading lists point at the sibling course where the depth belongs there

## Answer

The index pass and the naming decision, both done:

- **The count is settled: thirteen Modules, and it is the specification
  that gave.** Tickets 58–67 are resolved and the repo carries M12
  (native images and packaging) and M13 (the production capstone) as
  separate, fully authored Modules - doc, lab pair, quiz and checklist
  each. Merging them to keep the spec's twelve would trade verified
  content for a number, so M13 stays, Track C becomes M09–M13, and the
  amendment is recorded in `docs/adr/0006-thirteen-modules-not-twelve.md`
  plus a dated note in the spec's own Further Notes. The glossary's
  *Module* entry now says thirteen; the capstone doc's "the twelve
  Modules" means the twelve Modules M13 follows, which stays true.
- **The README's module table now matches what exists.** It lists M01 to
  M13 with the doc paths as authored (the stale `07-testing.md`,
  `08-transactions.md`, `09-observability.md` and `12-native-and-production.md`
  spellings corrected to the real files), the lab slugs as built (row 12's
  `lab-12-capstone-*` replaced with `lab-12-native-images-and-packaging-*`),
  an M13 row, and the twelve-Module "Native images & the production
  capstone" hybrid title split into the two titles the modules actually
  carry. Docs are linked now that every one exists, M12's infra states
  GraalVM (opt.) to match the lab index, and the total reads ~38h with
  Track C as M09–M13 in the Tracks table.
- **The pacing tables match.** `modules/00-welcome.md`'s suggested-pacing
  table says C is M09–M13 at ~16h and the course ~38h, and points at the
  README's module table as the authoritative index.
- **The 2026-10-22 audit's leftovers are both answered.** Since that audit
  ran, every module doc and every lab module was authored, so its "still open"
  paragraph no longer describes the repository - and this ticket settles the
  count question it flagged, by amending the specification. The audit's other
  three criteria were checked then and re-checked today.
- **The lab index in `labs/README.md` already matched what is built**
  (rows 12 and 13 were corrected by the lab-authoring tickets) and needed
  no change. `spring-boot-4-course-implementation-plan.md`, which
  predates the authored modules, carries a pointer to the ADR rather than
  a silent rewrite of the historical plan.

## Comments

- **2026-10-22 · acceptance audit** — Three of the four criteria already
  hold, and were checked: the root README's module table lists M01 to M12
  with its track, its time and its lab, the sibling course is named in the
  README's *Related course* section, and every Markdown link in the course
  docs resolves (one broken link, in this tracker's `map.md`, was fixed).
- **Still open**: the pacing table lists M01 to M12 while only the M01 to
  M04 docs exist, and the lab index in `labs/README.md` names eleven lab
  modules that are not built. The table and the index should be re-run after
  the last Module is authored, or should say plainly which rows are planned.
- **Naming to settle**: the index treats M12 as *Native images & the
  production capstone*, while the backlog splits M12 (*native images and
  packaging*) from M13 (*the production capstone*). The specification says
  twelve Modules. One of the two has to give.
- **2026-10-25 · agent** — Ticket done; the naming decision it raised is
  recorded in ADR 0006 and the spec is amended to thirteen Modules. No
  build surfaces changed - docs, glossary and tracker only - so the two
  seams were not re-run here; ticket 69 (which audited both seams on
  2026-10-22 and re-verified the versions table) re-checks them from a
  clean tree.
