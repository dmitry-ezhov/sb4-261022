# 56: M11 quiz: Resilience and virtual threads

**What to build:** The quiz for M11: five questions that test whether the reader understood when virtual threads help and when they do not, what a timeout does not solve, and how to observe saturation, each with an answer that explains itself.

**Blocked by:** 55

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

The quiz was drafted with the doc (ticket 53); this ticket graded it against
its criteria and the ticket's three named understandings, and rebuilt three
of the five questions.

- **Grading jury (the ticket's named understandings):** the draft tested
  *when virtual threads help* only as mechanics - question 1 asked the
  worker model, the introducing release and the connector caps, never the
  contrast the doc argues (the held request gets cheap; the synchronized
  concern retires while native-frame stacks still pin). *What a timeout
  does not solve* was stated only in question 2's answer, never in its
  question - the reader was asked the spellings and the builder mechanism,
  and had to produce neither the configurer-bypassing call base nor the
  bound-vs-repair point. *How to observe saturation* went untested
  entirely: the dead busy-worker gauge and the refusal/retry-counter
  instruments live in the doc and the checklist, and no question touched
  observation.
- **The wording failure was question 5's tail clause:** "so that the
  sentence's only error is its date" told a reader who never opened the
  Module where the error is, reducing the answer to recalling a number -
  the same telegraph tickets 26, 41, 46 and 51 removed from their drafts.
- **Question 1 rebuilt** (when virtual threads help and when they do not,
  as the contrast): the upgrade memo carries three promises - the borrow
  stops holding a scarce worker under a slow upstream (true; the Module's
  thesis), the JDK 25 rollout audit is settled because nothing pins any
  more (false: JEP 491 retired the `synchronized` cause, native frames
  still pin, and this library's audit is short but not zero), and the
  connector can now accept more than 8192 (false: the property flips the
  executors - `VirtualThreadExecutor`, then the `SimpleAsyncTaskExecutor`
  and `SimpleAsyncTaskScheduler` - not the connector; `max-connections`
  and `accept-count` bind arriving work unchanged). The flag's mechanics
  survived into the answer, verified rather than assumed.
- **Question 2 extended** to make the reader produce what the timeout does
  not solve: the stem's memo now declares the two lines "the fix that
  bounds every outbound call this library can make", and the answer walks
  the two fences the sentence crosses - the reach is the builder's
  configurer (a client built *around* the injected builder waits forever),
  and a timeout is a bound, not a repair (the failed ask still fails the
  borrow, which is why the replay is a declaration of its own on the
  retry row).
- **Question 4 rebuilt** to carry the observation the ticket names: the
  refusal type and its parentage stay (`InvocationRejectedException`
  extends `RejectedExecutionException`, read off the 7.0.9 class
  file), the colleague's debug line becomes the wrong call to judge, the
  Boot 2 busy-worker gauge is answered with what it reads on this boot
  (a spawner, no stable thread count; no idle `tomcat-handler-` worker in
  the dump), and the saturation's real surface is named - the refusals as
  problem details and, one row up, `library.availability.retried` on the
  `MethodRetryEvent`, exactly the counter the lab-11 solution carries.
- **Question 5 kept, tail clause removed:** the sentence's error is now
  part of the reader's job ("where it goes wrong - the release is part of
  the answer"); the release (`3.4`), the jar enumeration that checked it,
  and the 4.1 packaging move are all still the answer's burden.
- **Question 3 kept unchanged:** the arithmetic (1 + `maxRetries`
  attempts, 100ms and 200ms), the empty-`includes` semantics
  ("a retry attempt for *any* exception") and the proxy trap - one
  defensible answer, none of it guessable. Claims in the rebuilt
  questions were checked off the resolved lab-11 solution (`RetryMeter`'s
  counter, `AvailabilityChecker`'s geometry, the injected-builder client)
  and re-verified where the question states a jar fact: the
  `AnnotationDefault` of `@ConcurrencyLimit.policy` resolves to `BLOCK`,
  `ThrottlePolicy` is exactly `{BLOCK, REJECT}`, and the singular timeout
  property carries `"deprecation": {"replacement": ..., "since": "4.0.0"}`
  in the shipped metadata - not written from memory.

## Comments

- **2026-10-24 · agent** — Graded the ticket-53 quiz against the acceptance
  criteria; the jury found the virtual-thread help/not-help contrast
  untested (question 1 asked mechanics), the timeout question never
  asking what the timeout does not solve, observation entirely untested,
  and question 5's tail clause telegraphing where the sentence's error
  lives. Rebuilt questions 1, 2 and 4, removed the question-5 telegraph,
  kept question 3. Claims checked against the resolved lab-11 solution
  and re-verified jar facts on the 7.0.9 and 4.1.1 artifacts. Doc edit is
  the resolution; no code changed.
- **2026-10-24 · agent** — The map pointer for ticket 56 landed in
  `map.md`, and ticket 55's missing pointer was backfilled beside it
  (the entry had not landed with that ticket's resolution, which ticket
  55's own record does not mention; the backfill says so in the map).
- **2026-10-24 · agent** — Review (code-review, two axes, HEAD~1 base):
  both axes green; three judgement-call notes applied. Question 4's stem
  carried the debug-line element the answer judged but never asked for,
  so the ask now names it; the answer's BLOCK paragraph retold question
  1's starvation promise nearly verbatim, crossed to point at that
  promise; question 5's rebuilt stem gave a guesser the release's
  involvement, so the ask is "name the release that actually flipped the
  default" again - the reader must still find that the sentence is wrong
  and which release. The spec axis found no factual inconsistencies
  (quiz claims cross-checked against the doc's table, the lab-11
  solution's `RetryMeter`/`AvailabilityChecker`, and the solution
  `application.yml`).
