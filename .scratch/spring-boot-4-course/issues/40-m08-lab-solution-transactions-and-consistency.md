# 40: M08 lab solution: Transactions and consistency

**What to build:** The Solution module for M08: the reference implementation of making the borrow operation transactional so two concurrent borrows cannot take the same Copy, proved by a test that fails without the lock, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 39

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Answer

The module is `labs/lab-08-transactions-solution`, listed in the labs
aggregator directly after the start module. The application the M08 start
module carries (the M07 suite's base) is its main source, inherited
byte-for-byte except the Loan feature, where the Lab's work lives; the
Loan feature, the migration and every test the Lab's subject defines are
written from scratch. Ticket 39's probe - the throwaway implementation used
to prove the diagnostics' other half - guided the shapes below; all three of
its surfaced facts are honoured.

**The store moves to the database.** `V002__create_loan.sql` adds the loan
table the diagnostics demand: standard SQL only (identity key, two
varchars, two dates), same words on both engines, columns named
`member_id` and `copy_id`, a foreign key to `copy`. One Loan per Copy is
deliberately *not* a unique constraint - a comment on the migration argues
it: the rule's guard is the borrowing use case's one statement, and a
constraint would swap the loser's refusal from the use case's own words to
a constraint violation.

`LoanRepository` is the store, and every statement the loan feature issues
lives in it: `countByMemberId` answers the Loan limit with a `select
count(*)` off the loan table in the M03 store's shape; `takeOnLoan(copyId)`
is the guarded update verbatim from the M08 doc (`update copy set on_loan
= true where id = :copyId and on_loan = false`), returning the 1/0 the use
case routes on; `save` is the one write that must hand back the row's own
key, made with `SimpleJdbcInsert` (`JdbcClient` cannot return generated
keys), while the reads stay on `JdbcClient`. No transaction annotation on
the store, per the checklist: demarcation belongs to the use case alone.
The `CopyOnWriteArrayList` and the minted-in-compiled-code identifiers are
gone.

**The borrow is the boundary.** `LoanService.borrow(String memberId, Copy
copy)` is `@Transactional` - the count, the guard and the insert inside
one commit, a domain refusal rolling the whole thing back. The Copy
arrives mapped at the edge: `LoanController` calls the M04 availability
gate first (still outside any transaction), then resolves the request's
copy id - the barcode, the external name - to the row via the
`CopyRepository.findByBarcode` derived read, and hands the use case the
Copy the database holds; no barcode crosses into the boundary. An unknown
barcode cannot be refused as not-on-the-shelf (the availability service
just said yes to it), so the edge answers `404`: `CopyNotFoundException`,
translated by the handler into a "Copy not found" problem detail,
mirroring `TitleNotFoundException`. The guard's `0` throws
`CopyNotAvailableException` with the Copy's *barcode* in the detail - the
M06 words, reached after the availability service said yes - so the loser
hears exactly what a third borrower would.

The response payload keeps the library's words: `Loan(Long id, String
memberId, String copyId, ...)` - the member's identifier and the Copy's
barcode as the request named them, and the loan row's key as the id, the
same database-key-in-the-payload precedent the book feature has carried
since M05. The diagnostics never assert the payload's shape, and the
M07-era expectations that the boundary's *dates* answer at
`2026-01-05`/`2026-01-19` survive unchanged.

**The suite asserts the Lab's behaviour, written from scratch.** The
M07-inherited classes that don't touch the Loan store stand untouched
(`TitleControllerTests`, `TitleRepositoryTests`,
`ReservationControllerTests`, `PackagedApplicationIT`,
`FixedClockConfiguration`); the loan classes are the Lab's:

- `LoanControllerTests` (webmvc slice) - the boundary's answers, including
  two the store moved: a barcode the database does not hold is a 404 from
  the copy lookup, and a borrow the use case refuses (mocked) is answered
  in the M06 words, whatever asked it to throw.
- `LoanWiringTests` (the one full-context MockMvc home, from scratch) -
  the deleted version's borrows named copies in no database, so every
  borrow here seeds its Copy rows through SQL first, the way the
  diagnostics model it. Created/dated borrows write the Loan row and mark
  the Copy; a repeated borrow is a 409 with one Loan row left and the Copy
  marked (the H2-visible half of the guarantee); the Loan limit refuses
  through the real store's count with the three written rows asserted.
- `LoanRepositoryTests` (container tier, PostgreSQL 17 +
  `@ServiceConnection` + `@EnabledIfDockerAvailable`) - the store's
  statements on the second engine: the inserted row read back in its own
  columns, the count answering it, and the guard's contract read at that
  engine - 1 on the take, 0 on the repeat, 1 on a different Copy - with a
  Javadoc that names the concurrent half the diagnostics own. A skip is a
  no-daemon message, never a pass.

**Verification.** `./mvnw -f labs -pl lab-08-transactions-solution verify`
is green, 22 tests plus the packaged-jar IT, with both container classes
actually run under the local Docker daemon, the coverage floor met, and
Spotless/PMD/SpotBugs/Checkstyle present. The diagnostics on this module
(`./mvnw -f labs -pl lab-08-transactions-solution verify -Pdiagnostics`)
are green 3/3 including the two-thread PostgreSQL race - exactly one 201,
one 409 in the M06 problem-detail words, one Loan row, the Copy marked.
The same command on the start module stays red 3/3 with the Lab's
messages, so the asymmetry is intact. `./mvnw -f labs verify` builds the
whole reactor green (23 modules) and
`./mvnw -f labs verify -Pdiagnostics-solutions` - CI's statement - is
green: lab-08's Solution passes its diagnostics through the single
command, the Start module skipped through its `diagnostics.skip=true`.
`labs/pom.xml` lists the module; `labs/README.md` already carried the
pair's names in its lab index.
