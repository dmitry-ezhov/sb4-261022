# 42: M08 checklist: Transactions and consistency

**What to build:** The checklist for M08: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 41

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers the boundary wraps the use case, not the repository
- [x] it covers the concurrency guarantee is proved by a failing-then-passing test
- [x] it covers no transaction is opened on a read-only path
- [x] it covers the isolation level is a deliberate choice, stated in the doc
- [x] no item restates a Quality gate that the build already enforces

## Answer

The checklist was drafted with the doc (ticket 38); this ticket graded it
against its criteria and closed the two that were open.

- **Four criteria the draft already met:** the boundary item (`@Transactional`
  on the service's use case, the repository annotated by nobody, the
  boundary-per-store-call arrangement named as the defect), the
  failing-then-passing diagnostic item with its command, verifiability
  (every item names what a read of the change answers, or the command that
  decides), and no restatement of a build gate (the gates speak in
  formatting, defects and coverage; the checklist speaks in transactions).
- **No transaction is opened on a read-only path - new item**, placed with
  the boundary items. Checked against the resolved solution before it was
  written: the module carries exactly one `@Transactional` (on
  `LoanService.borrow`), no `readOnly = true` anywhere, and no annotation
  on the controller or the repository - so "the only `@Transactional` in
  the change is on the use case" is a search of the diff, as the item says.
  The item also pre-empts the misreading that would fail it: the Loan
  limit's count sits inside the boundary by design - it is half of the
  guard - and is not a read that opened a transaction; the read-only paths
  are the ones that run with no boundary at all (the barcode mapping the
  controller does before the use case, the catalogue's Copies of a Title).
- **The isolation level is a deliberate choice, stated in the doc - new
  item**, placed with the stated-decision items beside the
  optimistic-versus-pessimistic one. Checked the same way: no
  `isolation = ...` appears anywhere in the solution, and the absence is
  exactly what the doc's isolation section argues (PostgreSQL's
  `READ_COMMITTED` plus the guard statement placed correctly; tightening
  isolation to fix a race the statement should have fixed is the defect).
  Verifiable by reading the change and reading the doc section it points
  to - not by re-litigating the trade.
- **Confirmed with a run:** `./mvnw -f labs -pl
  lab-08-transactions-solution verify -Pdiagnostics` - BUILD SUCCESS, both
  diagnostics tests green, and because a Docker daemon is present the
  PostgreSQL rig actually ran (`TransactionsOnPostgresqlDiagnosticsTest`,
  Testcontainers postgres:17, 1 test, 0 skipped) - the run the
  checklist's diagnostic item, and the quiz's question 5, describe.

## Comments

- **2026-10-23 · agent** — Graded the ticket-38 checklist against the
  acceptance criteria; two criteria were open (read-only paths, isolation
  as a stated choice) and each became an item of its own, with both
  checked against the resolved lab code. Doc edit is the resolution; no
  code changed.
