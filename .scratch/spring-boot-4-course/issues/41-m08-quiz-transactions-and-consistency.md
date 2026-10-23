# 41: M08 quiz: Transactions and consistency

**What to build:** The quiz for M08: five questions that test whether the reader understood where the boundary belongs, what propagation changes, and which locking strategy prevents the double loan, each with an answer that explains itself.

**Blocked by:** 40

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

The quiz was drafted with the doc (ticket 38); this ticket graded it against
its criteria and rebuilt the two questions that failed the grade.

- **Grading jury (the ticket's named understandings):** the drafted quiz
  covered *what propagation changes* (question 3, the unchanged API hiding
  the second pooled connection) and *which locking strategy prevents the
  double loan* (question 4, the count's protection is the boundary and the
  `1`/`0` answer decides the Loan row's writer). The first named
  understanding - *where the boundary belongs* - went untested: questions 1
  and 2 ask the machinery and JPA-manager rows, and nothing forced the
  placement decision the doc's "wraps the use case, and nothing bigger"
  section argues for.
- **Question 2 rebuilt** (boundary placement): the scenario is the
  solution's own shape reviewed by a colleague - move `@Transactional`
  onto the controller for a transaction end to end, and pull the M04
  availability call into `LoanService.borrow` so check and commit are
  atomic. One defensible answer, two mechanisms: the proxy rule (the
  controller is not a proxied bean, so Spring MVC invokes the method
  directly and the annotation is read by nobody - the boundary sits on the
  service's use case because that is the bean the proxy wraps), and the
  pool (a remote round trip inside the boundary pins one of HikariCP's
  `maximum-pool-size` connections for the call's duration). Both claims
  are the doc's own and the solution's javadoc states the second verbatim
  (`LoanService.borrow`: the M04 availability call stays where it has
  always been, in the controller, before a transaction exists).
- **Question 5 rebuilt** (the rig's claims): the draft was guessable from
  its wording - "reports 'concurrency proven' without a Docker daemon in
  sight" narrates the verdict before the reader knows anything, the
  "do the keys survive?" failure mode ticket 26 named. Rebuilt as a
  partition: the solution asserts three claims (the guard's `0` for the
  second borrow, two concurrent borrows cannot both take the Copy, the
  loser hears a `409` problem detail) and the reader must say which the
  H2 run carried (the first and third - application code and statements,
  faithfully executed) and which it did not (the second - H2's `for
  update` runs politely for one thread, exactly the condition the race
  exists to violate), so the answer cannot be guessed without
  understanding what each engine can and cannot judge. The skip-is-not-a
  -pass half survives inside the answer, anchored to the `@EnabledIfDockerAvailable`
  guard the Solution module's `TransactionsOnPostgresqlDiagnosticsTest`
  actually carries.
- **The JPA-transaction-manager question was dropped to make room** - the
  same trade tickets 26 and 36 made: its answer was a negative-existence
  fact ("there never was a `JpaTransactionManagerAutoConfiguration`"),
  recallable from the doc but not reason-able, and the question named the
  class it asked about. The modularisation row stays fully taught in the
  doc, and question 1 still anchors it.
- **Questions 1, 3 and 4 kept**: one defensible answer each, none
  guessable from wording, each naming its delta row or section.
  Claims in the rebuilt questions were checked against the resolved lab
  code (the controller's availability call, `LoanService`'s javadoc, the
  diagnostics' `@EnabledIfDockerAvailable`), not written from memory.

## Comments

- **2026-10-23 · agent** — Graded the ticket-38 quiz against the
  acceptance criteria; two questions failed the jury (the
  boundary-placement understanding untested; the rig question telegraphed
  by its wording) and were rebuilt with claims checked off the resolved
  lab code. Doc edit is the resolution; no code changed.
