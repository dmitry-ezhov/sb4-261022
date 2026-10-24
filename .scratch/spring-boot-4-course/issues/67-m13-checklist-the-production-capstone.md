# 67: M13 checklist: The production capstone

**What to build:** The checklist for M13: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 66

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers every earlier checklist is applied to this code
- [x] it covers the operational surface is documented
- [x] it covers the build is green with every gate enabled
- [x] it covers the capstones answers the brief it was given
- [x] no item restates a Quality gate that the build already enforces

## Answer

The checklist was drafted fresh against the doc (ticket 63) and its Lab
section, graded against the ticket's seven criteria before landing, the
way ticket 62 graded its M12 checklist. It is the `## Checklist` section
appended to `modules/13-the-production-capstone.md`, in the established
seven-item shape: shared intro, bold-led items, each closing with a
"Verified by" that is a read or a run. The two meta-criteria are graded
across every item, the four named coverage targets are one item each:

- **The seven criteria, graded item by item.** *Observable
  properties over steps* is global: no item's subject is a step to
  perform - "the outage is published as state, on the two moments the
  process already knows", "the bounds are the environment's numbers",
  "the diff is the brief", "the green the change is accepted under" -
  and each item's *how* sentences describe what the property looks like
  when kept and when broken. *Verifiable by read or run* is each item's
  closing line: the publish homes are read off the diff, the bounds'
  behaviour is read off the bounds diagnostics' override boot, the tree
  diff answers the brief item, the yml answers the documented-surface
  item, the commands answer the strict-green item. The four named
  coverage targets are one item each: *every earlier checklist applied
  to this code* is the M01-to-M12 walk item (M02's jar read through
  `PackagedApplicationIT`, M04's timeouts on the wire, M05's migration
  in the diagnostics' fresh-H2 schema, M09's counters through the
  management socket, M10's chains the outage walked, M11's bounds and
  drain, M12's repackage - with the readiness state named as the one
  gap the Lab closes), the *operational surface documented* is the
  assembled-yml-and-javadoc item (the yml's own comments carry each
  decision and where its override arrives from; no new document), the
  *green build with every gate enabled* is the strict-green item (the
  whole-module `verify`, the Solution diagnostics, `-Pquality-lenient`
  named as mid-task convenience, both asymmetry commands), and *answers
  the brief it was given* is the diff item: the Solution's diff against
  the Start module is exactly the three files the Lab's four numbered
  moves name, and everything outside them is the same bytes.
- **No item restates a gate.** The gates speak formatting, defects and
  coverage; the items speak publishes, wires, placeholders, trees, yml
  comments and reported runs. The one gate-adjacent item names the
  commands a review runs and demands the run that was made be the run
  reported - M07's precedent for the same duty - and never asserts a
  rule Spotless, PMD, SpotBugs, Checkstyle or JaCoCo would fail the
  build for; the coverage floor appears nowhere.
- **Facts checked against the artifacts, three wording corrections
  made before landing.** The change is read directly
  (`ApiExceptionHandler`, `AvailabilityChecker`, the solution's
  `application.yml`, the red/green diagnostics), and the diff against
  the Start module answered in three files exactly as the brief item
  now claims. Mid-grading corrections: the retry-counter claim was
  first "counts no more replays than the budget allowed", which
  miscounts the route the bounds diagnostic's own ceiling names - the
  two-event ceiling (the failed attempt's event plus the exhaustion
  mark) is what a one-attempt budget owes, and the item now says that;
  "the twelfth could not apply to this code" became the readiness gap
  phrased as what it is - an item the twelfth's walk did not have,
  which the Lab closes; and a stray "console" typo left by an editing
  pass was caught and restored to "ticket".

## Comments

- **2026-10-25 · agent** - Authored the checklist as a doc edit: the
  Checklist section appended to
  `modules/13-the-production-capstone.md` in the established format,
  graded against the ticket's seven criteria before landing, with the
  lab-13 pair's code diff, the diagnostics' assertions and the bounds
  numbers read off the artifacts before each item claimed them. The
  diagnostics were not re-run: no code changed, doc edit is the
  resolution, and the red-on-Start/green-on-Solution record is taken
  from ticket 65's resolved runs. Map entry appended.
- **2026-10-25 · agent** - Review pass (two axes, HEAD~1 base). The
  Spec axis verified every load-bearing claim against the artifacts -
  the three-file diff between the Start and Solution trees, the publish
  homes, the String-variant defaults, the bounds diagnostics' numbers
  (the two-event counter ceiling, the 2.5s read under a named 5s, the
  3s drain), `PackagedApplicationIT`'s manifest read and the three
  profile spellings in the root POM - and found the seven criteria met
  and no scope creep. Three wording defects fixed in the doc: the
  bounds item's headline first claimed all three bounds sit in "one
  configuration namespace", false of the timeouts and the drain window
  (now: the geometry in one namespace beside the base URL, the
  `spring.*` keys beside it); item 2's "during one outage" miscounted
  the readiness diagnostics' evidence (the `library.loans.issued` read
  is post-recovery - now "across one outage and its recovery"); and
  item 6's "in Module order" enumerated M02 before M01 (now M01
  first). The Standards axis found no hard violations against the
  sibling checklist conventions, the tracker format or the glossary;
  its two command-spelling judgements were consolidated into the Spec
  fix - the checklist's commands now match the Lab section's and the
  labs index's spelling (`-pl ... verify` with no `-am`), which also
  removed the same Start-module red command being asserted two ways in
  one doc.
