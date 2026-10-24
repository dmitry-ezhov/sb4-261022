# Thirteen Modules, not twelve

The course ships thirteen Modules - M01 to M13 - organised in three Tracks:
the bridge (M01–M04), data and testing (M05–M08), and the platform (M09–M13),
with Track C ending in a native-images-and-packaging Module (M12) and a
production capstone (M13). The specification originally said twelve.

We did this because the two subjects the twelve-Module outline folded into
one platform Module turned out to be different work done at different moments
on different surfaces. Native images and packaging is a *build-time* subject:
what AOT processing reads, what `spring.aot.enabled` freezes, what form the
artifact travels in. The production capstone is a *run-time* subject: an
operational read of the assembled application - what boots, what the platform
may ask, what the process refuses - whose lab is the one behavioural promise
the course teaches but never builds (the readiness publish). An authoring
pass at each showed neither fits an evening when the other rides along, and
the vocabulary pays for the split: M12 builds forms, M13 assembles the whole
and reads it, and no Module has to claim both.

The alternative was to merge the two into one Module to keep the
specification's twelve intact. That was rejected because both Modules were
already authored and verified by the time this decision surfaced - doc, lab
pair, quiz and checklist each, tickets 58 through 67 - and unpicking one
Module pair to satisfy a count in a draft would trade finished, verified
content for a number. The other alternative, keeping twelve by dropping M13,
breaks the course's own promise that the path covers first commit to
production.

Consequentially, the glossary's definition of a *Module*, the repository
README's module table and the welcome doc's pacing table state thirteen and
Track C as M09–M13, and the capstone doc's references to "the twelve
Modules" mean the twelve Modules M13 *follows*, not the course's total.
