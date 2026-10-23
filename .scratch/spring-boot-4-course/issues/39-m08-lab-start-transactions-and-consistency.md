# 39: M08 lab start: Transactions and consistency

**What to build:** The Start module for M08: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers making the borrow operation transactional so two concurrent borrows cannot take the same Copy, proved by a test that fails without the lock.

**Blocked by:** 38

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Answer

`labs/lab-08-transactions-start` is the library application exactly as
M07's Solution left it: every main source, the migration, the
application.yml and every test class are byte-for-byte the lab-07 solution's,
with three deliberate differences - the POM (artifactId, description, and the
two Start-module skips `<jacoco.skip>` and `<diagnostics.skip>`), the reactor
entry in `labs/pom.xml`, and the diagnostics package, whose M07 classes are
replaced by the Lab's own two. No application code was rewritten and the
module is not listed as ready for a learner until ticket 40 has its pair.

**The diagnostics** (`TransactionsDiagnosticsTest`,
`TransactionsOnPostgresqlDiagnosticsTest`) state the doc's three-part
upgrade, each red message naming the piece that is missing:

- `aBorrowIsARowTheDatabaseHolds` - writes one Title's one Available Copy
  into the database, borrows it through the API, and demands the Loan row
  the database holds (`loan` where `copy_id` resolves the Copy through its
  barcode). On the Start module it fails where the borrow reaches nothing:
  there is no loan table, and the message names the migration the Lab adds
  (a V002__ file under `db/migration`, columns `member_id` and `copy_id`) and
  the store move.
- `aSecondBorrowOfTheSameCopyIsARefusalNotASecondLoan` - borrows the same
  Copy twice. On the Start module both borrows are `201`, and the message
  names the one-statement guard (`update copy set on_loan = true where
  id = :copyId and on_loan = false`) and the `0`-answer's translation into
  the M06 handler's `409` problem detail, "Copy is not on the shelf"; the
  test also demands one Loan row and the Copy actually marked on loan.
- `twoConcurrentBorrowsCannotTakeTheSameCopy` (@EnabledIfDockerAvailable,
  PostgreSQL 17 through Testcontainers 2 and `@ServiceConnection`, exactly
  the M05 rig) - two Members, two threads, one Copy. On the Start module
  both borrows are `201` and the message names the transaction boundary and
  the guard statement; the assertions also pin the loser's refusal (problem
  detail in the availability words), the single Loan row and the marking.

**Verification.** The default build is green through every gate the start
module opts nothing out of but the coverage floor (`./mvnw -f labs
-pl lab-08-transactions-start verify`): 16 tests and the packaged-jar IT
green, the container test of the copied suite actually run under the local
Docker daemon, PMD, SpotBugs, Checkstyle and Spotless applied. The
diagnostics run (`./mvnw -f labs -pl lab-08-transactions-start verify
-Pdiagnostics`) is red 3/3 with the messages above. The whole labs reactor
(17 modules) builds green, and `-Pdiagnostics-solutions` is green with the
Start module skipped through its `diagnostics.skip=true`.

**The contract's other half was proved**, ticket-34 style, with a throwaway
probe module at a scratch path outside the repository (deleted afterward):
a from-scratch implementation of the Lab's ask - V002__create_loan, a loan
store over `JdbcClient` whose `takeOnLoan` is the guarded update, a borrow
that is the `@Transactional` boundary with the count, the guard and the
insert inside it and the availability call still outside in the controller -
passed the diagnostics green 3/3, including the real two-thread race on
PostgreSQL: exactly one `201`, one `409` in the M06 words, one Loan row, the
Copy marked. The probe lives nowhere.

**For ticket 40 (the Solution module).** The diagnostics are fixed
statements, so the reference implementation must satisfy exactly what the
three tests assert, and three facts the probe surfaced are worth carrying:

1. *The external Copy id is the barcode.* The diagnostics write Copy rows
   through the database and borrow them by the `barcode` column's value; the
   solution's edge mapping is barcode -> row key, then the guarded update
   works on the key. The Loan payload can carry the external identifiers -
   the diagnostics never assert its shape.
2. *The copied lab-07 wiring tests will not survive unchanged.* Their
   borrows name arbitrary copy ids ("c-001") that exist in no database; a
   database-backed borrow refuses them with `409`. The Solution writes its
   own suite anyway and must seed Copy rows through the database before the
   wiring test borrows - the diagnostics themselves model that in
   `writeACopyOnTheShelfOfARegisteredTitle`.
3. *The loan limit's protection stays the boundary's* - the H2 diagnostic
   deliberately does not ask anything extra of the count: on the Start
   module it already answers, and the doc's quiz answer 4 is the review
   record for why. The Solution must keep the count inside the same
   boundary and read it from the `loan` table, which
   `LoanRepository.countByMemberId` shape (one "how many") reads the same
   whichever store backs it.
