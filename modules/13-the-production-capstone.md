# M13 - The production capstone

M12 closed with the application packaged in every form it will ever travel
in: the plain jar, the extracted layout, the AOT-processed jar under its
switch, the AOT cache, and the native image for the site that wants it. The
code in your editor is finished, and it travels. What no Module has yet done
is read the whole thing together - not as twelve chapters about one
application, but as *one application the chapters assembled* - and state the
operational contract it now keeps. This Module is that read. It walks the
library as a deployment sees it: what boots, what the platform is allowed to
ask, what a stranger reaches, what the process refuses with, what it
measures, what one slow dependency costs, what the schema owns, and what the
build froze - the whole checklist a run of `./mvnw -f labs verify` proved and
the twelve delta tables decided, assembled in one place and checked against
4.1.1. By the end you can review the deployed application the way you review
a PR: line by line, against claims you can verify, with the Lab putting the
checklist into diagnostics so the assembly stays green the way modules
stay green. One boundary this course draws, and the capstone is the Module
that must say it loudest: what happens *around* the artifact - the
orchestrator, the load balancer, the database cluster, the alerting stack -
is deployment infrastructure, and it is out of scope; the same goes for the
*depth* the sibling course owns - chaos injection, native-image internals,
the circuit-breaker zoo. This course taught the mechanism; this Module
teaches the assembly of the mechanisms into one deployable, and the
checklist for telling whether the deployment keeps its promises.

## What you knew

In Boot 2, standing an application like this library up in production was an
act of assembly, and every piece of it was yours. You packaged the fat jar
and `java -jar` was the launch; if the init system had to own it, you set
`executable=true` on the repackage and shipped the launch script inside the
jar. Configuration was one `application.properties` baked next to the
classes and bent per environment by a `--spring.config.location` flag or an
environment variable spelling only. The schema was `spring.jpa.hibernate
.ddl-auto` - `create-drop` on the dev H2, `update` on anything longer-lived -
or, if you were disciplined, Flyway with hand-maintained `V<NN>__` scripts.
The gate was a `WebSecurityConfigurerAdapter` subclass whose chain you had
copied since Boot 1, and the actuator rode the API's port with the one
exposure line you memorised (`management.endpoints.web.exposure.include
=health,metrics`) standing between a monitor and `heapdump`. The failure
story a [Member](../GLOSSARY.md) saw was the tail of a stack trace: an
empty `500` body, no problem details anywhere. Timeouts were a
factory setting per `RestTemplate`; a retry was the `spring-retry`
artifact's `@Retryable`, in a package the framework does not resolve; the
capacity model was Tomcat's 200-thread pool; the shutdown default was
`immediate`, and graceful draining was a setting most codebases never
turned on. Observability was the two endpoints the exposure line named, a
`MeterRegistry` you fed, and a `LogstashEncoder` you pasted in and tuned by
hand. And the operational checklist - the "when it is slow, restart it and
never deploy on a Friday" page - lived in a wiki, unlinked to any command,
asserting nothing.

You know all of that fluently. You are **not** assumed to know that 4.1.1
answers most of those questions before you configure anything - the probes
on by default on every platform, the drain on by default one generation
earlier - or that the pieces the twelve Modules built now fit together into
a contract the deployment can *state*: the build-time environment its AOT
decisions freeze, the two questions its probe groups answer, the exact
status each refusal gets, the two counters the operator reads first, the
drain window the process promises, the schema the migration owns on two
engines. You are **not** assumed to know which of these flips happened in
Boot 4 and which one generation earlier (graceful shutdown: 3.4; the
probe default: 4.1), or whose assembled sum no Module has yet stated as
one table.

## What changed

The Boot 4 column is the one this course's build resolves - Spring Boot
4.1.1 over Spring Framework 7.0.9 - every cell below read off the resolved
jars. This Module's own reads, freshly done for this table: the
`AvailabilityProbesAutoConfiguration` class file in 4.1.1's
`spring-boot-health` jar (`javap -v` on the annotation's
`matchIfMissing`), the `spring-configuration-metadata.json` of
`spring-boot-web-server` 4.1.1 (`server.shutdown`), `spring-boot-webmvc`
4.1.1 (`spring.mvc.problemdetails.enabled`'s default),
`spring-boot-actuator-autoconfigure` 4.1.1 (the exposure defaults and
`show-details`), `spring-boot-health` 4.1.1 (`show-details`),
`spring-boot-http-client` 4.1.1 (the plural spellings live, the singular
ones deprecation-tagged `since: 4.0.0` with replacements named), the
`spring-boot-starter-actuator` 4.1.1 POM (five dependencies), and the
formatter class listing inside the `spring-boot` 4.1.1 core jar (the
structured-logging ids). Cells this table carries forward from the twelve
earlier Modules' tables keep those Modules' provenance - each row says
which - re-quoted, not re-derived. Where a *behaviour* had to be seen, not
stated, the row leans on the lab application that booted in front of the
Module that tested it, named per cell. The Boot 2 column is the last Boot 2
(2.7), the Boot 3 column the last Boot 3 (3.5). The runtime is JDK 25
throughout - this course's root POM sets `<java.version>25</java.version>`.

|  | Spring Boot 2 (2.7) | Spring Boot 3 (3.5) | Spring Boot 4 (4.1.1) |
|---|---|---|---|
| The launch line | `java -jar` over the repackaged jar, or the fully executable jar (`executable`, `embeddedLaunchScript` are repackage params) started by an init system; the loader rides in the manifest | the same command; the AOT machinery exists (`process-aot` from 3.0) but belongs to the native build | the jar is a jar again: `executable` and `embeddedLaunchScript` are gone from the 4.1.1 repackage mojo, the classic loader and its `loaderImplementation` param are gone (M12 read all six plugin descriptors) - and the documented forms multiply: the extracted layout (`java -Djarmode=tools -jar ... extract`, the 3.3 rename of `layertools`), the AOT-optimized jar under `java -Dspring.aot.enabled=true -jar`, the AOT cache trained against the extracted form on this JDK 25, and the native image for the site that builds with the version-25 distribution (M12's cells and its lab-12 diagnostics, which booted all of the JVM forms) |
| What the environment decides | everything, at every boot: conditions, profiles, bean instances - the jar carries no memory of the environment it will meet | the *native* build freezes them; a JVM jar still decides at boot | the AOT-processed jar freezes them for the JVM run too: `process-aot` refreshes the container against the build-time environment - conditions evaluated, beans that existed at the walk then always exist (the how-to page's own sentence, and M12's `AotInitializerNotFoundFailureAnalyzer` read) - so a profile that decides conditions must ride the `process-aot` execution's `<profiles>`, and a `.enabled` flag stops being a runtime switch under the AOT boot; the regular flag-free run is the supported fallback |
| What the platform may ask about the process | liveness and readiness exist as sub-paths since 2.3, behind `management.endpoint.health.probes.enabled` defaulting **false**, auto-enabled only when `ProbesCondition` finds Kubernetes | the same condition, re-compiled: the property or the platform, nothing else | **on by default, every platform**: `AvailabilityProbesAutoConfiguration` in the new `spring-boot-health` module is `@ConditionalOnBooleanProperty("management.endpoint.health.probes.enabled", matchIfMissing = true)` - the module's own `javap -v` read of the 4.1.1 class file - so an upgrading application *gains* two answering endpoints it never configured; the M09 boot read `200` off both on a laptop |
| Who reaches what, over which socket | `management.server.port` exists, but it is a habit to adopt rather than a default to refuse: the actuator shares the API's port in most Boot 2 codebases; the web exposure default is `health`, JMX reads **every** endpoint (`*`), details read `never` unless flipped | JMX's `health` cut is the 3.x change; everything else holds | the safe state is still the handed-down state - the module's fresh metadata reads of 4.1.1 find web exposure `health`, JMX exposure `health`, `show-details` `never` - and the assembled application adds the two decisions M09 and M10 each taught in one half: the management surface on its own socket (`management.server.port: 0`, the public socket answering `404` for `/actuator/health` - the lab-09 run), the management chain claiming `/actuator/**` at `@Order(1)` with the probe public and the rest `OPERATOR`, the API chain closing with `anyRequest().authenticated()` (the lab-11/12 chains, quoted in M10 and M12) |
| What the process refuses with | whatever you wrote: standard exceptions answer `500` with an empty body, domain refusals are `RuntimeException`s the UI swallows | `spring.mvc.problemdetails.enabled` exists (3.0), defaults **false** - opt-in only; Framework 6.1 moved binding failures to the dispatch level so they answer `400` before your code runs | the switch exists *and still defaults false* - the module's fresh metadata read of the 4.1.1 `spring-boot-webmvc` jar - and the assembled application does not wait for the switch: one handler maps the typed refusals it owns, each status attributed, each answering `application/problem+json` - `409` for the [Loan limit](../GLOSSARY.md) and the [Available copy](../GLOSSARY.md) not on the shelf, `404` for an unknown [Copy](../GLOSSARY.md), `400` for the binding the validator refuses, `502` for the retry budget the availability service spent (the M06, M08 and M11 diagnostics read each of these off the wire) |
| What the process measures and logs | two endpoints behind one memorised line; `LogstashEncoder` pasted into the POM; `management.metrics.export.<id>.*` is the spelling; no tracing anywhere | structured logging is two property lines since **3.3** (`logging.structured.format.console`/`.file`); tracing wired inside the actuator | the three format ids (`ecs`, `logstash`, `gelf`) still live in the core jar - the module's fresh `unzip -l` lists the formatter classes for logback and log4j2 both - and the actuator starter is a five-dependency POM (fresh read: `spring-boot-starter`, `spring-boot-starter-micrometer-metrics`, `spring-boot-actuator-autoconfigure`, `spring-boot-health`, the micrometer pair); the assembled application publishes two counters of its own - `library.loans.issued` tagged with the [Title](../GLOSSARY.md), `library.availability.retried` on the `MethodRetryEvent` - beside the automatic `http.server.requests`, and nothing leaves the process while no exporter is configured (M09's condition reads) |
| What one slow dependency costs | no timeout property anywhere; one in-flight wait holds one of 200 platform threads; a retry is an extra artifact; `server.shutdown` defaults to `immediate`, so graceful draining is opt-in | 3.4 gives the timeout a property home (`spring.http.client.*`) and flips the shutdown default to `graceful` - the flip one generation before Boot 4 | the plural spellings are the live ones (`spring.http.clients.connect-timeout`, `spring.http.clients.read-timeout`) and the singular ones stay in the metadata deprecation-tagged `since: 4.0.0` with their replacements named - the module's fresh read of the `spring-boot-http-client` 4.1.1 metadata; `ServerProperties` carries default `graceful` in the new `spring-boot-web-server` module (fresh metadata read); the retry and the bulkhead are Framework 7's own annotations (`@Retryable`, `@ConcurrencyLimit`), resolved by jars the build already pulls, and the virtual switch (`spring.threads.virtual.enabled`, default still `false`) makes holding a request cheap; the assembled application names its drain window (`10s`) rather than inheriting thirty seconds nobody discussed |
| What the schema is allowed to be | `ddl-auto` by habit - `create-drop` in dev, `update` in the longer-lived deployment - or `schema.sql`; Flyway optional discipline | Flyway 11, its auto-configuration still inside `spring-boot-autoconfigure` | the migration owns the schema: Flyway **12.4.0**, and the trap M05 ran into - a bare `flyway-core` configures nothing on 4.1.1, `spring-boot-starter-flyway` is what makes the migration run - and `ddl-auto: validate` refuses Hibernate's guesses; the migration's words: one migration, two engines, every type standard SQL, the same script the H2 boot and the PostgreSQL boot take unchanged; the columns are operational vocabulary - `member_id`, `copy_id`, `barcode` are the words an incident is written in |

Eight rows, and the sum is one sentence: **in Boot 2 the deployment hoped
and the runtime decided; on 4.1.1 the deployment states a contract and the
build freezes the part the runtime has no business deciding twice.** Three
kinds of change hide in that sentence, and each kind is where an assembly
review spends its attention:

- **The first kind: defaults flipped where you were not looking.** Two
  answers the old habit configures arrive no longer configured: the probe
  endpoints answer on every platform (the fresh `matchIfMissing` read
  above), and the drain runs unless someone names `immediate` again. Both
  flips are *gains* for an upgrading application - the deployment inherits
  more than it knew to ask for - and the checklist's job shifts from
  "configure the right thing" to "notice what is now configured and
  attribute the decision": a probe group that answers, a drain that
  happens, each now has *provenance*. The flip that predates Boot 4
  (graceful, in 3.4) matters for a different reason: a Boot 2 codebase
  that upgrades straight to 4.1.1 cannot tell, from its own memory, which
  release moved which default - this table is the attribution.
- **The second kind: the contract became stateable.** Every row's 4.1.1
  cell answers in terms a deployment can write down - a switch whose
  default is named, a property whose plural spelling is the live one, a
  status attributed to each refusal, a counter whose tag list is
  enumerated, a drain window with a length, a schema whose owner is a
  migration. A Boot 2 deployment could only *describe* these
  (the wiki page did, in prose nobody executed); a 4.1.1 deployment
  states them in one place - configuration, build, code - where each is
  read back or refused.
- **The third kind: the build began to decide.** The AOT row is the one
  whose centre of gravity moved from run time to build time, and that is
  the row whose consequence survives longest into operations: the
  conditions, the profiles and the beans the AOT walk evaluated at build
  time are the ones the deployed boot replays. "Which environment did the
  build see?" is no longer a curiosity about CI - it is part of the
  artifact's identity, the question whose answer the checklist keeps
  next to the tag the artifact ships under.

## How it works now

### The application the twelve Modules assembled

Read the library as one object, in the order a packet reaches it. The
artifact is one repackage-built jar that also exists as the extracted
layout, the AOT-processed jar, the cached jar and the native image - the
M12 forms are its skins, and the checklist below runs against whichever
form the deployment chose. Inside the process, a request arrives on the
public socket and meets the API chain: the [Title](../GLOSSARY.md)
catalogue's reads are public, `/api/loans/**` wants the `MEMBER` role, and
everything else is closed by `anyRequest().authenticated()` rather than by
being named. A borrow that passes the chain reaches the use case through
method security - `@PreAuthorize("#memberId == authentication.name")`, the
second lock whose first lock is the controller passing the principal
instead of a body field - and then the boundary: the guard statement
refuses the second borrower of the one [Available copy](../GLOSSARY.md)
inside one `@Transactional` window, the [Loan limit](../GLOSSARY.md) is
counted against the authenticated [Member](../GLOSSARY.md), and the
[Loan](../GLOSSARY.md) row lands with its due date fourteen days out - a
[Loan period](../GLOSSARY.md) read from configuration, not a constant. The
borrow asks out first: the availability client behind the injected builder
(bounded by the `spring.http.clients.*` property, retried by the proxy
behind `@EnableResilientMethods`, observed by the `MethodRetryEvent`
counter), whose exhausted budget answers `502` on the same surface every
other refusal answers on. The process that serves all this measures itself
two counters' worth, answers two probe questions, and drains on stop.

What the twelve Modules built, then, is not twelve features - it is one
deployment contract, and the assembled configuration is where the contract
reads back. Assembled `application.yml` per line - the excerpt below is
the lab-12 solution's own file, minus the comments that taught each line:

```yaml
spring:
  threads:
    virtual:
      enabled: true        # three beans swap; the connector caps stay real
  http:
    clients:
      connect-timeout: 1s  # the live spelling; the singular is deprecated
      read-timeout: 2s     # every built client inherits the settings
  lifecycle:
    timeout-per-shutdown-phase: 10s   # the drain window is a decision

management:
  server:
    port: 0                # the management surface is a different socket
  endpoints:
    web:
      exposure:
        include: "health,metrics,threaddump"   # the default plus what the
                                               # diagnostics read, no more

library:
  loan:
    period: 14d            # the Loan period, named where deploys override it
    limit: 3               # the Loan limit - the domain rule, not a default
```

Every line is one of the twelve Modules' decisions standing in the file
the deployment actually ships: a reader who can explain any line of it has
the course; a reader who cannot has the Module that taught the line. That
is the practical test of "assembled": **the one configuration file is the
operational checklist in executable form**, each line a claim the
diagnostics can watch fail rather than a paragraph in a wiki.

### The operational checklist

This is the section the Module exists for, read as an ordered review.
The checklist a reviewer applies to *this* application before it ships -
each item phrased as the claim the deployment keeps, with the way the lab
verified it, so the review is a sequence of observable questions, not a
diff of opinions:

1. **The launch line names the form.** The plain jar repackage boots
   everywhere the JDK runs; the AOT jar boots under
   `java -Dspring.aot.enabled=true -jar` and *says so* - the boot log
   reading "Starting AOT-processed", a reflective boot printing no such
   line - and an AOT switch on a jar that never walked prints the analyzer
   sentence that names the switch instead of a stack trace. The extracted
   form is the run the docs call faster and recommended in production,
   and the cache trained against it is the cheapest start the
   JDK-25 site gets. Verified by M12's diagnostics, which launched each
   form and read the boot output.
2. **The build-time ledger is written down, not discovered.** Under the
   switch, the boot replays the conditions the build evaluated - profiles
   that decide conditions must ride the `process-aot` execution's
   `<profiles>`, beans that existed at the walk are then always there, and
   `.enabled` flags stop being runtime switches on an AOT boot. The
   flag-free run is the supported fallback, which is what makes the
   contract reviewable rather than irreversible: the same jar runs
   regular, and what changes between the boots is your decision to
   explain. Verified by M12's red/green pair - the switched boot and the
   analyzer's own sentence for the jar that never walked.
3. **The two probe questions answer about themselves.**
   `/actuator/health/liveness` answers the "restart me?" question with
   nothing of the process's dependencies inside it - no outbound call, no
   database read, no availability client - because a restart is the
   platform's repair for a broken *process*, never for a broken
   *dependency*; `/actuator/health/readiness` answers the "route me?"
   question, and a `REFUSING_TRAFFIC` publish -
   `AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC)`
   - moves it to `OUT_OF_SERVICE`, "unroute
   me", never "break me". Boot 4.1.1 turns both on by default (the delta
   table's fresh read), so the first probe question an upgrade review asks
   is answered before anyone writes configuration: the probes that used
   to be a property are now a default to be *deliberately* kept or turned
   off, not a feature to be wired.
4. **Who may ask is enumerated, not ambient.** The unauthenticated caller
   earns `401`; a principal without the `MEMBER` role earns `403` before
   the use case runs; the [Member](../GLOSSARY.md) who names another
   [Member](../GLOSSARY.md)'s id earns `403` from the method security -
   and the chain's `@Order` is load-bearing: the management chain claims
   `/actuator/**` first, the probe stays public, everything else on that
   surface wants `OPERATOR`; the API chain closes with
   `anyRequest().authenticated()`. No path is safe by being unlisted -
   the closing rule owns the unlisted ones. Verified by M10's five
   diagnostics, which exercised the chain at both sockets.
5. **Every refusal answers in the problem-detail vocabulary, with its
   status attributed.** The [Loan limit](../GLOSSARY.md) exceeded, the
   [Available copy](../GLOSSARY.md) gone, the unknown
   [Copy](../GLOSSARY.md), the binding the validator refuses, the retry
   budget spent - each is one handler method's answer, each
   `application/problem+json`, each carrying the title of its refusal.
   The switch (`spring.mvc.problemdetails.enabled`) defaults *false* on
   4.1.1 as on 3.5, so the assembled application's handler is a decision,
   not a default - and the types are attributed one-to-one: a search of the
   change for `@ExceptionHandler` finds one per typed exception, and no
   `@ExceptionHandler(Exception.class)` swallowing what nobody
   anticipated. Verified by M06's, M08's and M11's diagnostics, which read
   each refusal off the wire.
6. **The counters answer the questions an incident asks first.**
   `library.loans.issued` - how many [Loan](../GLOSSARY.md)s, tagged with
   the [Title](../GLOSSARY.md)'s bounded id; `library.availability.retried`
   - how many times the library asked again, the outage's signature read
   beside the borrows that failed; `http.server.requests` - every request,
   tagged by the *template* its path matches, so the dashboard never meets
   a raw URI. No tag identifies a [Member](../GLOSSARY.md), a
   [Copy](../GLOSSARY.md) or a request: the counter's tags are bounded, or
   the counter is a time series zoo. Verified by M09's and M11's
   diagnostics, which read both counters back through the management
   socket.
7. **The outbound wait is bounded before it is retried.** The timeouts are
   the plural properties the M04 builder consumes - every client built
   from the injected builder inherits them; the retry lives on the
   availability method behind the proxy (`@EnableResilientMethods` once,
   `@Retryable` with `includes` naming the wire failure), *outside* the
   M08 boundary's transaction by the contract the M11 doc verified, and
   the exhausted budget answers the `502` problem detail of item 5 rather
   than a hang or a stack trace. The `timeout` attribute is `0` by
   default - no bound at all - so the "is the whole retry program bounded?"
   question is one a review asks aloud. Verified by M11's diagnostics.
8. **The drain is a decision with a length.** The 4.1.1 default is
   graceful - the fresh metadata read - so the review question is not
   "does it drain" but "how long does it promise": the window
   (`spring.lifecycle.timeout-per-shutdown-phase: 10s` here) is one the
   application can actually keep (one bounded availability read plus one
   transaction), and a stop mid-borrow lands the [Loan](../GLOSSARY.md)
   rather than losing it. Verified by M11's drain diagnostic, which booted
   the application, began a borrow, stopped the context and watched the
   row land.
9. **The schema the boot runs is the migration's, on both engines.**
   The migration owns the schema's full state: the V001 and V002
   scripts run before any read, on H2 and on PostgreSQL
   unchanged; `ddl-auto: validate` refuses Hibernate's guesses;
   the columns carry the glossary's words (`member_id`, `copy_id`,
   `barcode`) because an incident is written in the words the domain
   uses. Verified by M05's red/green trap pair (bare `flyway-core` boots
   and migrates nothing) and the M08 races on the second engine.
10. **The gates a green build passed.** The reactor's `verify` is the
   same command a learner runs; the coverage floor applies to the
   Solution module and the Start module opts out of that one gate, while
   the diagnostics profiles skip the floor because a diagnostics run
   asserts requirements, not coverage; and the diagnostics of both halves
   of the Lab state the items
   above as assertions rather than recitation. This is the item that
   makes the checklist *executable*: the deployment's contracts are
   lab-13's red/green pair, one command away.

### The words an upgrade review greps for

The delta tables of the twelve Modules each verified their renames; the
assembled review needs them *in one place*, because an upgrade diff does
not arrive divided by Module. The list below is the compact grep set - what
to search a POM, an `application.yml` and a change for - with the Module
whose jar-read verified each item, so the review states the release, not
just the pattern:

- **Property spellings that resolve but lie**: `spring.http.client.*`
  (deprecation-tagged since 4.0.0, replacement named - M11); 
  `management.otlp.logging.*` (M09); `management.metrics.export.<id>.*`
  (renamed in 3.0, the 2.x spelling an error alias in every later jar -
  M09); `management.trace.http.*` (M09); the singular `spring.http.serviceclient`
  and the singular per-protocol paths (M11's renaming row).
- **Package moves a class-name property cannot compile around**:
  `org.springframework.boot.actuate.health` (M09);
  `org.springframework.boot.autoconfigure.security.servlet.*` (M10).
- **Starter names**: `spring-boot-starter-webmvc-test` (the slice moved out
  of the test starter - M01); the security resource-server rename
  (`spring-boot-starter-security-oauth2-resource-server`, the old name
  deprecated in place - M10); `spring-boot-starter-flyway` (M05).
- **Removals, where compilation already refuses them**: 
  `WebSecurityConfigurerAdapter`, `authorizeRequests()`, `and()`, the
  no-arg configurers, `AntPathRequestMatcher`/`MvcRequestMatcher` (M10);
  `loaderImplementation`, `spring-boot-loader-classic`, `executable`,
  `embeddedLaunchScript`, the `layertools` jarmode (M12);
  `org.springframework.boot.actuate.health.HealthIndicator` (M09).
- **Arrivals a Boot 2 hand would otherwise rewrite**: the framework retry
  and bulkhead (`org.springframework.resilience.annotation`, spring-core's
  `RetryTemplate` - M11); the probe default on (M09, this table);
  structured logging since 3.3 (M09); the AOT cache under its own
  4.1 packaging page, retitled from 3.5's "Class Data Sharing" (M12).

And the attribution sentence the assembled review should quote, because
half the habits above moved **one generation before Boot 4**: graceful
shutdown 3.4, `spring.http.client.*` 3.4, structured logging 3.3,
`httpexchanges` 3.0, `problemdetails` 3.0, CSRF's modern defaults (5.8 and
6.5). Boot 4's own moves are mostly *packaging* (M09, M10, M12's
consistent finding), plus the two behaviour flips this table carries -
the probe default and the AOT-as-deployment-form - so an upgrade note that
blames Boot 4 for a 3.4 flip has named the wrong release, exactly the
misattribution M10's CSRF paragraph modelled against the Boot 2 habit.

### What the assembled application costs

The capstone's honest paragraph, because the earlier Modules named
their prices and assembly is where they are paid together:

- **The build-time freeze is a production commitment.** An AOT boot
  replays the conditions the build evaluated against the build's
  environment - profile names, condition outcomes, the beans that existed
  at the walk - so "which build produced this artifact" is the first
  question an incident asks, and the build's decision ledger is the
  artifact's identity. The build that produced the AOT jar (the walk the
  `process-aot` binding runs on every `package` of the lab-12 solution)
  spends its minutes on *every* form's jar that will ship
  switched, whether or not a native image is ever built - the compute is
  a CI budget line it never was in Boot 2, and the retrain triggers for
  the AOT cache ("as long as the application is not updated *and* the
  same Java version is used") are two, not one.
- **The defaults that flipped are decisions to attribute, not free.** The
  answering probes and the draining shutdown arrive with no
  configuration; the review's job is the sentence "this is on because Boot
  4.1.1 made it on, and nobody turned it off on purpose" - which is the
  sentence an incident asks when the answer surprises.
- **The observability ledger is open by construction and private by
  configuration.** Two counters and the automatic `http.server.requests`
  observation are the baseline; nothing leaves the process while no
  exporter is configured (M09's gate), and the tags' cardinality is the
  whole fraud surface: a [Copy](../GLOSSARY.md)-id tag or a
  [Member](../GLOSSARY.md)-id tag on `library.loans.issued` is a time
  series zoo and a privacy defect in the same line, checked by reading
  the increment rather than by booting.
- **The schema's ownership is load-bearing, not a decoration.** A
  migration-boot process validates before it serves: the deployment that
  hides a snowflake `ddl-auto: update` underneath a healthy boot carries
  a database whose words the application no longer recognises, and the
  validate gate is the refusal that surfaces the divergence the first
  boot after the change - one of the few places where the wrong answer is
  fast.

### Where this Module stops

The line the capstone is obliged to keep drawing: everything this doc
walks is *inside the artifact or its configuration*. The orchestrator
configuring the probes' URLs, the load balancer obeying the readiness
answer, the network between the deployment and its availability service,
the alerting stack reading the counters, the database cluster behind the
JDBC URL - deployment infrastructure, out of scope, the sibling course's
word if the reader needs it named. The same goes for the depth two Modules
already fenced: native-image internals and profile-guided optimization
(M12), circuit breakers and chaos injection (M11). The capstone's question
is whether the assembled artifact, as built, keeps the promises every
Module taught it to make - and the Lab puts the question where this course
keeps every question: in diagnostics, runnable red and green.

## The Lab

The Lab is the library application M12 left: the assembled application, in
its four forms, with the two counters published, the two chains standing,
the retry written in, the drain window named, the migration on both
engines. What no Module ever asked it to do is the one thing this
checklist's item 3 enumerates with the rest: *tell the platform*. The
application answers its probe questions with the states Boot handed it -
liveness and readiness about nothing the process itself decided - and when
the availability service stops answering, the refusals pile up as `502`
problem details, the retry counter climbs, and readiness keeps answering
`ACCEPTING_TRAFFIC` through all of it: the one promised state the twelve
Modules *taught* - M09's published readiness, plain in its doc's snippet -
and no lab ever *built*, the application's will stated as state, the
platform allowed to route on.

Your job on the Start module is to close that gap, the way the operational
checklist reads:

1. **The outage is state, not a symptom.** When the availability service
   becomes unreachable, the application publishes
   `ReadinessState.REFUSING_TRAFFIC` through
   `AvailabilityChangeEvent.publish`, the one API the M09 doc taught -
   so `/actuator/health/readiness` moves to `OUT_OF_SERVICE`; when it
   recovers, the state moves back.
   Nothing else changes: the [Title](../GLOSSARY.md) catalogue
   stays public, the borrows keep answering their `502` problem details,
   the counters keep counting - readiness is the routing question, and the
   refusal question was already answered. What changed is the process
   saying one sentence about itself, and the sentence compiles against
   the 7.0.9 classpath exactly as written:

   ```java
   /**
    * The library refusing or accepting traffic is an event, not a
    * boolean: readiness is published so every listener - the read probe
    * above all - reads the same thing.
    */
   void beginMaintenance(ConfigurableApplicationContext context) {
       AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
   }
   ```

2. **The state is published, not toggled.** The routing answer and the
   refusal answer tell one story by construction: the readiness indicator
   the platform reads is the published state, and the
   `library.availability.retried` counter the operator reads is the same
   outage seen from the Member's side - so the diagnostics can assert
   both without reading any source.
3. **The bounds the deployment supplies are the ones the process keeps.**
   The drain window, the retry geometry and the client timeouts - the
   bounds whose numbers are decisions - arrive from the environment at
   boot instead of living only in the jar's defaults, so the deployment
   can name them
   without a rebuild; the lab carries them in an override
   the diagnostics apply to a real boot.
4. **The assembled checklist is the diagnostic suite.** The Lab's
   diagnostics do not test the assembled application *in sample form*;
   they walk the operational checklist of the previous section, item by
   item - the boots the deployment takes, the probe questions, the refusal
   map, the counters, the drain - so each
   diagnostic states one claim a reviewer would otherwise have taken from
   the wiki.

The requirement is stated by Diagnostic tests - the tests that fail on the
Start module (which answers readiness with a state it never decided) and
pass on the Solution module, which is written from scratch rather than
patched. The command that states the requirement, the one the
[lab index](../labs/README.md) entry teaches:

```bash
./mvnw -f labs -pl lab-13-the-production-capstone-start verify -Pdiagnostics
```

Read the failure messages the way this course reads every delta table row:
each names the promise the assembled application is not keeping - a
readiness that never moved, an outage the platform was not told about, a
bound that only the jar's default supplies, a checklist claim the running
application does not answer. The same tests pass against
`lab-13-the-production-capstone-solution` (and, in CI, against every
Solution module at once with
`./mvnw -f labs verify -Pdiagnostics-solutions`); read that module last,
to see how small the capstone's move is once the state is an event and the
bounds are configuration - then the course itself is reviewed with the
same checklist: every line of the assembled `application.yml`, every
counter, every refusal, every form - each one an answer the build
verifies.

