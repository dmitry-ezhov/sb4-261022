# 24: M05 lab start: Persistence: JDBC, JPA and migrations

**What to build:** The Start module for M05: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers replacing the in-memory catalogue with a real Title and Copy schema, a repository and a migration, running the same lab on H2 and on PostgreSQL through Testcontainers.

**Blocked by:** 23

**Status:** resolved

- [x] the Start module exists and is listed in the labs aggregator
- [x] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [x] its diagnostics fail, and each failure message names what is missing
- [x] the default build stays green; the diagnostics run is red
- [x] the application is the one the previous Module left, extended rather than rewritten

## Comments

- **2026-10-22 · acceptance audit** — Not started, but there is scratch work in
  the working tree: `labs/lab-05-persistence-start/` and
  `labs/lab-05-persistence-solution/` hold a copy of the M04 application
  (both sides are byte-identical, and neither has a POM, a diagnostics
  package, a schema or a migration). They are not in `labs/pom.xml`, so the
  reactor does not see them. Treat them as a starting point for the M05 POM
  and the schema, not as a half-finished Lab.

## Answer

Built in the commit that added `labs/lab-05-persistence-start`, listed in
`labs/pom.xml` by the same commit (the scratch directories named in the audit
held nothing but empty `src` trees, so the module was authored from the M04
solution pair directly).

- **The Start module** is the M04 application verbatim - the availability
  service consulted through the HTTP Service client, RFC 9457 problems, the
  M04 tests - plus two diagnostics classes and the test-scoped classpath the
  diagnostics need (`spring-boot-starter-jdbc`, `h2`,
  `testcontainers-postgresql`, `spring-boot-testcontainers` - the same
  convention as lab 04's test-scoped WireMock). Nothing persistence-shaped
  appears on the main classpath: no starter, no `spring.datasource` keys, no
  migration, no repository. The `book/TitleRepository` defect the M05 doc
  describes is there as it stands, with its "replaced by a real database in
  the persistence module" comment.
- **Two diagnostics classes** in `com.example.library.diagnostics`:
  - `PersistenceDiagnosticsTest` (H2): `theSchemaIsOwnedByAMigration`
    requires an applied-migrations row in the quoted-lowercase
    `"flyway_schema_history"` plus the `title` and `copy` tables, and
    `theCatalogueIsTheDatabase` writes a Title and an Available Copy
    straight into the database and requires them to surface through
    `GET /api/titles` and `GET /api/titles/{id}/copies` - the requirement
    the M04-shaped compiled `List` cannot meet. The class points the
    application at an `h2:mem` database of its own via
    `@DynamicPropertySource`.
  - `PersistenceOnPostgresqlDiagnosticsTest`: a static
    `@Container @ServiceConnection` PostgreSQL 17 Testcontainer, and the
    same migration/reads checks against it - one module, two engines. A
    Docker daemon-less machine skips it (`@EnabledIfDockerAvailable`) and
    works the lab on H2 alone.
- **Verified both ways.** Default build `./mvnw -f labs -pl
  lab-05-persistence-start -am verify`: green, 8 tests plus the packaged-jar
  IT, every gate except the floor. `... verify -Pdiagnostics`: red 3/3, and
  each failure names its requirement - the flyway-starter trap, the missing
  migration tables, the compiled `List`, the un-migrated PostgreSQL.
  Diagnostics were additionally graded against a throwaway implementation
  (starter-jdbc/flyway + a `JdbcClient` repository + the doc's migration) to
  prove they are satisfiable; two findings took effect and are recorded on
  ticket 25. The committed module contains none of that scratch code.
- **Not included** (belongs to the Solution module, ticket 25): the JPA
  starter, the migration, the repository reads, `ddl-auto: validate`, the
  `spring.datasource` configuration, and the PostgreSQL diaspora of the
  Flyway support artifact.
