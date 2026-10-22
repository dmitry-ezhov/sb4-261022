# 23: M05 doc: Persistence: JDBC, JPA and migrations

**What to build:** The Module doc for M05: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers the JDBC and JPA starters, Spring Data repositories, schema migrations, connection pools, and where H2 and PostgreSQL genuinely differ, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 22

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering the JDBC and JPA starters, migrations and connection pools
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

Built in the commit that added `modules/05-persistence.md`.

- **The Module doc** covers the JDBC starter, the JPA starter, the ORM
  versions, hand-written SQL (`JdbcTemplate`/`JdbcClient`), Flyway
  migrations, the HikariCP connection pool and the `@DataJpaTest` slice -
  each as a row of the Boot 2 → Boot 3 → Boot 4 delta table - and then how
  they work in 4.1: the starter POMs' nesting, schema ownership
  (Flyway creates, Hibernate validates), repositories vs records, the
  `JdbcClient` form, the pool properties, and where H2 and PostgreSQL
  genuinely differ.
- **Every Boot 4 claim in the delta table was checked, not remembered**,
  against the artifacts the build resolves and the published POMs:
  - `spring-boot-starter-jdbc` 4.1.1's own POM lists `spring-boot-jdbc`,
    `spring-boot-jdbc`'s own deps (`spring-boot-sql`,
    `spring-boot-transaction`, `spring-jdbc` 7.0.9) and HikariCP 7.0.2 -
    and `spring-boot-starter-data-jpa`'s POM shows it is
    `spring-boot-starter-jdbc` plus `spring-boot-data-jpa`, which is
    `spring-boot-hibernate` (Hibernate 7.4.5.Final) plus
    `spring-data-jpa`;
  - `spring-boot-dependencies` 4.1.1 manages Flyway 12.4.0, H2 2.4.240,
    Hibernate 7.4.5.Final and Jakarta Persistence 3.2.0; the Flyway
    auto-configuration lives in the new `spring-boot-flyway` module
    (package `org.springframework.boot.flyway.autoconfigure`), reachable
    through the new `spring-boot-starter-flyway` - a bare `flyway-core`
    configured nothing, verified by running a 4.1.1 application with only
    `flyway-core` declared (the migration never ran) and then with the
    starter (it ran);
  - Boot 2.7.18 and Boot 3.5.16's own published POMs give the historical
    columns: HikariCP 4.0.3/6.3.3, Hibernate 5.6.15.Final/6.6.53.Final,
    Flyway 8.5.13/11.7.2, `javax`→`jakarta` Persistence 2.2.3/3.1.0;
  - `spring.datasource.hikari.*` keys (`maximum-pool-size`,
    `minimum-idle`, `connection-timeout`) are unchanged in 4.1.1's
    configuration metadata, and `spring.sql.init.mode` still defaults to
    `embedded` (`org.springframework.boot.sql.autoconfigure.init
    .SqlInitializationProperties`, in the new `spring-boot-sql` module);
    Boot 2.5.0 deprecated `spring.datasource.schema` and 2.6 introduced
    `spring.sql.init.mode` - checked in the metadata of 2.5.0, 2.6.15 and
    2.7.18;
  - the `@DataJpaTest` slice moved: it is absent from
    `spring-boot-test-autoconfigure` 4.1.1 and present in
    `spring-boot-data-jpa-test` 4.1.1 under
    `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
    while 3.5.7's `spring-boot-test-autoconfigure` still carries it under
    `org.springframework.boot.test.autoconfigure.orm.jpa`.
- **Every snippet was compiled and executed** in a scratch module on
  `spring-boot-starter-parent` 4.1.1 with JDK 25: the `Copy` entity and
  `CopyRepository` derivation, the `TitleCatalogue`/`JdbcClient` query, the
  `V001__create_title_and_copy.sql` migration and the Hikari keys. The
  migration ran on H2 2.4.240 under Flyway 12.4.0 with `ddl-auto=validate`
  passing, on PostgreSQL 17 unchanged (verified in a throwaway container:
  both tables, the constraint, the inserts and both queries), and the
  default `@DataJpaTest` slice on H2 with the migrations off created the
  schema itself, confirming `create-drop` remains the embedded default.
- **The Lab** is named with its two module names
  (`lab-05-persistence-start` / `-solution`) and the learner's command,
  `./mvnw -f labs -pl lab-05-persistence-start verify -Pdiagnostics`.
- **The assumed/not-assumed split** is stated in "What you knew": fluent in
  the Boot 2 persistence stack; not assumed to know the starter
  modularisation, Hibernate 6/7, `JdbcClient`, the Flyway starter, or where
  the JPA test slice went.
