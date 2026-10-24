# 53: M11 doc: Resilience and virtual threads

**What to build:** The Module doc for M11: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers virtual threads on JDK 25, Spring's task execution, the resilience built into Framework 7, timeouts, bulkheads and graceful shutdown, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 52

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering virtual threads, timeouts, retries and graceful shutdown
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

The Module doc was written in the same shape the M10 doc set (the
authoring guide's skeleton, `javap`-cited evidence, a Lab section that
points forward), with every delta-table cell read off the resolved
artifacts rather than remembered.

- **The seven rows split into the three kinds of change, and each kind is
  verified its own way.** The *switch* kind (virtual threads, graceful
  shutdown): `spring.threads.virtual.enabled` is absent from the 2.7.18 jar
  and present from the 3.2 line, default `false` in 4.1.1
  (`org.springframework.boot.thread.Threading` in the base `spring-boot`
  jar); Boot's Tomcat customizer installs
  `org.apache.tomcat.util.threads.VirtualThreadExecutor` with the
  `tomcat-handler-` prefix (bytecode in `spring-boot-tomcat-4.1.1.jar`);
  `server.shutdown` was read across six jars - 2.7.18, 3.2.12 and 3.3.12
  answer `immediate`, 3.4.13, 3.5.16 and 4.1.1 answer `graceful`, so the
  flip is Boot **3.4**'s - and `spring.lifecycle.timeout-per-shutdown-phase`
  is 30s everywhere; and 4.1.1's own change in the shutdown area is
  packaging: `ServerProperties` lives in the new `spring-boot-web-server`
  module. The *renaming* kind: `spring-boot-http-client-4.1.1.jar`'s
  `spring-configuration-metadata.json` carries both spellings, each
  singular entry named deprecated with `"replacement"` and `"since":
  "4.0.0"`, the plural entries unmarked - and the course's own lab-10
  application.yml already carries the plural. The *arrival* kind:
  `org.springframework.resilience` and `org.springframework.core.retry`
  are absent from the 6.2.19 jars and present in 7.0.9's.
- **Retry arithmetic read off the 7.0.9 constant pool and the published
  contract.** `@Retryable` defaults: `maxRetries` 3 (total attempts
  1 + maxRetries), `delay` 1000, `jitter` 0 (added *or* subtracted, never
  below the base delay), `multiplier` 1.0, `maxDelay`
  `Long.MAX_VALUE`, `timeout` 0 ("no timeout applied"),
  `timeUnit` `MILLISECONDS`. The empty-`includes` semantics ("a retry
  attempt for *any* exception"), the `includes` matching nested causes,
  and the advice ordering (retry *outside* `@Transactional`/`@Cacheable`,
  *inside* `@Async`) are quoted from the annotation's published contract -
  they are behaviour statements the bytecode could not prove.
- **The bulkhead row is bytecode-checked.** `ConcurrencyLimit.ThrottlePolicy`
  is an enum with exactly `BLOCK`/`REJECT`, `BLOCK` is the annotation's
  default, and `InvocationRejectedException extends
  RejectedExecutionException` is read off the 7.0.9 class file.
- **The runtime claim was run, not stated.** The lab-10 application
  booted on 4.1.1 with `spring.threads.virtual.enabled=true`
  (Tomcat 11.0.24) starts clean, and `/actuator/threaddump` behind the
  operator credential the M10 chain teaches lists 22 threads and not one
  `tomcat-handler-` worker - the doc turns that absence into the Lab's
  observation.
- **Two JDK/Tomcat statements the bytecode cannot prove are marked as
  doc-read**: JEP 444 (virtual threads finalised in JDK 21) and JEP 491
  (JDK 24 stops `synchronized` from pinning; native frames still pin),
  plus Tomcat's documented "a new virtual thread to execute each task"
  for the executor. The doc says where each leans on documentation.
- **The snippets compile.** The doc's five Java snippets and the
  annotation attributes were compiled verbatim in a scratch 4.1.1-era
  project - spring-context/core/web/aop/beans/expression 7.0.9 plus
  micrometer-core 1.17.1 (Boot 4.1.1's managed version) -
  `javac` answering clean on the final text's formatting.
- **The Lab is named and pointed at.** The doc names
  `lab-11-resilience-and-concurrency-start`/`-solution` (the lab index's
  row 11 names them; tickets 54 and 55 author them), states the three
  moves ticket 54's diagnostics will assert, and gives the diagnostics
  command plus the `verify -Pdiagnostics-solutions` CI form.
- **Glossary.** Member, Copy, Loan, Loan limit, Title and Reservation are
  used and linked to `../GLOSSARY.md`; no synonyms introduced.

## Comments

- **2026-10-24 · agent** — Authored the M11 doc per the skeleton and the
  M10 house style; every table cell verified against the resolved jars
  (Boot 4.1.1 / Framework 7.0.9; Boot 3 column 3.5.16; Boot 2 column
  2.7.18), the two jar-provable behaviours run or read off published
  contracts where the bytecode could not carry them. Doc edit is the
  resolution; no code changed.

