# M09 - Actuator and observability

M08 settled the library's *accounting* - the borrow commits as one boundary,
the guard is one statement, two racing [Member](../GLOSSARY.md)s cannot both
take the one [Available copy](../GLOSSARY.md). This Module asks a different
question about the same running application: **what does it say while it
runs** - is it alive, is it ready for traffic, how many
[Loan](../GLOSSARY.md)s did it issue this morning - and who is allowed to
ask. By the end of it you know what Boot 4's actuator starter no longer
contains and where the pieces went, why the liveness and readiness probes
answer before you configure a thing in 4.1.1 (they did not in any Boot you
used before), how a metric about loans is published and read back, how
structured logging and OpenTelemetry reached the starter, and what must
never be exposed to a stranger.

## What you knew

In Boot 2, observability was the actuator and one number per question. The
starter: `spring-boot-starter-actuator`, which pulled
`spring-boot-actuator-autoconfigure` and `micrometer-core` - and nothing was
split anywhere, because there was nothing to split: the health endpoint, the
metrics machinery, the web trace store and every other endpoint lived in
Spring Boot's one monolith. The numbers: a `MeterRegistry` you injected and
fed with `counter(...).increment()`, the exporter of choice being Prometheus
(`management.metrics.export.prometheus.enabled` in 2.7's spelling), and the
exposure line you memorised once and carried for years:
`management.endpoints.web.exposure.include=health,metrics`. The probes: Boot
2.3 turned liveness and readiness into two health sub-paths,
`/actuator/health/liveness` and `/actuator/health/readiness`, enabled
automatically when the process found itself on Kubernetes and available
anywhere else only after you wrote the property. Health details stood behind
`show-details=never` unless you deliberately changed it. Structured logging:
not a Boot concern - you pasted `logstash-logback-encoder` into the POM and
tuned a `LogstashEncoder` by hand. Tracing: not a Boot concern either -
Micrometer Tracing was years away, and the honest Boot 2 answer was "logs,
metrics, and hope".

You are assumed to know all of that fluently. You are **not** assumed to know
that Boot 4 split the actuator's contents over at least five modules and
moved `org.springframework.boot.actuate.health` out from under every custom
indicator ever written against it, that 4.1.1 turns the probes **on by
default on every platform** - the bytecode is `matchIfMissing = true`, and we
booted 4.1.1 on a laptop and read `200` off liveness with no Kubernetes in
sight - that the `httptrace` endpoint stopped being `httptrace` in Boot 3.0,
that structured logging has been a two-line property in Boot since 3.3, or
what Boot 4's own answer to "I want the OpenTelemetry SDK wired" looks like:
a starter of its own, `spring-boot-starter-opentelemetry`, and a
`management.opentelemetry.logging` namespace a Boot 2 memory would never
guess.

## What changed

The Boot 4 column is the one this course's build resolves - Spring Boot 4.1.1
over Spring Framework 7.0.9, every cell below read off the published
`spring-boot-dependencies` POMs of 2.7.18, 3.5.16 and 4.1.1, off the starter
POMs' declared dependencies, off the actuator jars we listed and decompiled
(`@Endpoint(id=...)` annotations read out of the class files), and - where a
*behaviour* had to be seen, not stated - off a 4.1.1 application booting in
front of us with only `spring-boot-starter-webmvc` and
`spring-boot-starter-actuator` on the classpath (see the verified-versions
table in the [lab index](../labs/README.md)). The Boot 2 column is the last
Boot 2 (2.7), the Boot 3 column the last Boot 3 (3.5).

|  | Spring Boot 2 (2.7) | Spring Boot 3 (3.5) | Spring Boot 4 (4.1.1) |
|---|---|---|---|
| The actuator starter | `spring-boot-starter-actuator` pulls `spring-boot-actuator-autoconfigure` and `micrometer-core` **1.9.17** - three artifacts, the whole observability story in one | the same starter shape, but the managed jars are trace-neutral now: `micrometer-observation` and `micrometer-jakarta9` **1.15.12** ride along with `spring-boot-actuator-autoconfigure` | the starter pulls `spring-boot-starter-micrometer-metrics` (whose contents are the module `spring-boot-micrometer-metrics` and `micrometer-core` **1.17.1**), `spring-boot-health`, `spring-boot-actuator-autoconfigure`, and `micrometer-observation` + `micrometer-jakarta9` **1.17.1** - four of the six pulled artifacts are Boot's own new modules |
| Where the endpoint code lives | one `spring-boot-actuator` jar: `actuate.health`, `actuate.metrics`, `actuate.trace.http`, `actuate.cache`, `actuate.quartz`, `actuate.session`, `actuate.flyway`, `actuate.liquibase`, `actuate.integration`, and twenty three `@Endpoint` classes in all | still one jar - every endpoint of 2.7's set, with `trace/http/HttpTraceEndpoint` already replaced by `web/exchanges/HttpExchangesEndpoint`, plus the `sbom` endpoint 3.3 added | `spring-boot-actuator` keeps **15** core endpoints; the health endpoint and every indicator type moved into `spring-boot-health`; the metrics endpoint and every exporter into `spring-boot-micrometer-metrics`; `caches`, `quartz`, `sessions`, `flyway`, `liquibase`, `integrationgraph` travel with each feature's own module - we listed the jars and counted both sides |
| The web-trace endpoint | id `httptrace`, property prefix `management.trace.http.*` | id `httpexchanges` - renamed in 3.0 - and property prefix `management.httpexchanges.recording.*`, with the old keys kept as *error-level* aliases naming their replacements | unchanged from Boot 3: same id, same properties, same alias policy - we read `@Endpoint(id="httpexchanges")` off the 4.1.1 class file to be sure |
| Liveness and readiness | `management.endpoint.health.probes.enabled` defaults to **false**, and a hand-written `ProbesCondition` matches the property *or* auto-enables when `CloudPlatform.getActive(...)` says "running on Kubernetes" - the strings are in the 2.7.18 bytecode | the same condition, re-compiled: property defaults false, Kubernetes auto-enables | the condition is rewritten: `AvailabilityProbesAutoConfiguration` in the new `spring-boot-health` module is `@ConditionalOnBooleanProperty("management.endpoint.health.probes.enabled", matchIfMissing = true)` - **on by default on every platform; and we booted 4.1.1 on a plain laptop and liveness answered `200` UP with no configuration** |
| Who may read what by default | web exposure: `health` only (`info` left the default in 2.5); **JMX exposes every endpoint (`*`)** | web: `health` only; JMX: `health` too - the `*` default is gone | unchanged from Boot 3: web `health`, JMX `health` - read off each jar's configuration metadata, not remembered |
| A custom health indicator | `org.springframework.boot.actuate.health.HealthIndicator`, returning `org.springframework.boot.actuate.health.Health` | the same packages | **moved**: `org.springframework.boot.health.contributor.HealthIndicator` and `Health`, types of the `spring-boot-health` module - `org.springframework.boot.actuate.health` is a package 4.1.1 does not contain, and the starter brings the replacement transitively |
| The metrics consent | `management.metrics.export.<id>.*` is the primary spelling (`management.metrics.export.prometheus.enabled`) | renamed in 3.0 to `management.<id>.metrics.export.*`; the 2.x spelling survives as an *error-level* deprecation alias naming its replacement - both spellings sit in the 3.5 jar's metadata and only the second applies | unchanged from Boot 3: same namespace, same aliases - the 4.1.1 `spring-boot-micrometer-metrics` metadata still carries the 2.x spellings as `deprecated: error` |
| Structured logging | none - the string `logging.structured` appears nowhere in the 2.7.18 core jar's metadata | built into Boot 3.3: `logging.structured.format.console` / `.file` over `logging.structured.json.*` | unchanged from Boot 3 - the same keys, and the same three format ids, which we read out of the 4.1.1 core jar's `CommonStructuredLogFormat` (`ecs`, `logstash`, `gelf`) and then observed line by line with the console set to `logstash` |
| Tracing and OpenTelemetry | nothing: the 2.7.18 dependency POM names `opentelemetry` **zero times** - Micrometer Tracing is a Micrometer feature Boot 2 never wired, and Sleuth was the separate consumer dependency | wired: `spring-boot-actuator-autoconfigure` carries the `tracing` and `opentelemetry` auto-configurations, the namespace is `management.tracing.*`, OTel SDK **1.49.0** is managed, and the OTLP *log-record* export lives inside the actuator at `management.otlp.logging.*` | split out: `spring-boot-micrometer-tracing` (+ `-brave`, `-opentelemetry`) and the SDK wiring in `spring-boot-opentelemetry`, over micrometer-tracing **1.7.1** and OTel **1.62.0**; the bundling starter is `spring-boot-starter-opentelemetry` (metrics + tracing + log export), the Brave route is `spring-boot-starter-zipkin`, and `management.otlp.logging.*` became an error-level alias of `management.opentelemetry.logging.export.otlp.*` since 4.0 |

Nine rows, and the sum is not M08's "nothing moved": observability is where
Boot 3 did the real wiring and Boot 4 kept rearranging the packaging on top
of it. The rows worth re-reading at full length:

- **The starter row** is the arithmetic one. The name
  `spring-boot-starter-actuator` resolved to the same two Boot artifacts in
  Boot 2 and Boot 3; on 4.1.1 its dependency list is four Boot-owned modules
  plus the micrometer pair - the *plumbing* that stays (endpoint machinery,
  web mapping, audit, info, env, shutdown, heap dump), the *metrics pipeline*
  via `spring-boot-starter-micrometer-metrics`, and the whole health story
  via `spring-boot-health`. `micrometer-observation` and
  `micrometer-jakarta9` complete the list - the artifact pair whose shape
  Boot 3.5 introduced and 4.1.1 merely re-versioned to 1.17.1.
- **The location row** is M02's *one responsibility, one module* transposed.
  In 2.7 `spring-boot-actuator` carried everything from health to Flyway
  inspection in one jar; on 4.1.1 we counted fifteen `@Endpoint` classes left
  in that jar and the rest redistributed - health into `spring-boot-health`,
  metrics and every exporter into `spring-boot-micrometer-metrics`, and the
  per-feature endpoints (`caches`, `quartz`, `sessions`, `flyway`,
  `liquibase`, `integrationgraph`) alongside the feature code they inspect,
  in the module the feature's own starter brings. The 3.5 jar carries all of
  2.7's packages with `trace/http` already renamed in place - the endpoint
  rename predates 4 by two majors, and Boot 3 renamed nothing else.
- **The probe row** is the behaviour change the Lab below leans on. Boot 2
  and Boot 3 hid liveness and readiness behind a condition that decompiles
  to: *the property, or Kubernetes*. Boot 4's
  `AvailabilityProbesAutoConfiguration` is a single
  `@ConditionalOnBooleanProperty(..., matchIfMissing = true)` - no platform
  question anywhere in the bytecode, because the property's default is the
  answer and the default is now **true**. We proved it the way both this
  doc's mode and M08's insist on: bytecode first, then a boot - the
  4.1.1 application answered `200` on `/actuator/health/liveness` and
  `/actuator/health/readiness` with no configuration and no orchestrator.
- **The spelling rows** are the quiet land mines, and both follow the alias
  pattern Boot 3 established: a renamed *property* is not deleted, it stays
  in the metadata as `deprecated: error` with its `replacement` named, so a
  Boot 2 configuration that reaches 4.1.1 stops the boot with the new
  spelling written down - decode it once and move on. The metrics consent
  re-spelled in 3.0, the OTLP log export re-spelled in 4.0, and the one
  *endpoint* rename in 3.0 changed both its id (`httptrace` ->
  `httpexchanges`) and its property prefix in the same release.

The table deliberately carries **no row for the web exposure default** - a
thing that did not change gets no row, and `management.endpoints.web.exposure.include`
resolves to `['health']` in the configuration metadata of all three columns,
a check we ran per jar rather than assumed. What *did* move on the exposure
axis is the JMX half: 2.7 exposed **every endpoint over JMX by default**
(`*`), and Boot 3 cut that default to `health` and 4 kept it. And the health
details default is `never` in all three columns
(`management.endpoint.health.show-details` read off each jar), which is why
the "what must never be exposed" section below is about habits, not about a
door Boot 4 suddenly left open: the safe state, in every Boot this course's
build resolves, is the state the starter hands you.

One Micrometer renaming the table cannot carry, because it is off this
course's path: the server adapters. Boot 2's `micrometer-core` managed a
`javax.servlet`-era set and Boot 4 standardises on `micrometer-jakarta9` -
the Jakarta migration you met in M01 reached the observability starter too.
The library runs Tomcat and has no second servlet stack, and this Module says
so rather than teach the adapters.

## How it works now

### The starter is the same name and a different contents list

What `spring-boot-starter-actuator` 4.1.1 declares, read off its POM:

1. `spring-boot-starter` - the base, as ever;
2. `spring-boot-starter-micrometer-metrics` - the Boot 4 starter whose
   contents are the `spring-boot-micrometer-metrics` module: the
   `io.micrometer.core.instrument.MeterRegistry` wiring, the composite
   registry arrangement, the `/actuator/metrics` endpoint, and every metrics
   *exporter* auto-configuration (Prometheus among them, at
   `management.prometheus.metrics.export.*`);
3. `spring-boot-actuator-autoconfigure` - the thin remainder after the
   split: the endpoint machinery and web mapping, audit, info, env,
   configprops, shutdown, `heapdump`/`threaddump`, the `sbom` endpoint,
   scheduled-tasks and startup reporting - the fifteen endpoint classes the
   location row counted;
4. `spring-boot-health` - the new home of the row above: the health endpoint
   and group machinery, the availability-state indicators
   (`LivenessStateHealthIndicator`, `ReadinessStateHealthIndicator`), and
   the *types* a custom indicator codes against -
   `org.springframework.boot.health.contributor.HealthIndicator` and `Health`;
5. `micrometer-observation` 1.17.1 and `micrometer-jakarta9` 1.17.1 - the
   trace-neutral observation API and the Jakarta servlet adapter, the one
   pair whose shape Boot 3.5 already carried.

The practical consequence is the lesson M02 taught under another name: **the
starter opens the actuator's plumbing; the piping - traces, the OTel SDK, the
export of log records - is still your decision**. The two starters 4.1.1
provides for that decision:

- `spring-boot-starter-opentelemetry` - the full pipeline in one line: it
  declares the metrics starter, tracing through Micrometer Tracing's OTel
  bridge (`micrometer-tracing-bridge-otel` and the module
  `spring-boot-micrometer-tracing-opentelemetry`), the OTLP trace exporter
  (`opentelemetry-exporter-otlp`, **1.62.0**), and the
  `spring-boot-opentelemetry` module whose log-export auto-configuration
  answers at `management.opentelemetry.logging.*`.
- `spring-boot-starter-zipkin` - the Brave road: the
  `micrometer-tracing-bridge-brave` adapter with Zipkin's reporter, read off
  the starter's POM, for the team whose tracing backend is not an OTLP
  endpoint.

And nothing is sent anywhere out of the box: the exporters are gated behind
their configuration (the conditions we read off the auto-configurations,
`ConditionalOnEnabledTracingExport` and friends), so a starter that arrives
with no endpoint configured publishes the *Observation* layer and no bytes -
which is the right default for a lab that boots on a laptop.

### Liveness answers "restart me?"; readiness answers "route me?"

Liveness and readiness are two questions about one process, and 4.1.1 keeps
them *states* first and HTTP endpoints second:

- **Liveness** - should the platform keep this process running, or replace
  it? The state is `org.springframework.boot.availability.LivenessState`,
  with exactly two values, both read off the 4.1.1 class file: `CORRECT` and
  `BROKEN`. The process answers liveness about *itself*: the bits it runs on
  are intact, whether it can serve what it wants to serve is not the
  question. A liveness check that ends in "is the database reachable?" is
  the defect this course names in one line: it wires the platform's
  *restart* decision to a dependency outage, so a temporarily broken
  database turns "route around it for a minute" into "restart a process
  whose own code is fine, forever". Liveness has no
  database, no network call, and no availability-client inside it - the
  probe is the availability state the process itself holds.
- **Readiness** - are you willing and able to receive traffic? The state is
  `ReadinessState`, again two values read off the class: `ACCEPTING_TRAFFIC`
  and `REFUSING_TRAFFIC`. This is the state that moves when the application
  warms up, when it has been told to leave the load balancer for a rolling
  deployment, and when a temporarily unavailable dependency should stop
  routing rather than restart. It is the platform's *routing* decision, not
  its life decision.

The state moves by event, not by boolean bookkeeping. The availability
registry lives in `spring-boot`'s core
(`org.springframework.boot.availability.ApplicationAvailability`, in place
since Boot 2.2 and the one availability class that never moved), and you
publish a change through the one API:

```java
/**
 * The library refusing or accepting traffic is an event, not a boolean:
 * readiness is published so every listener - the read probe above all -
 * reads the same thing.
 */
void beginMaintenance(ConfigurableApplicationContext context) {
    AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
}
```

The probe endpoint and the availability indicators translate the published
state: a readiness that has stopped accepting answers `OUT_OF_SERVICE`, not
`DOWN` - "unroute me", never "break me".

And the part the Lab builds on is the part 4.1.1 hands you with no
configuration at all, which is the delta table's probe row read at runtime:

- `management.endpoint.health.probes.enabled` - **on by default in 4.1.1**,
  on every platform, `matchIfMissing = true`; it was `false` unless
  Kubernetes was detected in 2.7 and 3.5. An application upgrading from Boot
  2 or 3 therefore *gains* two reachable endpoints it never configured - the
  two your orchestrator answered and your local boot never did, now
  answering on the laptop too. The probe groups are `liveness` and
  `readiness`, mapped
  at `/actuator/health/liveness` and `/actuator/health/readiness`, and the
  pre-existing property `management.endpoint.health.probes.add-additional-paths`
  (unchanged in every jar's metadata since 2.3) can additionally pin them at
  `/liveness` and `/readiness` on the *main* server port for the servlet
  rigs that want one fewer host port.
- `management.endpoint.health.group.<name>.include` - the general group
  mechanism the probes are, themselves, two pre-wired instances of. Unchanged
  in 4.1.1 under the module move; one line here rather than a lecture.

### A metric about loans: publish it where it happens, tag it in the domain's words

The metrics seam: with the actuator starter in place, a `MeterRegistry`
arrives wherever you declare it, and `registry.counter(...).increment()` is
the whole publishing step. What a review asks, in order:

- **The counter lives where the thing happens.** The use case increments as
  it issues, the controller does not count after the fact, and there is no
  "collect the stats later" stage the metric depends on:

  ```java
  /**
   * The metric: loans issued, counted where the loan is issued - one line
   * in the use case, and the scraping, the histogram and the dashboard are
   * somebody else's semantics.
   */
  @Service
  public class LoanCounter {

      private final MeterRegistry registry;

      LoanCounter(MeterRegistry registry) {
          this.registry = registry;
      }

      public void loanIssued(String titleId) {
          registry.counter("library.loans.issued", "title", titleId).increment();
      }
  }
  ```

- **The name is the domain's, the tags are low-cardinality.** The metric
  name `library.loans.issued` is a sentence in the glossary's words - a
  [Loan](../GLOSSARY.md) was issued - and the tag is the
  [Title](../GLOSSARY.md) it was issued against, a value bounded by the
  catalogue (finite Titles, per the glossary) rather than by tenants or
  requests. The line never to write is the same line the review always
  catches: a tag whose values are as unbounded as the requests - a request
  id, a [Copy](../GLOSSARY.md) identifier would turn the counter into its
  own time series zoo.
- **The metric is readable without extra tooling.** With
  `management.endpoints.web.exposure.include=health,metrics`, the request
  `GET /actuator/metrics/library.loans.issued` answers, on the 4.1.1
  application we booted with exactly the snippet above and one
  `loanIssued("t1")` call behind it:

  ```json
  {"availableTags":[{"tag":"title","values":["t1"]}],
   "measurements":[{"statistic":"COUNT","value":1.0}],
   "name":"library.loans.issued"}
  ```

  The same name a Prometheus registry holds later, the same `COUNT` the
  dashboard reads - the endpoint is the cheapest way to answer "is my
  metric real?".

Two properties of the metrics starter worth knowing before the Lab:
`management.metrics.tags.<key>` applies a global tag to *every* metric
(the usual answer for "which environment is this from"), and the HTTP
instrumentation is already on: every Spring MVC request the library serves
is observed as `http.server.requests`, in 4.1.1 with no code of ours - we
requested `/titles/42` from the booted application and read the
observation back with its tags:

```json
{"availableTags":[
   {"tag":"exception","values":["IllegalArgumentException","none"]},
   {"tag":"method","values":["GET"]},
   {"tag":"uri","values":["/titles/{id}","/actuator/health","/actuator/metrics"]},
   {"tag":"outcome","values":["SERVER_ERROR","SUCCESS"]},
   {"tag":"status","values":["500","200"]}], ...
 "name":"http.server.requests"}
```

- the recorded `500` with `IllegalArgumentException` is the same run's
  second `/titles/{id}` request, for an id the catalogue does not hold: the
  error path is observed by the same automatic instrumentation, under the
  same template, and earns no code of its own;
- the `uri` tag is the *template* (`/titles/{id}`), which is exactly why the
  automatic observation is cheap even at high traffic, and exactly why
  writing your own `uri` tag (the raw, per-request URI) is the cardinality
  mistake again.

### Custom health: the type moved, the shape did not

A custom indicator on 4.1.1, written against the types the delta table moved -
the loan table the borrow use case writes, counted the way Spring Data counts
any repository the M08 application already owns:

```java
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * The Loan feature answers about itself, in the health vocabulary, with the
 * data the borrow use case trusts.
 */
@Component
public class LoanHealthIndicator implements HealthIndicator {

    private final LoanRepository loans;

    LoanHealthIndicator(LoanRepository loans) {
        this.loans = loans;
    }

    @Override
    public Health health() {
        return Health.up()
            .withDetail("loansHeld", loans.count())
            .build();
    }
}
```

Every piece of that snippet checked against 4.1.1:

- `org.springframework.boot.health.contributor.HealthIndicator` and `Health`
  - the two imports that make the compile a Boot 4 compile. The package
  `org.springframework.boot.actuate.health` the 2.7 and 3.5 memory reaches
  for does not exist on 4.1.1's classpath; the module
  `spring-boot-health` (which the starter pulls, so no new POM line is
  needed) exports the replacement. The bean-discovery step is unchanged:
  a `@Component` implementing the interface is picked up and named by the
  bean's name, `loanHealthIndicator` appearing under the composite health as
  the component `loan`.
- `Health.up().withDetail(...).build()` and the `UP`/`DOWN`/`OUT_OF_SERVICE`
  statuses are the same API surface 2.7 exposed - the move was of package,
  not of design; we read the 4.1.1 `Health` class's static builders
  (`up()`, `down()`, `outOfService()`, `status(...)`) before writing this.
- The indicator appeared in the composite payload only because our test run
  set `management.endpoint.health.show-details=always` - the component name
  showed next to `"status":"UP"`. Production keeps the default (`never`, in
  every column's metadata), and a reason to show detail is spelled
  `show-details=when-authorized`, one setting with an owner.
- A health group - `management.endpoint.health.group.<name>.include` -
  bundles indicators under their own sub-path when a dashboard wants to ask
  narrower questions; the probes are two pre-wired groups, which is why the
  probe row above and this note are the same mechanism saying two things.

The one warning the library's shape earns: an indicator that performs an
outbound call runs that call on *every* health poll, and health polls are
frequent by design - the availability client from M04 does not belong inside
`health()` unless the check is cached and cheap. What a temporarily
unavailable dependency does to the application's *loans* surface would rather
move traffic away (readiness, above) than fail a restart decision (liveness)
- the division of labour the two state names are for.

### Structured logging: two property keys, three format ids, in the core jar

The delta table's quiet row: since Boot 3.3 the JSON layouts live inside
`spring-boot`'s *core* jar (`org.springframework.boot.logging.structured.*`,
which we listed in the 4.1.1 core jar next to the logback and log4j2
formatter classes) - so the actuator starter is not even needed, only the
two property keys:

```properties
logging.structured.format.console=logstash
logging.structured.format.file=ecs
```

- `logging.structured.format.console` reshapes what the console appender
  writes; `logging.structured.format.file`, the file appender. The three
  built-in ids are `ecs` (Elastic Common Schema), `logstash` and `gelf`
  (Graylog Extended Log Format) - read out of the 4.1.1 `CommonStructuredLogFormat`
  class rather than remembered - and a format class of your own is legal by
  giving its fully qualified class name as the value, which is the extension
  point the `logging.structured.json.*` keys (`include`, `exclude`,
  `rename`, `add`) refine without code.
- We then observed it, because the log *is* a behaviour claim: the same
  4.1.1 application, booted with `-Dlogging.structured.format.console=logstash`,
  printed from its first line:

  ```json
  {"@timestamp":"2026-10-23T21:04:41.100973+05:00","@version":"1","message":"Starting VerifyApplication using Java 25.0.4.1 with PID 68974 (...)","logger_name":"com.example.verify.VerifyApplication","thread_name":"main","level":"INFO","level_value":20000,"tags":["COMMONS-LOGGING"]}
  ```

  - the keys the hand-tuned `LogstashEncoder` of a Boot 2 POM produced, now
    from a property line: the `logstash-logback-encoder` dependency and its
    configuration block are simply no longer needed.

The tracing tie-in one paragraph below, because the reason structured lines
carrying `trace_id` and `span_id` matters is the tracing pipeline, and the
correlation switch is `management.tracing.baggage.correlation.*` in the
`spring-boot-micrometer-tracing` module whose keys we read off its metadata.

### OpenTelemetry, behind the Observation API you already used

Micrometer Observation is the seam, and the M04 doc's declarative HTTP
client already speaks it - every Spring MVC request and every
`RestClient` call is an `Observation` the pipeline below consumes, which is
why the [Loan](../GLOSSARY.md) story in this Module publishes *one* line of
`counter(...)` rather than an instrumentation framework. What Boot 4 did is
the pipeline, not the API:

- Boot 3.5 wired tracing *inside* `spring-boot-actuator-autoconfigure` (the
  `tracing` and `opentelemetry` packages we listed there), with
  `management.tracing.*` for the sampler and propagation, OTel **1.49.0**
  managed, and the OTLP *log-record* export at `management.otlp.logging.*`
  - three auto-configurations living where the 2.7 memory kept none.
- 4.1.1 split them out to the module map: tracing in the
  `spring-boot-micrometer-tracing(-brave/-opentelemetry)` modules, the
  OpenTelemetry SDK's beans in `spring-boot-opentelemetry`
  (`management.opentelemetry.enabled`, `resource-attributes`, and the log
  export at `management.opentelemetry.logging.export.otlp.*`), and - the
  user-facing simplification - `spring-boot-starter-opentelemetry` wraps
  metrics, tracing and log export in one line. The old OTLP keys remain as
  *error-level* aliases (`management.otlp.logging.endpoint` -> replacement
  `management.opentelemetry.logging.export.otlp.endpoint`, `since 4.0.0`,
  read off the 4.1.1 metadata), so the re-spelling is enforced, not
  suggested.
- The annotation route exists and is *off* by default: `@Observed`
  (`io.micrometer.observation.annotation.Observed`, verified present in
  micrometer-observation 1.17.1) works through an aspect that only installs
  when `management.observations.annotations.enabled=true` - the property we
  read (default `false`) off the 3.5.16 and 4.1.1 metadata. The reason to
  prefer the registry over the aspect is the same reason the Lab's snippet
  is a constructor: an explicit declaration is one the reader can trace,
  and the aspect is a broadcast the reader has to switch on to *notice*.

### What must never be exposed

The starter's defaults already hold the line; the review's job is to notice
every place a convenient line would move it. In the order a review reads the
diff:

1. **Health details stay behind `show-details=never`.** The default, in the
   configuration metadata of every column we read (2.7.18, 3.5.16, 4.1.1),
   and the one that keeps component names, JDK versions and every
   `withDetail` you wrote - the M08 loan numbers among them - off a public
   payload. `when-authorized` exists for the operator who *has* a reason;
   `always` is a change with a name on it.
2. **The inspection endpoints stay unexposed on the web.** `env`,
   `configprops`, `heapdump`, `threaddump`, `loggers`, `httpexchanges` are
   the endpoints that answer "what does this process know and where does it
   keep it" - the default exposure (`health`) excludes them in all three
   columns, and `include=*` is the one-liner that hands that knowledge to
   anyone who can reach `/actuator`. The M10 Module covers the
   authentication side; the exposure side of the review is one line: how
   did the default change, and who agreed to it.
3. **`shutdown` is never just enabled.** In all three versions the endpoint
   is `@ConditionalOnAvailableEndpoint(ShutdownEndpoint)` and absent from
   the default exposure - our discovery-page observation (the booted 4.1.1
   application's `/actuator` answer) listed exactly `self` and `health` and
   nothing named `shutdown`. A configuration that re-adds it without
   authentication is a kill switch on a public path.
4. **The management surface is a different socket, or a stated exception.**
   `management.server.port` (with `management.server.base-path`,
   both verified present in the 4.1.1 metadata) moves the actuator onto its
   own port: the public API's ingress rules, its rate limits and its
   certificate continue to know nothing about `heapdump`. The library is a
   public API-shaped service, so the management port being reachable only
   from inside the deployment is the deployment decision; leaving the ports
   merged must be *written down*, not inherited.
5. **JMX is the same review in a second dialect.** The default was `*` in
   2.7 and is `health` in 3.5 and 4.1.1 - the same "the safe state is the
   handed-down state" rule, and the reason a Boot 2 operations habit
   ("scrape the JMX tree") reads as an exposure claim rather than a benign
   default on 4.1.1.

## The Lab

The Lab is the library application you last touched in M08 - the borrow
commits atomically, the [Copy](../GLOSSARY.md) guard is one statement, the
[Loan](../GLOSSARY.md) rows live in the database. None of that is observable
from outside the process: the application publishes no metric about the
loans, defines no health indicator of its own, and its actuator surface
shares the API's socket - nothing announces to an operations team what the
transaction lab proved.

Your job on the Start module is the observability upgrade, in this order:

1. the liveness and readiness probes answer - in 4.1.1 that is the starter's
   default, which is precisely the claim the Lab's diagnostics check before
   anything else;
2. the metric about loans is published in the Loan feature's code - the use
   case increments where it issues, with the metric named in the glossary's
   words and tagged with a bounded dimension;
3. the management endpoints are kept **away from** the public API - the
   management server moves to its own port, the exposure list stays at the
   default plus what the Lab's diagnostics read, and the never-list of the
   previous section holds.

The observability requirement is stated by Diagnostic tests in the
`diagnostics` package - the tests that fail on the Start module (which
publishes nothing) and pass on the Solution module, which is written from
scratch rather than patched. The command that states the requirement, the
one the [lab index](../labs/README.md) entry teaches, is:

```bash
./mvnw -f labs -pl lab-09-observability-start verify -Pdiagnostics
```

Read the failure messages the way this course reads every delta table row:
each names the behaviour that is missing. The same tests pass against
`lab-09-observability-solution` (and, in CI, against every Solution module
at once with `./mvnw -f labs verify -Pdiagnostics-solutions`); read that
module last, to see how small the observable story is once the starter's
defaults and the use case's own seams hold.

## Quiz

Five questions about the delta table above and the observability register it
equips you to review. Answer first; the answers say why, not just what.

1. **About the starter and location rows: a Boot 2 codebase upgrades to
   4.1.1. The starter's name in the POM is unchanged, the health endpoint
   answers at boot, and then one class fails to compile: an import of
   `org.springframework.boot.actuate.health.HealthIndicator` - a package
   4.1.1 does not contain. Name the two moves that produced this error, and
   why the starter's dependency list keeps the *application* running while
   the *class* is broken.**

   Two moves in one error. The *module* move: the health endpoint and all
   its support left the `spring-boot-actuator` jar (the 2.7 and 3.5
   monolith) for the new `spring-boot-health` module. The *package* move:
   the custom-indicator types took the `org.springframework.boot.health`
   package root the whole Boot 4 modularisation uses - `HealthIndicator`
   and `Health` now live in `org.springframework.boot.health.contributor`,
   and the endpoint machinery in
   `org.springframework.boot.health.actuate.endpoint`. The application runs
   because the *starter* still resolves - its 4.1.1 POM pulls
   `spring-boot-health` transitively, so every *auto-configured* piece (the
   endpoint, the groups, the availability indicators) is on the classpath
   and wired; the failure is confined to the code that names a moved
   package root, and the fix is one import line to the new package plus
   nothing else, because the interface's contract - `Health health()` and
   the `Health.up().withDetail(...).build()` chain - never changed. The
   error is loud and cheap, which is why the delta table states the row as
   a package move, not a redesign.

2. **About the probe row and the section that splits liveness from
   readiness: a colleague declares the upgraded library's probes "too
   shallow", and their change moves the availability-service client - the
   M04 gate the borrow use case calls - into the liveness probe group, with
   the reasoning "the platform restarts what liveness calls broken, and an
   unreachable dependency is exactly the kind of outage a restart fixes."
   Name the question each of the two probes answers, the state pair each
   holds, where a temporarily unreachable dependency belongs instead, and
   the one API the change of readiness state is published through.**

   The two probes are two questions about one process, and splitting them
   is the section's first move. Liveness answers "restart me?" - is this
   process itself intact, the bits it runs on working - and its state is
   `LivenessState`, with exactly the two values `CORRECT` and `BROKEN`. It
   answers about the process, so it carries no database, no network call
   and no availability client inside it: the availability service is not
   part of the process, and a restart is the platform's fix for a broken
   *process*, not for a broken *dependency* - which is why the colleague's
   liveness spends an outage demanding the one remedy that cannot remedy
   it, restart after restart, while the process's own code was never the
   fault. Readiness answers "route me?" - willing and able to receive
   traffic - and its state is `ReadinessState`, with exactly
   `ACCEPTING_TRAFFIC` and `REFUSING_TRAFFIC`; a temporarily unreachable
   dependency belongs to this decision, because what the outage should
   move is traffic, not life. And that move is an event, not a boolean a
   handler reads: the one API is
   `AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC)`,
   which the probe endpoint translates to `OUT_OF_SERVICE`, not `DOWN` -
   "unroute me", never "break me". The doc adds a second, cost-shaped
   verdict on the same diff: an outbound call inside any `health()` runs
   on every health poll, and health polls are frequent by design - the
   client belongs there cached and cheap, or not at all.

3. **About the section that publishes the metric where the Loan is issued:
   two colleagues review the Lab's solution, and each proposes a change.
   One replaces the `library.loans.issued` counter with an INFO log line
   per borrow - "the log already carries every Loan; count the lines when
   the dashboard asks." The other keeps the counter but adds a second tag
   to it, the [Copy](../GLOSSARY.md) identifier - "so one Copy's borrowing
   history becomes a graph of its own." Name the question the counter
   answers that the counted lines cannot, and what property decides
   whether a tag is safe.**

   Both proposals fail the same section from opposite ends. "How many
   [Loan](../GLOSSARY.md)s were issued this morning" is a question about
   an aggregate, and the aggregate is what the counter owns: one
   increment in the use case, where the Loan is issued, and one series -
   `library.loans.issued` - in the registry, cheap to keep, alertable,
   comparable across mornings, and readable before any dashboard exists
   (`GET /actuator/metrics/library.loans.issued` answers with the
   measurements). A log line answers an event's question about one Loan,
   and count-the-lines turns it into the aggregate's answer the expensive
   way: ship, index and query the whole corpus to produce the number the
   registry holds for free, a cost that grows with every borrowed
   [Loan](../GLOSSARY.md) rather than staying one series wide. The
   second proposal attacks the other end: a tag whose values are as
   unbounded as the requests - the section's own examples are a request
   id and a Copy identifier - splits the one counter into its own time
   series zoo, one series per Copy ever borrowed, and the per-Copy
   history is a log or payload query's question, not a metric tag's. The
   property that decides a tag's safety is boundedness by the data, not
   by the traffic: the [Title](../GLOSSARY.md) the Loan was issued
   against is finite for the catalogue, which is why the section's
   increment reads `registry.counter("library.loans.issued", "title",
   titleId).increment()` and not a Copy-keyed line.

4. **About the tracing row: a colleague says "with
   `spring-boot-starter-opentelemetry` on the classpath we must configure
   an OTLP endpoint, or Boot 4 will refuse to start" - and a second
   colleague says the opposite, "it starts and sends everything by
   default" - both are wrong in the same way. State the 4.1.1 contract, in
   the bytecode's terms, and then what *does* happen to a trace when no
   endpoint is configured.**

   The contract the auto-configurations implement is *gated exporting* over
   an *enabled observation layer*: the starter wires the pipeline - the SDK's
   beans (`spring-boot-opentelemetry`), Micrometer Tracing's bridge
   (`spring-boot-micrometer-tracing-opentelemetry`, over OTel **1.62.0**),
   the OTLP trace exporter, and the log export - but each exporter is
   *conditional on its configuration*: the conditions whose names we read
   off the jars (`ConditionalOnEnabledTracingExport`,
   `ConditionalOnEnabledLoggingExport`, `ConditionalOnEnabledOpenTelemetry`)
   only activate when their export is configured through
   `management.tracing.export.enabled` /
   `management.opentelemetry.logging.export.*` property presence. What
   happens without an endpoint: the application starts, the Observation API
   and the metrics registry run as before, the tracing pipeline constructs,
   and *no bytes leave the process* - the default is silent-wired, not
   pre-configured. Both colleagues imported the same binary claim and
   neither looked at the starter's gates - which is why the doc's claim
   cites the bytecode rather than either of them.

5. **About the never-exposed list: you are reviewing a PR whose
   `application.properties` adds
   `management.endpoints.web.exposure.include=*` alongside
   `management.endpoint.health.show-details=always`, and the author's
   commit message says "make local debugging easier". Name what each line
   hands a stranger, and rewrite the change so the debugging convenience
   survives without ever reaching a deployed configuration.**

   The `include=*` line rewrites the default exposure list (the one whose
   value we read - `['health']` - from all three columns' metadata) to
   every discoverable endpoint on the web: `env` and `configprops` publish
   the process' property sources and its configuration keys, `heapdump` is
   a dump of the heap to anyone who can hit the management path,
   `threaddump` and `loggers` both reveal identities and let a caller adjust
   replay-time logging behaviour, `httpexchanges` replays requests. The
   `show-details=always` line removes the health payload's quiet default -
   component names, versions, and every detail string the code's
   `withDetail` calls publish (the loan numbers of the library's own
   indicators) - one GET away. The rewrite that keeps the convenience:
   the exposure line targets only what the debugging *needs*
   (`health,metrics,loggers`), further narrowed per environment, and the
   details flip moves into a *test* profile - `application-local.properties`
   or a test's `properties = {...}` - which is exactly where our own
   observation run set it, and where it can be as generous as the
   diagnostics like without ever being a change to the deployment.

## Checklist

What a reviewer applies to a solution of this Lab to judge it
production-shaped rather than merely working. Every item is observable -
checkable by reading the change or running a command - and none of them
repeats a gate the build already enforces.

- **Each probe answers the question it is named for.** Boot the
  application and read `/actuator/health/liveness` and
  `/actuator/health/readiness` - both `200` and state-named, with no probe
  property written anywhere in the change (4.1.1's default, which the
  delta table verified). The probes are not one check but two questions,
  and each is verified against its own: liveness answers "restart me?"
  about the process, so its path contains no database check, no
  availability client, and no network call - a search of the diff answers
  in seconds. Readiness answers "route me?", so the dependency that can
  be temporarily unreachable - the availability service above all -
  appears in no liveness group: what its outage moves is readiness,
  answered `OUT_OF_SERVICE`, never a liveness decision a restart cannot
  remedy.
- **Readiness is published, not toggled.** The code that stops or admits
  traffic goes through `AvailabilityChangeEvent.publish(context, state)` -
  one line, one event bus - and never through a boolean a handler reads;
  the `OUT_OF_SERVICE` answer a `REFUSING_TRAFFIC` publish produces is
  verified in the diagnostics.
- **The metric is worth having - it answers a question someone asks.**
  Every counter the change adds earns its place by one test the review
  applies in prose: name the question it answers and who reads the
  answer. The Loan counter is the shape a passing answer takes -
  `library.loans.issued` answers the question the Module's opening named,
  "how many [Loan](../GLOSSARY.md)s did it issue this morning", in the
  cheapest form the registry offers, and each increment in the diff is
  justified by exactly a sentence where the change says so (the borrow
  use case's comment states the fact the counter exists for and no
  other). A counter whose question nobody can state - a vanity number
  published because dashboards expect numbers, a second count of the
  `http.server.requests` the starter already carries - is deleted from
  the change rather than kept, tagged and tuned.
- **No sensitive data reaches the metrics.** The metrics are readable by
  everyone the management port reaches, and the review reads them as a
  stranger would: no tag name and no tag value identifies a
  [Member](../GLOSSARY.md) - the identity would be the privacy defect and,
  being unbounded, the cardinality defect in the same line, which is why
  the diagnostics' message names `a Member id` beside `a Copy identifier`
  among the tags never to write. The one tag the Lab's counter carries is
  the [Title](../GLOSSARY.md) id, an inventory word - the detail a custom
  indicator publishes stays with that item's criterion. No
  `management.metrics.tags.*` global tag arrives with the change - a
  global tag is applied to every metric, so a careless key there is a
  leak the diff can hide nowhere. Verified by reading the increment line
  and every `management.metrics` entry in the change.
- **The metric is published where it happens, tagged in the domain's
  words.** `library.loans.*` is the metric's name-space, the increment
  line lives in the use case (not the controller, not a consumer), and the
  tags name a bounded dimension ([Title](../GLOSSARY.md) id, result) -
  never a per-request id and never a [Copy](../GLOSSARY.md) identifier.
  Verified by one GET: `/actuator/metrics/library.loans.issued` (after
  exposing `metrics`, only if the Lab asks) names the tag and the `COUNT`.
- **A custom indicator, where the change adds one, is in the Boot 4
  package.** The imports are
  `org.springframework.boot.health.contributor.*` - and the change contains
  no `org.springframework.boot.actuate.health` anywhere, which the diff
  search confirms. The bean's domain name (`LoanHealthIndicator` reading
  back as `loan`) travels with the class, and the detail it publishes
  under `show-details=when-authorized` is a number the application can
  compute, not a string it fetched.
- **The management surface is not the public API.** `management.server.port`
  names a separate port in the application properties (and if the ports
  are deliberately merged, that decision is *written down in the change*,
  per the never-exposed list). The exposure list is the default plus the
  endpoints the Lab's diagnostics read, each addition with a reason in the
  same file; `include=*` appears nowhere.
- **The never list stays in place.** `show-details=always` outside a test's
  own configuration, `management.endpoint.shutdown.enabled`,
  `include=*` - a search of the whole change for each, and the only
  `show-details=always` in the repository is inside the tests, where our
  observation run set it, never in an `application.properties` that ships.
  Every piece of inspectable detail (`env`, `configprops`, `heapdump`,
  `threaddump`, `httpexchanges`) stays behind the default exposure, with
  M10's authentication answer as the *next* layer rather than a substitute
  for this one.
- **Structured logging, if the Lab's diagnostics demand it, rides the
  built-in ids.** `logging.structured.format.console` (or `.file`) values
  are `ecs`, `logstash`, or `gelf` - the ids the 4.1.1 core jar registers -
  and no third-party encoder dependency arrives with the change; the
  observable result is one JSON object per line, read out of the boot
  output the way this doc observed its own.
- **The spelling of every property in the change is the 4.1.1 one.** No
  `management.trace.http.*`, no `management.metrics.export.<id>.*`, no
  `management.otlp.logging.*`, no `management.health.probes.enabled` -
  each of those is one of the metadata's *error* aliases on 4.1.1, so a
  change carrying one is a change the boot itself will refuse; the diff
  search for the four prefixes answers in seconds.
- **The vocabulary is the glossary's in the names, not just in the prose.**
  The metric name, the indicator's component key, and the readiness event's
  terms speak about [Loan](../GLOSSARY.md)s, [Title](../GLOSSARY.md)s and
  [Member](../GLOSSARY.md)s - not "book", not "user", not "checkout" - so
  the dashboard the observability data feeds uses the same words the code
  and the docs use.
- **The diagnostics guarantee is proved by a failing-then-passing run.**
  Run it both ways and report what ran:
  `./mvnw -f labs -pl lab-09-observability-start verify -Pdiagnostics`
  (red - the Start module publishes nothing), then the same against the
  `-solution` module (green - the probes, the metric and the management
  separation hold); the asymmetry is the requirement, stated exactly as
  M08's ran.
