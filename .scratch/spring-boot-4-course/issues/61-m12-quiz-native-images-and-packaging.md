# 61: M12 quiz: Native images and packaging

**What to build:** The quiz for M12: five questions that test whether the reader understood what ahead-of-time processing changes, what a native image costs, and when to stay on the JVM, each with an answer that explains itself.

**Blocked by:** 60

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

The quiz was drafted fresh against the doc (ticket 58) and graded against the
ticket's three named understandings and its five criteria before landing.
No draft existed to grade - ticket 58's map entry names this ticket (61) and
62 as the doc-pair that owns the quiz and the checklist - so the answer text
below is what the two-pass grading produced.

- **The three named understandings, distributed:** *what ahead-of-time
  processing changes* is questions 1 and 2 - question 1 judges an upgrade
  memo's three promises (the frozen conditions/beans/profiles, the
  drop-the-flag regular-mode fallback which is TRUE and the counterintuitive
  one, and the process-aot-needs-GraalVM claim which is backwards: the walk
  rides the same build as the repackage and the JVM form is its own
  documented deployment form), question 2 names what Framework's AOT
  chapter says is untransformable (instance suppliers, `registerSingleton`)
  and keeps the hints ledger strictly separate from the definition ledger.
  *What a native image costs* is question 3 - four cost sentences filed
  across the build/run ledger, one mis-filed (sentence 4's invented premise
  "the image must be built where it runs"; the version-25 distribution is
  the build machine's, the run machine ships no JVM at all). *When to stay
  on the JVM* is questions 4 and 5 - question 4 is the AOT-cache runbook
  whose line (iii) fails twice (the cache must pair with the extracted
  form, and retraining is gated on two conditions, not one), question 5 is
  the three-site form-assignment review whose middle site blocks itself
  (a build-time-frozen form cannot serve a constraint that wants boot-time
  deciding; the flag-free regular run is the form that keeps it).
- **The trivia risk, handled the way the sibling quizzes were:** the delta
  table is dense with version numbers and this quiz deliberately makes none
  of them an asked-for number. JEP 483, the 0.10.6/1.1.8 pins, the 8GB box
  and the 0.08 sample appear only as evidence inside answers; the questions
  ask for judgment, attribution and ledger placement, not recall.
- **The facts, checked not remembered:** the analyzer's two sentences, the
  "evaluates `@Conditional` annotations based on the environment at build
  time" sentence, the beans-cannot-be-switched-off sentence, the
  instance-supplier/registerSingleton pair, the training-run commands and
  the extracted-form/same-Java conditions, the "complete, platform-specific
  executable" sentence, the JDK 24/JEP 483 attribution and the
  profile-freezing `<profiles>` snippet are all quoted off the M12 doc,
  which itself read them off the jars and pages. The lab-12 facts the
  answers lean on - the `process-aot` execution in its default phase
  (prepare-package), the "Starting AOT-processed" playback line, the
  combined cache-plus-switch run being the cheapest form - are read off
  the resolved lab-12 solution's POM and `DeploymentFormsDiagnosticsIT`.
- **The wording check:** every question's answer turns on a fact the stem
  does not state - the analyzer's regular-mode sentence (1), the
  untransformable-registrations pair (2), the build/run GraalVM split (3),
  the extracted-form and two-condition cache requirements (4), the
  reflective-path escape and the JEP 483 attribution (5). No stem
  narrates its verdict; the one-line-per-failure asks came out of
  tickets 26/41/46/51's telegraph removals.

## Comments

- **2026-10-24 · agent** — Authored the quiz as a doc edit: the Quiz
  section appended to `modules/12-native-images-and-packaging.md` in the
  established five-question format, graded against the ticket's three
  named understandings and its criteria before landing. Facts checked
  against the M12 doc's jar and page reads and the resolved lab-12
  solution (`DeploymentFormsDiagnosticsIT`, the solution POM). Doc edit
  is the resolution; no code changed, so no gate run applies.
- **2026-10-24 · agent** — Review (code-review, two axes, HEAD~1 base):
  both axes green. The Spec axis found no factual inconsistencies - quiz
  claims cross-checked against the doc, the lab-12 solution's POM and
  `DeploymentFormsDiagnosticsIT`. Three judgement-call notes from the
  Standards axis: the "hold-limit" compound is kept, the M12 doc itself
  (ticket-58-reviewed) having established it for `ReservationRules`, and
  the quiz must not diverge from the doc it summarizes; the verdict-refrain
  echoes across answers are deliberate reinforcement, the same device the
  sibling quiz scores answers with; the one house-dash slip in question
  3's answer was fixed and committed.
