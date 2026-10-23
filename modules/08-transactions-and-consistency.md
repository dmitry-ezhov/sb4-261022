# M08 - Transactions and consistency

M07 asked what each of the library's tests deserves; this Module asks what the
application's accounting deserves, because right now the answer is "nothing":
the Loan store is a `CopyOnWriteArrayList`, the borrow's guard reads the list
and then writes the row, and between those two statements another request is
welcome to make the same Copy disappear. By the end of it you know where a
transaction boundary belongs, what propagation and isolation are worth in 2026
(nothing has moved - we checked, which is its own lesson), how optimistic and
pessimistic locking differ in code rather than in slides, and what makes
borrowing a [Copy](../GLOSSARY.md) idempotent rather than merely instantiated.

## What you knew

In Boot 2, transactions were one annotation and two habits. The annotation:
`@Transactional` - Spring's own, from `spring-tx` - pasted on service methods,
with the `PlatformTransactionManager` auto-configured underneath: a
`JpaTransactionManager` when you ran JPA, a `DataSourceTransactionManager`
when you ran plain JDBC, and you rarely named either because Boot wired the
one the stack needed. The habits: propagation was the value you copied from
the last review - `REQUIRES_NEW` wherever a rollback had once eaten a log
write - and isolation was always left at the default, because the database's
default was feared and changing it was discussed quarterly. Locking came in
two shapes you knew: `@Version` on an entity column for the optimistic case
and `OptimisticLockException` on the second flush, `@Lock(LockModeType.PESSIMISTIC_WRITE)`
on a repository method for the rows you wanted held, rendering
`select ... for update`. The check-then-act race was not new to you either:
you had met it as a duplicate-booking bug, and the fixes were the same two -
make the guard part of the write, or hold the row while you inspect it.

You are assumed to know all of that fluently. You are **not** assumed to know
that Boot 4 scattered the transaction auto-configuration over three modules
with new names, that the JPA transaction manager's `@Bean` deliberately has
no auto-configuration class of its own in 4.1.1 - so nobody who remembers a
`JpaTransactionManagerAutoConfiguration` from any older course can find one -
that Jakarta Persistence 3.2, the release 4.1.1 builds against, extended what
a `@Version` column may be, or what makes borrowing a [Copy](../GLOSSARY.md)
here idempotent: nothing in the annotation, everything in one `where` clause.

## What changed

The Boot 4 column is the one this course's build resolves - Spring Boot 4.1.1
over Spring Framework 7.0.9, every cell below read off the published
`spring-boot-dependencies` POMs of 2.7.18, 3.5.16 and 4.1.1, off the jars the
build resolves, and - where a *behaviour* had to be seen, not stated - off a
4.1.1 application booting in front of us (see the verified-versions table in
the [lab index](../labs/README.md)). The Boot 2 column is the last Boot 2
(2.7), the Boot 3 column the last Boot 3 (3.5).

|  | Spring Boot 2 (2.7) | Spring Boot 3 (3.5) | Spring Boot 4 (4.1.1) |
|---|---|---|---|
| The transaction annotation | `org.springframework.transaction.annotation.Transactional` from `spring-tx` **5.3.31**; the JTA twin is `javax.transaction.Transactional` (`javax.transaction-api` **1.3**, and 2.7.18 also manages the renamed `jakarta` one, **1.3.3** - the same spec, twin naming) | the same Spring annotation from `spring-tx` **6.2.19**; the JTA twin is `jakarta.transaction.Transactional` (`jakarta.transaction-api` **2.0.1**) | the same Spring annotation from `spring-tx` **7.0.9** - we decompiled it against 5.3.31's jar, and the annotation with `Propagation` and `Isolation` is unchanged, members, defaults and all - and the JTA twin is unchanged: `jakarta.transaction.Transactional`, still **2.0.1** |
| Where `@Transactional` is switched on | `TransactionAutoConfiguration` inside `spring-boot-autoconfigure`, with `JtaAutoConfiguration` beside it and `DataSourceTransactionManagerAutoConfiguration` in the same jar's `jdbc` package | the same arrangement in the same jar | out of it: `spring-boot-transaction` holds the `TransactionAutoConfiguration` and the JTA one, `spring-boot-jdbc` holds the DataSource transaction manager's, and 4.1.1's `spring-boot-autoconfigure` carries none of them - we listed the jar and its cache was empty of transaction classes |
| Where the JPA transaction manager comes from | the `transactionManager` `@Bean` declared on `JpaBaseConfiguration` - there is no `JpaTransactionManagerAutoConfiguration` in 2.7 to have lost, and we read 2.7.18's class file to be sure | the same `@Bean` on the same class | the same `@Bean` on the same class - what moved is the class: `JpaBaseConfiguration` now lives in `spring-boot-jpa` - and we booted a 4.1.1 application and asked its context: the bean resolves to `JpaTransactionManager`, as ever |
| The lock vocabulary | Jakarta Persistence **2.2** - the `javax.persistence.*` names - over Hibernate **5.6.15.Final** | Jakarta Persistence **3.1**, `jakarta.persistence.*` ever since, over Hibernate **6.6.53.Final** | Jakarta Persistence **3.2** over Hibernate **7.4.5.Final** - and 3.2 is the release that adds `java.time.Instant` and `java.time.LocalDateTime` as legal `@Version` types, adds `lockMode()` (default `OPTIMISTIC`) plus `LockOption` overloads to `EntityManager`, and depends `Calendar`, `Date` and `Timestamp` for new applications |

Four rows, and three of them say: *your transaction code does not move.*
The annotation is the annotation; the manager is the manager; propagation and
isolation are the same seven and the same four (that is the row the table
deliberately does not carry - more below). What moves is the module
division underneath, the way M05 showed for persistence and M07 showed for
the test slices, and in the lock vocabulary one quiet spec refresh:

- **The machinery row** is the one that can break a POM at a bare
  dependency. If a Boot 2 codebase pinned `spring-boot-autoconfigure` and
  relied on the transaction auto-configuration being inside it, on 4.1.1 an
  application that declares only `spring-tx` compiles `@Transactional`
  happily and boots a bean that is not transactional at all: the switch-on
  lives in `spring-boot-transaction` now, and the data starters bring it
  transitively - `spring-boot-starter-data-jpa` pulls `spring-boot-starter-jdbc`,
  which pulls `spring-boot-jdbc`, which pulls `spring-boot-transaction`.
  Rely on the starter, and the row costs you nothing.
- **The transport row** is the honest-about-the-past one: 2.7's JPA side
  would not assemble a transaction manager where you might imagine - reading
  the 2.7.18 class file shows `JpaBaseConfiguration` already exposed the
  `@Bean`, exactly as 4.1.1 does. What changed *between* is the module the
  class is packaged in. If you were told "a `JpaTransactionManagerAutoConfiguration`
  disappeared", the disappearance is a myth: you checked, and the claim
  dies.
- **The lock row** is the one with payloads. `@Version` on an `Instant` was
  a Hibernate courtesy that the spec frowned at and now, in 3.2, enshrines -
  so an entity whose optimistic lock is a timestamp no longer needs a
  `Long version` beside it. And the deprecation of the `Date`/`Calendar` API
  is 3.2's version of M05's schema lesson: the `java.time` types are the
  portable ones.

The table has deliberately **no row for propagation and isolation** - a thing
that did not change gets no row, and these two did not: we decompiled
`Transactional`, `Propagation` and `Isolation` from `spring-tx` 5.3.31,
6.2.19 and 7.0.9 and the three API surfaces match. This is the M06 lesson
transposed: the delta table is where changes are stated, and an unchanged
thing is stated by its absence - but only after it was checked, not assumed.
What propagation and isolation *mean* still has to live somewhere, and that
is the third section below, because you last learned it under a Spring
version this course's build cannot even resolve.

One Framework 7 renaming the row cannot carry, because it is off this
course's path: if you drove raw Hibernate `Session` objects rather than JPA,
Framework 7 moved `org.springframework.orm.hibernate5` to
`org.springframework.orm.jpa.hibernate` - `HibernateTransactionManager`
carried over under the move. The library does not use that path, and this
Module says so rather than teach it.

## How it works now

### The machinery is three modules now, and the use case does not care

What a JPA application on 4.1.1 assembles underneath `@Transactional`, in the
order the starter brings it:

1. `spring-boot-starter-data-jpa` pulls `spring-boot-starter-jdbc`, whose
   module `spring-boot-jdbc` auto-configures the `DataSource` and - the part
   you could not see before the modularisation - carries
   `DataSourceTransactionManagerAutoConfiguration` at
   `org.springframework.boot.jdbc.autoconfigure`;
2. `spring-boot-jdbc` pulls `spring-boot-transaction`, and that module is
   what makes the annotation mean anything: its
   `TransactionAutoConfiguration` registers the proxy machinery that
   `@EnableTransactionManagement` used to be typed by hand, at
   `org.springframework.boot.transaction.autoconfigure` - new package, new
   module, same job;
3. the transaction manager itself is the `transactionManager` `@Bean` that
   `JpaBaseConfiguration` has carried since Boot 2 - the class now lives in
   `spring-boot-jpa`, next to the `EntityManagerFactory` builder it
   coordinates with, and 4.1.1 resolves it to a `JpaTransactionManager`; we
   ran the application under `@SpringBootTest` and printed the
   `PlatformTransactionManager` bean's class before claiming it.

Two consequences, one a chore and one a relief. The chore: a codebase that
names the infrastructure - composing its own `PlatformTransactionManager`
bean or setting a `@EnableTransactionManagement` by hand - has to know where
the pieces are, and this Module's rule is the course's rule since M02: name
the narrowest starter that brings what you use, and let Boot do the rest. The
relief: the use case does not care. `@Transactional borrow()` looks
byte-for-byte what it looked under 2.7.18, and the hours of an upgrade go
elsewhere - into the second engine, into the diagnostics - not into the
transaction's plumbing.

```java
@Service
public class LoanService {

    private final LoanStore loans;
    private final LoanProperties properties;
    private final Clock clock;

    LoanService(LoanStore loans, LoanProperties properties, Clock clock) {
        this.loans = loans;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * The boundary is the use case: the guard, the writes, one commit.
     *
     * @throws LoanLimitExceededException if the Member already holds Active
     *         loans up to the limit configuration sets
     */
    @Transactional
    public Loan borrow(Long memberId, Long copyId) {
        if (loans.countByMemberId(memberId) >= properties.limit()) {
            throw new LoanLimitExceededException(properties.limit());
        }
        LocalDate borrowedOn = LocalDate.now(clock);
        // A loan period is a number of days, and a due date is a date.
        long periodInDays = properties.period().toDays();
        return loans.take(copyId, memberId, borrowedOn, borrowedOn.plusDays(periodInDays));
    }
}
```

That method compiled and ran against 4.1.1 as part of checking this doc -
the [Loan limit](../GLOSSARY.md) refusal answered after three borrows had
landed, rows consistent with one commit per borrow. The ids are `Long`
because a database borrow works on database keys: the lab-07 application's
`borrow(String memberId, String copyId)` took the request's external
identifiers and mapped them at the entry points, and from the Lab below -
where the [Loan](../GLOSSARY.md) store moves into the database - the use case
is handed the rows' keys, M05's mapping kept at the edge, never inside the
boundary. (The store's method names below - `take`, `countByMemberId` - are
the probe's, and the Lab's repository renames nothing the borrow's rules
depend on: one "how many" and one "write it" is all a boundary is asked of
this store.) The piece the snippet does not show is the one this Module is
teaching, so the code is the introduction to it, not the end.

### The boundary wraps the use case, and nothing bigger

The transaction boundary is the use case [Loan](../GLOSSARY.md) being
extended, and the rules for placement fall out of what the boundary costs:

- **A boundary holds a database connection, and the pool is finite.**
  HikariCP's `maximum-pool-size` is the pool M05 configured, and every
  transaction in flight pins one of its connections. The boundary therefore
  spans the statements the use case needs and nothing else.
- **A remote call inside the boundary turns a transaction into a round trip**
  - the M04 availability client asks another service before the library
  commits; dragged inside the boundary it would pin a pooled connection
  against the response time of a WireMock stub in the tests and of a
  genuinely remote service in production. The controller calls the
  availability gate as it always has, and only then hands the borrow to the
  service; the region that sits under `@Transactional` begins at
  `LoanService.borrow`, never at the controller (`@Transactional` on a
  controller method would also be read by nobody: the transaction
  infrastructure proxies the beans it is declared on, and the controller's
  annotation would only be honoured if the request went through the proxy -
  which it does not).
- **The boundary does not have to wrap the largest API possible.** What is
  *inside* one borrow is: count the Member's Active loans - the
  [Loan limit](../GLOSSARY.md), the guard - insert the [Loan](../GLOSSARY.md)
  row, and mark the [Copy](../GLOSSARY.md) on loan. Everything else - the
  serialisation, the response, the next request's availability check - is
  outside it, and the code reads the same whether or not Spring is
  underneath, which is the property a review cares about.

The runtime contract you carry forward from Boot 2 is unchanged, because the
row said so: the method returns and the transaction commits; the method
throws and the transaction rolls back - unless the exception is checked and
declared, the `rollbackFor` asymmetry you knew is still there, still worth
one line in the code review (`RuntimeException` defaults to rollback,
checked exceptions default to commit, and the domain's refusals here are
runtime, so the defaults are already the right ones).

### Propagation and isolation: the parts that did not move

The delta table gave these no row, and the check that justified the absence
is stated there: `Transactional`, `Propagation` and `Isolation`, decompiled
from `spring-tx` 5.3.31, 6.2.19 and 7.0.9, match across the three - the
annotation is byte-for-byte what it was, so the *behaviour* questions it
always begged for an answer are still the questions:

- **`REQUIRED` - the default - means "join the transaction in progress,
  else start one."** Its property is exactly what a use case wants: a borrow
  called from anywhere joins the boundary that called it, and a borrow
  called cold opens its own.
- **`REQUIRES_NEW` suspends the outer transaction and opens a second.**
  Its cost is the one a review should ask about: *two* pooled connections
  are pinned for the duration, and a pool of ten exhausts at five concurrent
  requirers - the number is the reason this course's rule is "a log or an
  audit trail the caller's rollback must not take along" and nothing vaguer.
  The audit trail the borrow refusal writes, for instance, wants its own
  transaction so the refused borrow's rollback cannot eat the record:

  ```java
  /** A record the library keeps of its refusals, in its own transaction. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordRefusal(Long memberId, Long copyId) {
      refusals.save(new RefusalRecord(memberId, copyId, LocalDate.now(clock)));
  }
  ```

- **Isolation is the database's per-connection answer to what concurrent
  transactions may observe** - uncommitted writes (`READ_UNCOMMITTED`), a
  snapshot that is re-read (`READ_COMMITTED`), a snapshot that is fixed
  (`REPEATABLE_READ`), or the full fiction of one-at-a-time
  (`SERIALIZABLE`). The value `@Transactional(isolation = ...)` asks for is
  set as the transaction begins, on the connection the transaction borrows -
  the JDBC way to spell it, no transaction-local magic. The default is the
  database's default, which is why the course does not set it anywhere:
  PostgreSQL's `READ_COMMITTED` plus a guard statement placed correctly is
  the pairing the next section argues for, and tightening isolation to fix a
  race the statement should have fixed is doubling the cost of a defect to
  keep it.

### The guard as one statement: what makes borrowing a Copy idempotent

Here is the race the Module exists to close, in the application's own
words. Two requests arrive within the same second from two [Member](../GLOSSARY.md)s,
both asking to borrow the one [Available copy](../GLOSSARY.md) of a big
selling [Title](../GLOSSARY.md). Each runs the same use case, each in its
own boundary Spring opens for it: read the guard, write the row. Whether the
two reads see the same free [Copy](../GLOSSARY.md) is a question about timing,
and the version of `LoanService` above answers it with the wrong order:

```java
// The check-then-act shape: two transactions, one window.
CopyEntity copy = copies.findById(copyId).orElseThrow();      // read 1: on loan = false
if (!copy.isOnLoan()) {
    loans.take(copyId, memberId, borrowedOn, dueOn);          // the Loan row lands...
    copy.setOnLoan(true);                                      // ...and this write races
}
```

Between the read and the write, the task switched - Spring runs the second
transaction, both see `on_loan = false`, and two [Loan](../GLOSSARY.md)s
reference the same [Copy](../GLOSSARY.md). Nobody threw anything; the bug
committed silently. And that is why `@Transactional` around the use case is
necessary and not sufficient - both requests were transactional all along.
What closes it is making the guard and the write **one statement**, where
the database's own atomicity is the guard:

```java
/**
 * Borrows the Copy only if it is still on the shelf: one statement is the
 * whole guard.
 *
 * @return {@code 1} when this caller took the Copy, {@code 0} when someone
 *         else took it in the gap
 */
public int takeOnLoan(Long copyId) {
    return jdbcClient.sql("""
            update copy
            set on_loan = true
            where id = :copyId and on_loan = false
            """)
        .param("copyId", copyId)
        .update();
}
```

That statement ran, as written, against H2 under Flyway's `copy` table (the
one from M05's migration, `on_loan boolean not null default false`), and the
answers were exactly the contract: `1` on the take, `0` on the second. The
`where on_loan = false` clause is what makes borrowing a [Copy](../GLOSSARY.md)
**idempotent** - repeating the request, or racing the request, produces the
same outcome the single successful transaction did: one
[Loan](../GLOSSARY.md) per [Copy](../GLOSSARY.md). The `1` commits the loan
in the same boundary; the `0` is not an error, it is the truth the loser of
the race gets - a refusal carrying the facts `CopyNotAvailableException`
carries, reached *after* the availability service had already said yes - and
the use case translates it into the M06 vocabulary: a `409` problem detail,
`"Copy is not on the shelf"`, in the glossary's words. A third racer - the
one that arrives after the loan commits - is refused earlier and in the same
words, by the same handler.

Note what did not get a `where` clause and what that costs: the Loan row
uses no conditional insert, because the guard lives on the Copy - the
resource the race is *about*. The Loan limit check is a read against the
Member's row, and it takes `@Transactional`'s guarantee but not a special
statement - a member whose Active loans race the limit gets the guard's
protection too, because the count and the insert are in one boundary. It is
the Copy whose availability is contested between strangers, and whose guard
therefore has to be the statement; the limits are contested only through the
same member's requests, and a boundary plus a retry is the honest answer
there (and a retry is a framework feature elsewhere: see M11).

### Optimistic locking: the row remembers

`@Version` - the mechanism - never needed migration, which is why there is
no code to show other than the annotation: a numeric column increment every
write, an `update ... where ... and version = :read` behind every flush, an
exception when the count is zero. What 4.1.1 adds is the spec's own
permission for the version column you wanted to pick anyway: Jakarta
Persistence 3.2 makes `java.time.Instant` and `java.time.LocalDateTime`
legal `@Version` types, so an entity whose optimistic lock *is* its last
touch time no longer carries a redundant `Long version` beside the
timestamp. Same mechanism, wider vocabulary:

```java
    /**
     * The optimistic lock: the row remembers being read, and a write from a
     * stale read is refused at flush, not silently accepted.
     */
    @Version
    private int version;
```

The optimistic conflict is the exception the library sees when a flush has
lost the race, and 4.1.1 answered the "which exception" question the way it
answers every "which class" question - empirically: two detached copies of
the same row, the second's save-and-flush committing, the first's refused at
flush, and the failure appears as Spring Data's
`org.springframework.orm.ObjectOptimisticLockingFailureException` - Spring's
translation of Jakarta's `OptimisticLockException`, the wrapper that lets a
`@ExceptionHandler` translate once for JPA and JDBC alike. The first flush's
evaluator *won* by committing; the loser's write found the row's version
advanced and Spring's translator turned that into the domain's words.

When to prefer optimistic over the last section's statement guard: when the
race the code is guarding cannot be settled by one statement - a write that
spans rows, a rule whose evaluation needs a read first. Optimistic locking
there pays the transaction supervisor's bill *after the fact*, on the
offender, and leaves the hot rows unlocked. But do not reach for it when the
guard statement carries the whole rule: the `where on_loan = false` answer
committed the [Copy](../GLOSSARY.md) before any version comparison was
needed, and optimistic locking layered on top would be the same answer
arriving later and louder.

The one caveat optimistic locking earns its warning about: **an optimistic
lock does not keep the row's other writers waiting, it tells them they
lost.** The exception surfaces at the *caller's* flush, and what the caller
does then is a decision - retry the read and re-apply, or fail. An
application that papers over it by catching the exception and silently
retrying inside a loop is the optimistic-locking equivalent of the
check-then-write race it was introduced to avoid.

### Pessimistic locking: the read that holds the row

The pessimistic path, the *other* place a lock comes from - from the
dreaded read that will be preceded by a writing check - is worth its section
in 4.1.1 for one reason we met empirically: the lock must run inside a
boundary. A pessimistically locked read used outside one - a `@Lock`
annotated method invoked from a fetch-less context - refused to run at all:

```java
/** The read that holds the row: renders {@code for update} on the engine. */
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<CopyEntity> findOneByIdAndOnLoanFalse(Long id);
```

Calling that method outside a transaction failed on 4.1.1 before the
database was even consulted ("No active transaction"). Only
`@Lock`-annotated reads *inside* a boundary ran - and that refusal is the
mechanism being described, not a misconfiguration: a pessimistic lock is a
claim a transaction makes on the row it read, and with no transaction there
is nothing to hold the claim with. The lesson lands earlier than PostgreSQL
would teach it, and the H2 rig - which happily executes `for update` when
handed one - teaches both the rule and the danger away from a server engine
it no longer belongs to, which is exactly why the lock race in this course's
labs runs against Postgres and not H2 - see M05's "Where H2 and PostgreSQL
genuinely differ": this is the difference the in-memory engine cannot
represent honestly.

Pessimistic locking's honest summary is the cost list, in the order a
review asks them:

- **the wait**: a reader who loses the `select for update` equals a write
  blocked on a read lock someone else holds, and duration is out of your
  hands - the lock's expiry is the transaction's.
- **the pool**: the blocking is transaction-shaped, and the queued writers
  are holding idle pooled connections they wait on.
- **the deadlock**: two transactions in opposite orders of the same rows
  are the classic, and the engine resolves it by killing one of them
  outright, which the caller reads as an exception and should treat as a
  retry, not a domain refusal.

The rule this course's sample application follows, and the one this Module
argues for: **the guard statement comes first, pessimistic locking second.**
The borrow is settled by `where on_loan = false` because one row's one flag
is the whole rule; pessimistic locking earns its costs when the rule is
"inspect these rows while holding them against change" - the count-and-hold
of the [Reservation](../GLOSSARY.md) feature, say, where the check is
"nobody has a [Loan](../GLOSSARY.md) for this [Title](../GLOSSARY.md)"
against someone's borrow that may land while you inspect.

## The Lab

The Lab is the library application you last touched in M07 - loans and
reservations asserted at three tiers, its [Loan](../GLOSSARY.md) store still
the `CopyOnWriteArrayList` the `LoanRepository` comment has been promising
to replace since M05: "in memory for now; the transaction lab moves the
store to the database." Everything the borrow asks of the database -
the [Loan limit](../GLOSSARY.md)'s count, the [Copy](../GLOSSARY.md)'s
guard, the [Loan](../GLOSSARY.md) row - is currently in compiled code, so
there is no transaction at all, and two concurrent borrows of one
[Copy](../GLOSSARY.md) both succeed because the code plans them to.

Your job on the Start module is the consistency upgrade, in this order:

1. the Loan feature's calls go to the database - the store becomes a
   repository over a `loan` table the Lab adds its own migration for, the
   way M05's migration owns `title` and `copy`;
2. the [Copy](../GLOSSARY.md)'s guard becomes its one-statement form, and a
   second borrow of the same [Copy](../GLOSSARY.md) - concurrent or merely
   repeated - is refused, not double-lent;
3. the borrow is the transaction boundary: `@Transactional` on the use case,
   the [Loan limit](../GLOSSARY.md) count, the guard and the
   [Loan](../GLOSSARY.md) row inside it, the M04 availability call outside
   it.

The concurrency guarantee is the requirement the diagnostics state, and the
test that proves it fails on the Start module - the race the Start module's
code still allows - and passes on the Solution module's
statement-guarded one. Two concurrent borrows, one [Copy](../GLOSSARY.md):
the winner reports its loan, the loser hears the refusal in the M06
problem-detail words, and the database ends with one
[Loan](../GLOSSARY.md) row for that [Copy](../GLOSSARY.md), not two. The
run that decides:

```bash
./mvnw -f labs -pl lab-08-transactions-start verify -Pdiagnostics
```

The PostgreSQL diagnostics want a Docker daemon - the M05 promise this Lab
pays back - and a skipped run is a no-daemon message, not a pass. The
Solution module is written from scratch, not patched from the Start module:
read it to see how little the *fix* is once the boundary and the guard
statement have the roles the doc gave them, and how much of the diff is
tests that state every claim the code can no longer break.

## Quiz

Five questions about the delta table above and the use case it equips you
to review. Answer first; the answers say why, not just what.

1. **About the machinery row: a Boot 2 service class annotated
   `@Transactional` and a Spring context that works - and on 4.1.1 the same
   code, moved across, runs but `borrow` is not transactional and the
   [Loan limit](../GLOSSARY.md) refusal leaves two loan rows on one
   [Copy](../GLOSSARY.md). The POM compiles. What is on the classpath that
   lets it compile and stops it working - and name the module each half
   lives in on 4.1.1.**

   The annotation and its engine were split. `@Transactional` rides in
   `spring-tx`, which is on the classpath wherever the data starter is, so
   the code compiles and even tests that never race (the `1.4 s` slice kind)
   stay green - nothing fails loudly. But the *body* of the annotation no
   longer happens on 4.1.1 without Boot's own machinery: the proxy that
   opens and commits the boundary is `TransactionAutoConfiguration` in the
   new `spring-boot-transaction` module, reached transitively from the data
   starters, and it is gone from `spring-boot-autoconfigure` (whose jar
   carries none of the transaction classes now). So two fixes at once:
   ensure a data starter is on the classpath so `spring-boot-transaction`
   arrives, and let the `PlatformTransactionManager` be the one Boot
   autowires rather than hand-built. The failure stayed quiet because both
   halves compile - a silent transactionality loss is the reason the
   Delta Table's "checked" habit, in all four rows, is named and not
   implied.

2. **About the boundary - the section that says it wraps the use case and
   nothing bigger: a colleague reviews the Lab's solution and proposes two
   "cleanups" - move `@Transactional` up onto the controller method so the
   request is transactional end to end, and pull the availability call
   into `LoanService.borrow` so the check and the [Loan](../GLOSSARY.md)
   row commit atomically. Both are rejected. Name the mechanism each
   proposition breaks.**

   The first breaks the proxy rule: `@Transactional` is honoured only on
   beans the transaction infrastructure proxies, and the controller is not
   one - Spring MVC invokes the controller method directly, so an
   annotation there is read by nobody, and the end-to-end transaction it
   promises does not exist. The boundary sits on the service's use case
   because that is the bean the proxy wraps. The second breaks the pool:
   the availability call is a remote HTTP round trip, and dragged inside
   the boundary it pins one of HikariCP's `maximum-pool-size` connections
   for the duration - against a WireMock stub in the tests and a genuinely
   remote service in production - so the transactions the pool was sized
   for stop fitting while the round trip runs. The boundary spans the
   statements the database needs and nothing else: the controller asks the
   availability gate, maps the barcode to the
   [Copy](../GLOSSARY.md) row, and only then hands the borrow to
   `LoanService`, where the transaction begins.

3. **About the propagation row - the row the table does not carry: you
   verified `Propagation` and `Isolation` unchanged between 5.3.31 and
   7.0.9 by decompiling. A reviewer says: "so why learn the two again -
   what did not change between the transaction *APIs* is not the same as
   what did not change between transactions *on your pool*." Give the cost
   that the unchanged API hides, in the pool's numbers, and the rule that
   keeps it from becoming an incident.**

   The unchanged API is exactly why the cost hides: `REQUIRES_NEW`'s
   signature, semantics and defaults never moved, so a codebase that treats
   it as free continues to compile, pass its tests and behave badly under
   load. The hidden cost is the second pooled connection - the outer
   transaction's and the new one's - pinned concurrently for the inner
   method's duration. On a pool of ten (the `maximum-pool-size` M05 set),
   five concurrent `REQUIRES_NEW` requirers exhaust the pool not because
   ten transactions run at once but because five pairs of them do; the
   callers who cannot get a connection wait out `connection-timeout`, at
   which point six review-hours become one outage. The rule: `REQUIRES_NEW`
   is not for "it feels safer" - it is for writes that must survive the
   caller's rollback (an audit trail, a refusal the domain committed the
   decision to keep), and nowhere else. That is what makes "what the value
   means" portable across all three Boot versions and "what it costs" the
   thing that never stopped changing, because the pool it drains is the
   user's.

4. **About the transaction annotation - the row that states the annotation
   never moved: two concurrent `borrow`s name the one
   [Available copy](../GLOSSARY.md) of a Title. The guarded statement
   `update copy set on_loan = true where id = :copyId and on_loan = false`
   is the whole fix - but a reviewer objects: "the Loan row is written by
   the *service*, and the service cannot ask the one statement for its
   `Loan limit` count." Where does the count's protection actually come
   from, and which of the two requests ends up writing the Loan row?**

   The count the [Loan limit](../GLOSSARY.md) asks for does not get a
   conditional SQL statement, and it does not need one: it is a read
   against *one Member's* Active loans, and the requests that could race it
   are that Member's own borrows. The protection is the boundary - the
   count and the Loan row's insert are inside the same `@Transactional`
   demarcation - plus the honest acknowledgement that two borrows the same
   Member issues simultaneously are a retry problem, not a database one
   (limits race through one writer; a retry and the M06 words settle it).
   Between two *different* Members - the stranger-on-stranger case the
   statement exists for - exactly one caller's `update` matches a row, and
   the exactly-one answer decides the Loan row's writer: `1` means "your
   transaction commits and the Loan row lands"; `0` means "you lost the
   race" and the use case throws, the handler translates the refusal into
   the M06 problem detail (`409`, "Copy is not on the shelf") - so the loser
   writes *nothing*, not even a refusal row that would need cleaning up
   afterward. That is the annotation row's point read at full length: the
   unchanged annotation's contract is "commit these writes together", never
   "make the write conditional" - idempotency is bought in the statement,
   not in the annotation, and 4.1.1 changes neither.

5. **About the lock row - the row whose `for update` is the claim the rig's
   second engine exists to judge: the Solution module's diagnostics come
   back green on H2 and the PostgreSQL run is skipped, and the sign-off
   says "proven". The solution asserts three claims - the guard's `0` for
   the second borrow, two concurrent borrows cannot both take the Copy,
   and the loser hears a `409` problem detail. Which did the H2 run carry,
   which did it not, and why?**

   H2 carried the first and the third; it carried none of the second. The
   first and third are claims about the application's code and statements
   - H2 executes the conditional update and the M06 handler faithfully for
   one caller at a time, so the `0` answer and the problem detail are
   earned on it. The second is a claim about *concurrent truth* - two
   [Member](../GLOSSARY.md)s racing for one [Copy](../GLOSSARY.md) - and
   H2 is an in-memory engine whose `for update` executes politely for one
   thread at a time - exactly the condition the race exists to violate -
   so a green on H2 says "the code declares the lock", never "the database
   refused a concurrent transaction"; that claim belongs to the PostgreSQL
   refused a concurrent transaction"; that claim belongs to the PostgreSQL
   diagnostics, through Testcontainers, and the skip standing in for them
   is an environment message - no daemon, no engine, no evidence - which
   is a skip, never a pass. The split is the habit the lock row exists to
   teach: name the engine each claim was earned on before signing off.

## Checklist

What a reviewer applies to a solution of this Lab to judge it
production-shaped rather than merely working. Every item is observable -
checkable by reading the change or running a command - and none of them
repeats a gate the build already enforces.

- **The boundary wraps the use case, not the repository.** `@Transactional`
  sits on the service's use case and names that use case; the repository has
  no transaction annotation of its own and is invoked only from inside the
  boundary; a boundary-per-store-call arrangement (every method annotated,
  none composing) is the defect the review names. The annotation's absence
  is itself evidence - the only boundary in the feature, so the data
  layer's isolation reads the same as the service's.
- **The remote call is outside the boundary.** The M04 availability client - the
  HTTP stub - is not invoked from inside `@Transactional`, so the
  transaction spans the statements the database needs and the pool's
  `maximum-pool-size` is not spent waiting on a WireMock or a tempo'd
  service. A read of the code answers in seconds; a stack trace in
  production would take an evening.
- **No transaction is opened on a read-only path.** The feature's reads -
  the barcode mapping the controller does before the boundary, the
  catalogue's Copies of a [Title](../GLOSSARY.md) - run without a boundary
  of their own: the only `@Transactional` in the change is on the use case,
  so a search of the diff answers in seconds, and `readOnly = true` appears
  nowhere as decoration for a query auto-commit serves. The
  [Loan limit](../GLOSSARY.md)'s count is not that path - it is half of the
  guard, inside the boundary with the write it protects, not a read that
  opened a transaction.
- **The guard is one statement, and the answer decides.** The contested
  field ([Copy](../GLOSSARY.md) on loan) is not read-and-written by
  separate calls; the update's `where` clause carries the whole condition
  and the returned row count (1, 0) routes the use case's next sentence. No
  boolean read followed by a set is left anywhere in the same code path.
- **The second borrow is a refusal in the domain's words.** The `0` answer
  surfaces as the `CopyNotAvailableException` the availability feature
  already owns, which the M06 handler translates into a `409` problem
  detail titled in the glossary's words - not a `500`, not a silent second
  [Loan](../GLOSSARY.md), not an `OptimisticLockException` left unhandled
  at the boundary because nobody asked the proxy what it would do.
- **The concurrency guarantee is proved by a failing-then-passing diagnostic.**
  Run it both ways and report what ran:
  `./mvnw -f labs -pl lab-08-transactions-start verify -Pdiagnostics`
  (red - the Start module's race), then the same against the
  `-solution` (green). A skipped PostgreSQL diagnostic is a missing Docker
  daemon, stated as such, and never counted as a pass.
- **The vocabulary is the glossary's, in the schema too.** The [Loan](../GLOSSARY.md)
  row names its columns `member_id` and `copy_id`, the refusal is spoken of
  as a refusal of a [Copy](../GLOSSARY.md) that is not an
  [Available copy](../GLOSSARY.md) - not a "book", not a "checkout", not an
  "item", and the [Loan](../GLOSSARY.md) limit's count is of Active loans,
  never of "loans out".
- **The transaction manager is the one Boot wires.** No hand-built
  `PlatformTransactionManager` bean, no `@EnableTransactionManagement`
  typed in a configuration class, and the module the machinery came from
  is reached via the data starter the labs already declare - a review that
  finds a hand-wired transaction infrastructure in a Spring Boot
  application asks what the starter was thought to be missing.
- **The isolation level is the database default, on purpose, and the doc
  states it.** No `isolation = ...` appears on any `@Transactional` in the
  change, and that absence is the choice this Module's isolation section
  argues for: PostgreSQL's `READ_COMMITTED` plus the guard statement placed
  correctly, not a tighter isolation bought to fix a race the statement
  should have fixed. A reviewer reads the absence against the doc's
  statement rather than reopening the trade.
- **Optimistic-versus-pessimistic is a stated decision, not a habit.** The
  borrow's guard is a statement (the rule is one row's one flag); the
  reservation's hold on a [Title](../GLOSSARY.md) - where the rule spans a
  read and a write against contention - names the mechanism it chose and
  the cost it accepted (the wait, the pool, the retry-on-deadlock), so the
  next review of the same rule can begin from the decision rather than
  reopen it.
