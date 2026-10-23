# M05 - Persistence: JDBC, JPA and migrations

M04 was about the application's edges: the JSON it reads and writes, and the
service it calls. This Module is about the middle of it: where the
[Title](../GLOSSARY.md)s and the [Copy](../GLOSSARY.md)s actually live.
The application until now keeps them in a hard-coded `List`; by the end of
it the schema is a Flyway migration, the reads are repository calls, the connection pool is the same Hikari you tuned in Boot 2 under a new
version number, and you know where the in-memory H2 that every lab runs on
and the PostgreSQL this Lab can run on genuinely differ.

## What you knew

In Boot 2, `spring-boot-starter-data-jpa` brought Spring Data JPA over
Hibernate 5, with entity classes annotated from `javax.persistence.*`, a
`JpaRepository` per aggregate, derived queries from method names, and
`spring.jpa.hibernate.ddl-auto` configured by habit - `create-drop` on the
H2 you ran tests against, `update` on anything longer-lived. For the queries
that did not fit the derivation, `spring-boot-starter-jdbc` brought
`JdbcTemplate`. The connection pool was HikariCP - the default since Boot
2.0 - tuned through `spring.datasource.hikari.*`. Schema was either
`schema.sql` and `data.sql` under `spring.datasource.*` - the spellings
deprecated in 2.5 in favour of `spring.sql.init.*` - or, if you were
disciplined, Flyway with `V<NN>__` scripts in `db/migration`.

You are assumed to know all of that fluently. You are **not** assumed to
know what the modularisation of Boot 4's starters did to the artifacts under
them, what Hibernate 6 and 7 changed, that Boot's Flyway support is a
starter now, where the `@DataJpaTest` slice moved in Boot 4, or what
`JdbcClient` changes about a hand-written query.

## What changed

The Boot 4 column is the one this course's build resolves - Spring Boot
4.1.1 over Spring Framework 7.0.9, every version below checked against the
resolved artifacts, not remembered (see the verified-versions table in the
[lab index](../labs/README.md)). The Boot 2 column is the last Boot 2 (2.7),
the Boot 3 column the last Boot 3 (3.5).

|  | Spring Boot 2 (2.7) | Spring Boot 3 (3.5) | Spring Boot 4 (4.1.1) |
|---|---|---|---|
| The JDBC starter | `spring-boot-starter-jdbc`: `spring-jdbc` 5.3 and HikariCP 4.0.3 in one | `spring-boot-starter-jdbc` | `spring-boot-starter-jdbc` - the name did not change, the contents did: Boot's own `spring-boot-jdbc`, `spring-boot-sql` and `spring-boot-transaction` modules underneath, HikariCP **7.0.2** beside them |
| The JPA starter | `spring-boot-starter-data-jpa`, entities in `javax.persistence.*` | the same name, entities in `jakarta.persistence.*` | the same name - entities in `jakarta.persistence.*` (Jakarta Persistence 3.2), the auto-configuration spread over `spring-boot-jpa`, `spring-boot-hibernate` and `spring-boot-data-jpa` |
| The ORM | Hibernate **5.6.15.Final** | Hibernate **6.6.53.Final** | Hibernate **7.4.5.Final** |
| Hand-written SQL | `JdbcTemplate` | `JdbcTemplate`, and from 3.2 the fluent `JdbcClient` | both still there: `JdbcTemplate` and `JdbcClient`, in `spring-jdbc` 7.0.9 |
| Migrations | Flyway **8.5.13** - you add `org.flywaydb:flyway-core` and Boot's auto-configuration, inside `spring-boot-autoconfigure`, does the rest | Flyway **11.7.2**, same setup | Flyway **12.4.0** - and the trap: the auto-configuration moved into a new module, `spring-boot-flyway`. A bare `flyway-core` configures **nothing** now; you add `spring-boot-starter-flyway` |
| The connection pool | HikariCP **4.0.3**, under `spring.datasource.hikari.*` | HikariCP **6.3.3**, the same keys | HikariCP **7.0.2**, the same keys - three majors of Boot and not one pool property renamed |
| The JPA test slice | `@DataJpaTest` inside `spring-boot-starter-test`, package `org.springframework.boot.test.autoconfigure.orm.jpa` | the same - Boot 3 did not touch this | out of the test starter: the slice lives in its **own artifact**, `spring-boot-data-jpa-test`, in package `org.springframework.boot.data.jpa.test.autoconfigure` |

Three rows are worth a second look, because they are the ones that break a
working POM. **The migrations row is the trap**: `flyway-core` on the
classpath alone is not enough in Boot 4 - we verified it by running a Boot
4.1.1 application with only `flyway-core` declared, and the migration never
ran. The **JPA test slice** row is the same story M01 told about the web
slice: the annotation moved package, and the package it moved to lives in an
artifact `spring-boot-starter-test` no longer pulls in. And the **ORM row**
is the one that makes an upgrade real: two Hibernate majors in two Boot
upgrades, with Jakarta Persistence going 2.2 (`javax`) to 3.1 to 3.2.

## How it works now

### The starters are narrow, and they nest

The rule from M01 holds here too: depend on the narrowest starter that gives
you what you use, and read the starter's own POM before you assume what it
brings. The published POMs (checked for 4.1.1) say exactly this:

- `spring-boot-starter-jdbc` is `spring-boot-starter` plus Boot's own
  `spring-boot-jdbc` plus **HikariCP** - nothing else. It does not give you
  a repository of any kind.
- `spring-boot-starter-data-jpa` is `spring-boot-starter`, `spring-boot-jdbc`,
  `spring-boot-starter-jdbc` and `spring-boot-data-jpa` - so it transitively
  brings the JDBC half and the pool. JPA in a Boot application *is* JDBC in a
  Boot application, plus Hibernate.
- `spring-boot-starter-flyway` is `spring-boot-starter`,
  `spring-boot-starter-jdbc` and `spring-boot-flyway`. The last one is the
  module that makes the migration run in Boot 4; declare the Flyway
  version on its own instead, and H2 stays empty all boot.

A second trap sits one level deeper, surfaced when this lab was run through
its lab on the second engine: Flyway modularised its *database* support with
the same discipline. `flyway-core` (and with it `spring-boot-starter-flyway`)
speaks H2, but not PostgreSQL - booting a 4.1.1 application against a
PostgreSQL 17 container failed with `Unsupported Database: PostgreSQL 17.11`
until `org.flywaydb:flyway-database-postgresql` was on the classpath. The
lab's Solution module carries it, because the whole point of the lab is the
second engine: a lab that runs on PostgreSQL only from its tests, without the
driver and the database module the running application needs, would not be
one configuration.

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
```

The `jakarta.persistence.*` annotations are the same ones Boot 3 gave you -
Jakarta Persistence moved on from 3.1 to 3.2, but the *namespace* did not
move again. If your entities survived Boot 3, they survive Boot 4.

### The schema belongs to the migration, not to Hibernate

The `ddl-auto` default did not move: Hibernate still defaults to
`create-drop` against an embedded database and does nothing otherwise - we
checked it by running the default `@DataJpaTest` slice against H2 with the
migrations switched off, and Hibernate created the tables itself. That
default is the habit that kept Boot 2 tests green, and it is the habit this
course asks you to retire: once there is a `Copy` table that matters,
Hibernate guessing the schema is a defect on its own.

So the division of labour is: **Flyway creates, Hibernate validates**. The
migration is the only thing allowed to shape the database:

```sql
create table title (
    id     bigint generated by default as identity primary key,
    name   varchar(200) not null,
    author varchar(200) not null,
    isbn   varchar(17) not null unique
);

create table copy (
    id       bigint generated by default as identity primary key,
    title_id bigint not null,
    barcode  varchar(50) not null unique,
    on_loan  boolean not null default false,
    foreign key (title_id) references title (id)
);
```

That migration ran, as written, on H2 2.4.240 under Flyway 12.4.0, and - as
written, no variant for the second engine - on PostgreSQL 17. Every type in
it is standard SQL: identity columns, `boolean`, `varchar`. That is the
whole discipline of a shared schema: it is written once, and both engines
take it.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    locations: classpath:db/migration
```

`validate` means Hibernate checks that its entity model matches the tables
the migration made - a mismatch fails startup instead of failing a request.
The dialect, incidentally, is nowhere in the configuration: Hibernate 6 and
7 resolve it from the connection. The log for the check above says
`Database dialect: H2Dialect` without one line about it being configured.

### Repositories: records stay records, entities stay classes

JPA does not speak records - an entity must be a class with accessors, and
the column names it takes come from the field names. The payload type the API
layer serves stays a record. So the [Copy](../GLOSSARY.md) gets two shapes,
and the repository is where the split is made:

```java
@Entity
@Table(name = "copy")
public class Copy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Title this Copy is an item of, by the Title's key. */
    @Column(name = "title_id", nullable = false)
    private Long titleId;

    /** The library's barcode, unique across every Copy. */
    @Column(nullable = false, unique = true)
    private String barcode;

    /** Whether the Copy is currently on loan. */
    @Column(name = "on_loan", nullable = false)
    private boolean onLoan;

    Copy() {}

    public Copy(Long titleId, String barcode, boolean onLoan) {
        this.titleId = titleId;
        this.barcode = barcode;
        this.onLoan = onLoan;
    }

    public Long getId() {
        return id;
    }

    public Long getTitleId() {
        return titleId;
    }

    public void setTitleId(Long titleId) {
        this.titleId = titleId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public boolean isOnLoan() {
        return onLoan;
    }

    public void setOnLoan(boolean onLoan) {
        this.onLoan = onLoan;
    }
}
```

and the query for the [Available copy](../GLOSSARY.md)s of a Title is still
derived from the method name, as it has been since Boot 2:

```java
public interface CopyRepository extends JpaRepository<Copy, Long> {

    List<Copy> findByTitleIdAndOnLoanFalseOrderByIdAsc(Long titleId);
}
```

No implementation, no JPQL, and it ran as written on Hibernate
7.4.5.Final. Derived queries cost you nothing to write and are checked at
startup, which is exactly when a misspelled property should fail.

### Hand-written SQL takes a form now: JdbcClient

For the reads that express no domain concept - "find a work by ISBN" is
catalogue plumbing, not a rule about Loans - the starter of choice is the
plain JDBC one, and the interface in the middle is `JdbcClient`. It has been
in `spring-jdbc` since Framework 6.1 (Boot 3.2); Boot auto-configures a
`JdbcClient` bean as soon as `spring-boot-starter-jdbc` is on the classpath,
so you inject it as any other bean:

```java
@Repository
public class TitleCatalogue {

    private final JdbcClient jdbcClient;

    TitleCatalogue(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** One row of the title table, mapped into the payload type record. */
    public record TitleRow(Long id, String name, String author, String isbn) {}

    public Optional<TitleRow> findByIsbn(String isbn) {
        return jdbcClient.sql("""
                select id, name, author, isbn
                from title
                where isbn = :isbn
                """)
            .param("isbn", isbn)
            .query(TitleRow.class)
            .optional();
    }
}
```

That class - compiled, injected and executed against H2 while being checked
for this doc - is the whole of it: named parameters, a record as the row
type, `Optional` for a miss instead of a `null` or an exception. The row
mapping is what `JdbcTemplate` needed a `RowMapper` for. Boot 4 does not
change this API - it grew in Framework 6.1 (Boot 3.2), which you never
shipped - so it is part of the same three-major drift.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:library
    username: sa
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 2000
```

The keys are the ones you knew, and they are the ones Boot 4 manages
HikariCP 7.0.2 with: `maximum-pool-size`, `minimum-idle`, `connection-timeout`
are all verified to exist in 4.1.1's configuration metadata, spelled exactly
as Boot 2 spelled them. The pool is the one ingredient of this Module whose
upgrades have asked nothing of you since Boot 2 - HikariCP 4 to 7 happened
under the same property names.

### Where H2 and PostgreSQL genuinely differ

The course runs H2 everywhere by default, so that no Docker daemon is
needed - and offers PostgreSQL through Testcontainers on the three data
Labs (this one, M07, M08). The differences that matter are few, but they
are not cosmetic:

1. **Migrations do not mean the same thing on each.** H2 in memory is
   created at every boot, so Flyway replays `V001__` against a fresh
   database each time; against PostgreSQL, the migration runs once and the
   `flyway_schema_history` table remembers. A migration that only works
   when every boot is the first boot hides its own mistakes.
2. **Concurrency looks different.** H2's in-memory engine is a polite
   single-user simulation; row locks, blocking writes and the way a
   `for update` behaves under contention belong to a server engine. That is
   why the transaction Lab (M08) runs locking on PostgreSQL through
   Testcontainers - H2 would green-light code whose race Postgres exposes.
3. **The dialect is being resolved either way.** You never write one in the
   configuration - Hibernate reads it from the connection - so no
   configuration pins your database down. The discipline is the same on
   both: standard SQL in migrations (identity columns, `boolean`,
   `varchar`), no H2 compatibility modes, no dialect-swerving in queries.
   The schema above is the demonstration: one migration, two engines.

What you do not get from H2 in either case is production-shaped data
behaviour: it is the lab rig, not the product.

## The Lab

The Lab is the library application you last touched in M04 - the
availability service already consulted before a Loan is made. Its catalogue
is still the M04 shape: a `@Repository` class holding a `List.of` of
titles and copies in compiled code. Restart the application and the
catalogue is exactly what the builder decided, [Copy](../GLOSSARY.md)s and
all, whatever anyone did to the database.

Your job is the Start module: give the library's catalogue a real database.
The requirement is:

1. a Flyway migration creates the [Title](../GLOSSARY.md) and
   [Copy](../GLOSSARY.md) schema - the one shown above, or a shape like it;
2. the reads go through a repository - derived, or a `JdbcClient` query for
   the catalogue plumbing - and not through a hard-coded `List`;
3. the same module runs unmodified on H2 and, through Testcontainers, on
   PostgreSQL: one configuration, two engines.

The diagnostic tests that state this are excluded from the default build,
so the Start module stays green - and fails them until the job is done:

```bash
./mvnw -f labs -pl lab-05-persistence-start verify -Pdiagnostics
```

The Solution module is written from scratch, not patched from the Start
module: read it after you have made your own solution, to compare decisions
- how it names the schema, where it hands the read to `JdbcClient` instead
of a derivation, and which H2-to-PostgreSQL truths it puts in comments
rather than in code.

## Quiz

Five questions about the delta table above. Answer first; the answers say
why, not just what.

1. **Your POM has `spring-boot-starter-data-jpa`. Does the Boot 4 upgrade
   change that line?**

   No: the starter name survived all three majors - the *web* starter took
   the `spring-boot-starter-webmvc` rename in Boot 4, the data starters did
   not. What changed under the unchanged name are the Boot artifacts - the
   auto-configuration was spread across `spring-boot-jpa`,
   `spring-boot-hibernate` and `spring-boot-data-jpa`, Hibernate goes from
   6.6 to 7.4.5.Final and Jakarta Persistence from 3.1 to 3.2. The lesson,
   checked against 4.1.1 rather than remembered: most starters did not
   get renamed, and the renames that happened (the web one) happened for
   specific reasons, not as a sweep.

2. **You do what worked in Boot 2 and Boot 3: add `org.flywaydb:flyway-core`
   and expect Boot to run the migrations. What happens on Boot 4?**

   Nothing runs. The Flyway auto-configuration used to live in
   `spring-boot-autoconfigure`; it has been extracted into a module of its
   own, `spring-boot-flyway`, and that module only arrives via
   `spring-boot-starter-flyway`. Flyway 12.4.0 itself is still what Boot
   manages, and the migration paths still default to `db/migration`. We
   verified the silence empirically: a 4.1.1 application with bare
   `flyway-core` started with an empty H2 and no Flyway logging.

3. **About the migrations row: a codebase from Boot 2 has `schema.sql`
   and `data.sql`, guarded to run only for the H2 profile. Why does this
   course make them Flyway migrations instead?**

   Because the default mode of script-based initialization - verified in
   4.1.1's configuration metadata - is `spring.sql.init.mode=embedded`, and
   it has been `embedded` since Boot 2 introduced the property in 2.6: the
   scripts run against in-memory databases only. That default quietly made
   `schema.sql` and `data.sql`
   the tool for the lab rig and never for the real database (the Boot 2
   spellings `spring.datasource.schema` and `spring.datasource.data` were
   already deprecated in 2.5), and let the
   two schemas drift apart. A `V001__` migration runs against every
   engine, with Flyway remembering what has been applied - and DDL under
   version control is what makes the indices, the foreign keys and the
   unique constraints the same work on the lab rig that they are in
   production.

4. **Three Boot majors ago you wrote `spring.datasource.hikari.*` keys.
   Do they survive Boot 4, and what did the upgrades give you?**

   Virtually the same keys - `maximum-pool-size`, `minimum-idle`,
   `connection-timeout`, verified present in 4.1.1 - but under HikariCP
   7.0.2 instead of 4.0.3. The durability of that property layer is the
   one part of persistence where Boot asked you to learn nothing new
   across three majors, while the ORM on top of it changed majors twice.
   The upgrade story is not uniform; the version moves only where
   something had to.

5. **Your Boot 3 slice test imports**
   `org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest`.
   **What does the Boot 4 compilation change?**

   Both the package and the artifact. In 4.1.1 the annotation lives at
   `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
   which comes from `spring-boot-data-jpa-test` - a dependency
   `spring-boot-starter-test` stopped shipping, along with the data
   slices. Two moves to make: the import and, in the POM, a
   `<scope>test</scope>` dependency of `spring-boot-data-jpa-test`. Note
   that the *slice* itself never stopped working: once the two paths are
   fixed, the slice swaps in the embedded DataSource and binds the
   repositories exactly as it did in Boot 2 and Boot 3, with no other
   edits.

## Checklist

What a reviewer applies to a solution of this Lab to judge it
production-shaped rather than merely working. Every item is observable -
checkable by reading the change or running a command - and none of them
repeats a gate the build already enforces.

- **The catalogue is the database.** The reads are repository calls or
  `JdbcClient` queries; no compile-time `List.of` of titles or copies, no
  field that is initialized once and never consulted again.
- **The schema belongs to the migration.** A `V001__` Flyway script creates
  the Title and Copy tables, with the foreign key between them; `ddl-auto`
  is `validate`, not `update` or `create-drop` set in the application
  configuration, and `spring.sql.init.*` is untouched. H2's create-drop
  default may run against a test slice; the application never relies on it.
- **The failure behaviour survived the persistence swap.** A Title that
  does not resolve is still a `404` problem detail, not a `500` with a
  stack trace from the data layer: the Optional a repository or a
  `JdbcClient` query returns stays an Optional until the service decides,
  and the exception handler from M04 gets a chance to translate it.
  A `500` whose stack trace starts in `org.hibernate` is the defect M04
  was written to prevent, wearing a data-layer coat.
- **One schema, two engines.** The migration answers "does this run the
  same on H2 and PostgreSQL": only standard types - identity for the
  primary keys, `boolean not null` where on loan is needed - no
  H2-only `LIMIT` syntax, no `MODE=PostgreSQL` compatibility setting, and
  no dialect in the configuration.
- **The vocabulary is the glossary's, in the database too.** Table and
  column names are written in the glossary's terms - `title`, `copy` with a
  `title_id` - never a `book_id`, a `member_id` or an `item`. The words are
  [Title](../GLOSSARY.md), [Copy](../GLOSSARY.md), [Member](../GLOSSARY.md),
  [Loan](../GLOSSARY.md),
  [Available copy](../GLOSSARY.md) - a Copy that is not on loan - and the
  repository method names use the same words: `findByTitleId` is fine,
  `getItemsByBook` is not.
- **The entity/payload split is an intentional seam.** The JPA entities are
  classes with accessors, and they do not leave the persistence layer:
  the API returns records - the shape the JSON contract has been speaking
  since M04. A controller returning an `Entity` would silently break the
  media types, lazy-fetching, and the record-based payload policy in one
  line diff.
