# 66: M13 quiz: The production capstone

**What to build:** The quiz for M13: five questions that test whether the reader understood the trade-offs reviewed in one pass, and what to check before the application meets a user, each with an answer that explains itself.

**Blocked by:** 65

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

The quiz was drafted fresh against the doc (ticket 63) and graded against the
ticket's two named targets and its five criteria before landing. It is the
`## Quiz` section appended to `modules/13-the-production-capstone.md`, in the
established five-question format.

- **The two named targets, distributed:** *the trade-offs reviewed in one
  pass* is question 1 - the attribution memo, whose four "delivered by
  4.1.1 with no configuration" sentences sort two ways: the probe default
  survives as filed (the doc's own `matchIfMissing` read, one of the two
  behaviour flips the table attributes to Boot 4 itself), graceful
  shutdown is delivered but one generation early (3.4, the misattribution
  M11's post-mortem sentence modelled), the problem-details switch is
  doubly false (3.0's arrival, still default-false on 4.1.1), and the
  resilience annotations name the wrong *layer* (Framework 7.0.9,
  resolved by jars the build already pulls). *What to check before the
  application meets a user* is questions 2-5: the refusal map's
  consolidation into an `@ExceptionHandler(Exception.class)` catch-all
  (where the statuses are chosen, and the swallowing of unowned
  failures), the bounds that live in the environment (the annotation's
  String variants carrying `${...}` placeholders, the stripped yml booting
  shaped per the reviewed defaults, `includes` staying a compiled type
  reference), schema ownership under the failed rehearsal boot (`validate`
  as the fast wrong answer the cost paragraph prices, `update` and
  gate-removal as the two burials, the next migration as ownership), and
  the outage's five surfaces with the patch's extra `503` judged against
  the routing/refusal split the Lab's publish rests on.
- **The overlap risk, handled before drafting:** each question was
  checked against the sibling quizzes' ground so no question re-asks
  what a sibling already asked. M06 q3 owns the colleague-deletes-the-
  handler question, so question 2's angle is the *consolidation* PR and
  what the grep protects; M05 q2 owns the bare `flyway-core` trap, so
  question 4 never mentions the starter dependency; M09 q2 owns the
  liveness/readiness split and the publish API, so question 5 enumerates
  surfaces and judges the patch rather than re-teaching the probe pair;
  M11 q5 owns the graceful-shutdown attribution, so question 1 uses it as
  one wrongly-filed sentence among four rather than as the question; M12
  q1 owns the frozen-conditions profile mechanics, so no question asks
  them.
- **The trivia risk, handled the way the sibling quizzes were:** the
  delta table is dense with versions and pins, and the quiz makes none of
  them an asked-for number - every asked thing is a judgment (which
  filing is wrong, where a status is chosen, which bounds ride the
  environment, what a refusal bought, which surface changed).

## Comments

- **2026-10-24 · agent** — Authored the quiz as a doc edit: the Quiz
  section appended to `modules/13-the-production-capstone.md` in the
  established five-question format, graded against the ticket's two named
  targets and its criteria before landing. Facts checked against the M13
  doc's own reads (the `matchIfMissing`/`javap -v` probe read, the
  `spring-boot-web-server` and `spring-boot-webmvc` metadata reads, the
  3.4 attribution sentence) and against the resolved lab-13 pair's
  records (tickets 64/65: the String-variant geometry, the overrides read
  back on the wire, the publish in the boundary's handler and the
  checker's recovery). Doc edit is the resolution; no code changed, so no
  gate run applies. Map entries appended (ticket 65's pointer backfilled).
- **2026-10-25 · agent** — Code-review (two axes, HEAD~1 base): both axes
  green. The Spec axis cross-checked the quiz's load-bearing facts against
  the M13 doc and the lab records and found no inconsistencies; the
  Standards axis verified the ticket-file, authoring-template and
  register conventions against the sibling quizzes and found nothing to
  fix. Two wording notes were applied and committed: question 4's answer
  now says "each migration script is standard SQL" (the lab holds V001
  and V002; the loose "one script" miscounted against the doc's own
  "the V001 and V002 scripts"), and question 3's "line one of the
  assembled yml" became "a line of the assembled yml" (the drain window
  is not the excerpt's first line). One housekeeping judgement call
  answered, no change: the backfilled ticket-65 map pointer rides this
  ticket's commit, disclosed here, per the tracker's map-pointer
  convention and the precedent of ticket 56 backfilling ticket 55's
  pointer.
