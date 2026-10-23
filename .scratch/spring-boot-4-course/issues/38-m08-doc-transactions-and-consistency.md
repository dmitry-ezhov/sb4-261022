# 38: M08 doc: Transactions and consistency

**What to build:** The Module doc for M08: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers where a transaction boundary belongs, propagation, isolation, optimistic and pessimistic locking, and what makes borrowing a Copy idempotent, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 37

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering transaction boundaries, propagation and locking
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

`modules/08-transactions-and-consistency.md` is written and follows the
authoring guide's skeleton: What you knew / What changed (delta table, four
rows plus the deliberate no-row note) / How it works now (six subsections) /
The Lab / Quiz (five questions, each naming the delta-table row it asks
about) / Checklist (observable items only).

The table is checked, not remembered. Every cell was read off the published
`spring-boot-dependencies` POMs of 2.7.18, 3.5.16 and 4.1.1; the machinery
claims were read off the jars the build resolves (`spring-boot-transaction`
holds `TransactionAutoConfiguration`, `spring-boot-jdbc` holds
`DataSourceTransactionManagerAutoConfiguration`, and `spring-boot-autoconfigure`
4.1.1 carries none of them); and the JPA transaction manager claim was
settled by reading 2.7.18's `JpaBaseConfiguration` class file — there never
was a `JpaTransactionManagerAutoConfiguration` to lose — and by booting a
4.1.1 application and printing the `PlatformTransactionManager` bean's class
(`JpaTransactionManager`). A throwaway 4.1.1 probe module compiled and ran
every snippet (borrows, loan-limit refusal, conditional-update guard, `@Version`
conflict resolved through Spring's `ObjectOptimisticLockingFailureException`,
`PESSIMISTIC_WRITE` outside and inside a boundary, the `REQUIRES_NEW` audit
write); it lives at a scratch path outside the repository and is not committed.

Propagation and isolation got **no row** because they did not change —
decompiled `Transactional`, `Propagation` and `Isolation` from `spring-tx`
5.3.31, 6.2.19 and 7.0.9 match API for API — and the doc states that absence
as a checked result, the M06 lesson transposed. The Lab is named
(`lab-08-transactions-start`) with its diagnostics command
(`./mvnw -f labs -pl lab-08-transactions-start verify -Pdiagnostics`), the
reader's assumed knowledge is stated in both directions, and the domain
vocabulary is the glossary's and linked throughout. Two factual corrections
fell out of checking against the application: the `CopyNotAvailableException`
handler titles its `409` "Copy is not on the shelf" (not "Copy not
available"), and M05's migration owns `title` and `copy` only — the `loan`
table is a migration the Lab itself adds.

The M08 lab modules (`lab-08-transactions-start` / `-solution`) are not built
yet; the doc points at them as tickets 39-41 will materialise them.
