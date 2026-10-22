# 68: Course index and documentation pass

**What to build:** A learner can find any Module from the index, see the tracks, the pacing and the reading lists, and follow any link without hitting a dead end.

**Blocked by:** 67

**Status:** ready-for-agent

- [x] the index lists every Module with its track, its time and its lab
- [x] every link between docs resolves
- [ ] the pacing table matches the modules that exist
- [x] the reading lists point at the sibling course where the depth belongs there

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
