# 46: M09 quiz: Actuator and observability

**What to build:** The quiz for M09: five questions that test whether the reader understood liveness against readiness, what belongs in a metric rather than a log, and how to keep management endpoints off the public surface, each with an answer that explains itself.

**Blocked by:** 45

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

The quiz was drafted with the doc (ticket 43); this ticket graded it
against its criteria and rebuilt two questions.

- **Grading jury (the ticket's named understandings):** the drafted quiz
  covered *how to keep management endpoints off the public surface*
  (question 5, `include=*` plus `show-details=always`, each line's
  stranger-handover named, the rewrite into a test profile) and - through
  the probe row - the *default* that makes the probes answer on 4.1.1.
  The named understanding *liveness against readiness* went untested:
  question 2 asked the condition annotation, the upgrade's
  auto-configuration fact, and never forced the restart-versus-routing
  split the doc's probe section argues for. And *what belongs in a metric
  rather than a log* was not asked at all - question 3 covered the
  metrics-consent row's property spelling, a recall question.
- **Question 2 rebuilt** (liveness against readiness): the scenario is a
  colleague's change that moves the M04 availability-service client into
  the liveness probe group - "an unreachable dependency is exactly the
  kind of outage a restart fixes". One defensible answer, three
  mechanisms, all the doc's own claims: liveness answers "restart me?"
  and holds `CORRECT`/`BROKEN` with no network call inside it, so a
  remote outage turns the probe into a restart loop that retries what no
  restart can remedy; readiness answers "route me?", holds
  `ACCEPTING_TRAFFIC`/`REFUSING_TRAFFIC`, moves by the one API -
  `AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC)`
  - and answers `OUT_OF_SERVICE`, not `DOWN`; and the doc's cost warning
  applies to the client inside any `health()` unless the check is cached
  and cheap. The draft's verdict-telegraphing line ("The 4.1.1 bytecode
  says otherwise") went with it.
- **Question 3 rebuilt** (metric rather than log): the draft's
  metrics-consent question went, the same trade tickets 26 and 41 made -
  its answer was two property spellings and a metadata level, stated as
  text in the delta table and reasoned out by nobody. The rebuilt
  question reviews two propositions at once - replace the counter with an
  INFO line per borrow ("count the lines when the dashboard asks"), and
  add a Copy identifier tag ("one Copy's history becomes a graph") - and
  the answer is one decision each: the aggregate's question ("how many
  Loans this morning") is a counter's to answer, one series in the
  registry rather than a shipped, indexed, queried log corpus whose cost
  grows with every event; and a tag is safe when its value domain is
  bounded by the data - the Title is finite for the catalogue - never by
  the requests (a request id, a [Copy](../../../GLOSSARY.md) identifier:
  the time-series zoo).
- **Questions 1, 4 and 5 kept**: one defensible answer each, none
  guessable from wording - question 1 pins the modularisation rows (the
  compile error whose fix is one import), question 4 the gated-export
  contract (neither "refuses to start" nor "sends everything"), question
  5 the never-exposed list with its rewrite.
- Claims in the rebuilt questions were checked against the doc's verified
  material - the two states' class-file values, the publish API, the
  `OUT_OF_SERVICE` translation, the metrics endpoint's read-back and the
  named cardinality mistake - not written from memory.

## Comments

- **2026-10-24 · agent** — Graded the ticket-43 quiz against the
  acceptance criteria; two questions failed the jury (the
  liveness-versus-readiness split untested and question 2's verdict
  telegraphed by its wording; the metric-versus-log understanding
  untested, question 3 being spelling recall) and were rebuilt. Doc edit
  is the resolution; no code changed.
