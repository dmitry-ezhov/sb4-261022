# M11 - Resilience and virtual threads

M10 closed with the borrow authenticated: the chain is a bean, the
[Member](../GLOSSARY.md) is the principal, and the management surface answers
a stranger `401`. The API the Member calls is still M08's boundary over the
database, and one line in it still reaches out of the process: the
availability client M04 introduced asks another service whether a
[Copy](../GLOSSARY.md) is on the shelf before the borrow proceeds. That call
is the fragile edge of the whole application. It waits with no limit, it
fails with whatever the wire says and nothing retries it, and enough
concurrent borrows can exhaust the waiting capacity it shares with
everything else. This Module is the resilience treatment M04's doc promised
that edge: a timeout, a retry with backoff, a concurrency cap - and
underneath all of it, the JDK's cheap threads, so the requests that wait no
longer hold a rare, expensive one. By the end of it you know what
`spring.threads.virtual.enabled` actually swaps (the answer is three beans in
three different layers), where the timeouts now live and which spelling 4.1
answers to, what the retry the *framework itself* ships is - in annotations
and in a `RetryTemplate` - how a bulkhead is one annotation that refuses
with a typed exception, and why the graceful shutdown you had to configure
is now the thing you get without asking. One boundary this course draws,
and this Module is the one that must say it: the *depth* a resilience
engineer wants - circuit breakers, chaos and failure-injection testing -
is the sibling course's subject; this course teaches the mechanism and the
Boot 4 API, and this Module stops there.

## What you knew

In Boot 2 the capacity model was the thread pool. Tomcat kept a pool of
platform threads - `server.tomcat.threads.max` defaulted to **200** - and
one in-flight request held one of them from first byte to last. The pool was
the ceiling: 200 threads busy meant requests queued in `accept-count`
territory behind the 8192 connections the connector allows, and whatever
work was *slow* decided how soon the library stopped serving a Member. Boot
2 had no answer to that but tuning: `server.tomcat.threads.max`,
`server.tomcat.max-connections` and `server.tomcat.accept-count` carried the
same defaults (200, 8192, 100) at the end of the 2.7.18 line as at the
start, which is the polite way of saying the defaults assumed the work is
fast and the queue is short.

The outbound call had no safety net, and you supplied all of it by hand.
Timeouts were a factory setting: a `SimpleClientHttpRequestFactory` with
`setConnectTimeout` and `setReadTimeout` wired into the `RestTemplate` you
built once and hoped nobody rebuilt. Set none and the read timeout is
infinite - the call waits until the remote service, or the network between
here and it, answers. Retry was not Spring's problem: the framework resolved
nothing for it, and the ecosystem filled the hole the same way it had for
years - `spring-retry` with its own `@Retryable` (a different annotation, in
`org.springframework.retry.annotation`, from a *separate* artifact the
framework does not resolve) or a Resilience4j starter, each bringing its own
aspect, its own properties and its own concepts. A bulkhead was a
Resilience4j `Bulkhead` or a `Semaphore` you initialised yourself.

Shutdown was eager by design. `server.shutdown` at the end of the 2.7.18
line defaulted to `immediate`: process death, in-flight requests and all.
The `graceful` value existed (from Boot 2.3 onward) and let Tomcat stop
*accepting* requests while `SmartLifecycle` beans finished in-flight work
within `spring.lifecycle.timeout-per-shutdown-phase`, default 30 seconds -
but you turned it on, and most Boot 2 codebases never did.

You are assumed to know all of that fluently. You are **not** assumed to
know that `spring.threads.virtual.enabled` is not in the 2.7.18 jar at all
and arrived with the 3.2 line, that the property's default is still `false`
in 4.1.1 (switching it on is a decision the change makes, not a default it
inherits), that the graceful shutdown default flipped in **Boot 3.4** and
not in Boot 4, that Boot 4 renamed the timeout properties 3.4 had just
introduced (`spring.http.client.*` to `spring.http.clients.*`), that
Framework 7 grew its own retry - `org.springframework.resilience` and
`org.springframework.core.retry`, both absent from the 6.2.19 jars - with
`@Retryable`, `@ConcurrencyLimit`, `@EnableResilientMethods` and a
`MethodRetryEvent`, or that on this course's JDK 25 the `synchronized`
pinning that haunted virtual-thread rollouts in the JDK 21 era is retired
(JEP 491 shipped in JDK 24).

## What changed

The Boot 4 column is the one this course's build resolves - Spring Boot
4.1.1 over Spring Framework 7.0.9 - every cell read off the jars the build
pulls (`javap` for the annotation defaults and the bean arrangements,
`unzip -l` for what a package holds or does not, Boot's shipped
configuration metadata for the property spells), off the published
`spring-boot-dependencies` POMs of 2.7.18, 3.5.16 and 4.1.1, and - where a
*behaviour* had to be seen, not stated - off the 4.1.1 lab-10 application
booted in front of us (see the verified-versions table in the
[lab index](../labs/README.md)). Two statements the bytecode cannot prove -
the `force` mode's semantics and the JDK's pinning fix - are read off Boot's
and OpenJDK's own documentation, and the doc says so where it leans on them.
The Boot 2 column is the last Boot 2 (2.7), the Boot 3 column the last Boot
3 (3.5). The runtime is JDK 25 throughout - this course's root POM sets
`<java.version>25</java.version>`.

|  | Spring Boot 2 (2.7) | Spring Boot 3 (3.5) | Spring Boot 4 (4.1.1) |
|---|---|---|---|
| The request thread | one platform thread per in-flight request, held to the last byte; the pool is the capacity model: `server.tomcat.threads.max` **200**, `max-connections` **8192**, `accept-count` **100** | the same caps; `spring.threads.virtual.enabled` exists (the 3.2 line introduced it; it is not in the 2.7.18 jar), default **false** - setting it `true` has Tomcat's protocol handler run each request on its own virtual thread | mechanics unchanged from 3.5: the switch's default is still `false`, and `Threading` (`org.springframework.boot.thread`, in the base `spring-boot` jar) still reads the property; on this course's JDK 25 the JDK-level pinning caveat of the 21 era no longer applies |
| Task execution | `spring.task.execution` builds a `ThreadPoolTaskExecutor` pool (core size **8**, prefix `task-`) for `@Async`'s work | the same pool; under the virtual switch the bean becomes a `SimpleAsyncTaskExecutor`; 3.2 adds `spring.task.execution.simple.concurrency-limit` (+ `reject-tasks-when-limit-reached`), 3.5 adds `spring.task.execution.mode` (`auto`/`force`) | 4.1 keeps all of it and adds `spring.task.execution.propagate-context`; `TaskExecutionAutoConfiguration` still lives in `spring-boot-autoconfigure` - the packaging that moved for security in M10 did not move here |
| The outbound timeout | no Boot property. A factory per `RestTemplate`/`WebClient`, set by hand (`SimpleClientHttpRequestFactory.setReadTimeout`); unset means the read waits forever | the hand-setting holds most of the line: the `spring.http.client.*` keys first appear in **3.4** (absent from the 3.3.12 metadata) | the 3.4 spelling is deprecated since 4.0 (`since: 4.0.0`, with its replacement named, in the 4.1.1 metadata); the 4.1 spelling is the plural - `spring.http.clients.connect-timeout`, `spring.http.clients.read-timeout` - and `HttpClientAutoConfiguration` publishes the settings as an `HttpClientSettings` bean that every `RestClient.Builder` inherits |
| Retry | nothing in the framework: `spring-retry` (`org.springframework.retry.annotation.Retryable`, separate artifact) or a Resilience4j starter, each with its own aspect | still nothing - we listed `org.springframework.resilience` and `org.springframework.core.retry` in the 6.2.19 jars: not there | Framework 7.0 answers it: `@EnableResilientMethods`, `@Retryable` and `@ConcurrencyLimit` in `org.springframework.resilience.annotation`, a `MethodRetryEvent` to listen for, and a programmatic `RetryTemplate` in `org.springframework.core.retry` (spring-core) - resolved by the build, zero extra dependencies |
| The bulkhead | Resilience4j `Bulkhead`, or a semaphore written by hand | the same; 3.2 adds the executor-level spelling `spring.task.execution.simple.concurrency-limit` (with `reject-tasks-when-limit-reached`) for the simple executor | `@ConcurrencyLimit(limit = n, policy = BLOCK\|REJECT)` - BLOCK holds the caller, REJECT refuses with `InvocationRejectedException`, which extends `java.util.concurrent.RejectedExecutionException`, the rejection type the executor contract already speaks |
| Graceful shutdown | `server.shutdown` defaults to **`immediate`**; `graceful` is opt-in (from Boot 2.3), draining in-flight requests for `spring.lifecycle.timeout-per-shutdown-phase`, default **`30s`** | the same opt-in through 3.2 and 3.3 - **3.4 flips the default to `graceful`** (3.4.13 reads `graceful`) | the default stays `graceful`; in 4.1.1 the property lives in the new `spring-boot-web-server` module (`org.springframework.boot.web.server.autoconfigure.ServerProperties`) and the drain timeout default is still `30s` - what Boot 4 changed here is packaging, not behaviour |
| The JDK underneath | Boot 2's era ends on JDK 8-17: the JVM has no cheap thread, so *every* concurrency model is a pool | virtual threads exist: JEP 444 finalised them in JDK 21, and from 3.2 Boot can hand the web server and the task executor to them - with the `synchronized`-pinning caveat the era carried | this course's build runs **JDK 25**, where JEP 491 (shipped in JDK 24) stopped `synchronized` from pinning the carrier thread - the caveat Boot 3's virtual rollouts had to work around is retired; native frames still pin |

Seven rows, and the sum is one sentence: **what Boot 2 built out of scarce
pools and hand-wired guards, Boot 4 gets by flipping one property and
annotating the method.** Three kinds of change hide in that sentence:

- **The first kind is a switch.** Virtual threads and graceful shutdown are
  *arrangements* that existed before, gated behind properties; what changed
  across the three generations is how much of them you get for free. The
  virtual switch flips three beans at once - the protocol handler's executor
  in the web server, the application task executor, and the scheduler - and
  the `@ConditionalOnThreading(Threading.VIRTUAL)` condition (the annotation
  lives in `spring-boot-autoconfigure`'s
  `org.springframework.boot.autoconfigure.condition` package) is the
  mechanism in all three. Graceful shutdown needed no switch in the end: the
  default moved one generation before last (3.4), which is why the colleague
  who says "Boot 4 turned it on" is right about the *state* and wrong about
  the *release*.
- **The second kind is a renaming with a restraint.** `spring.http.client.*`
  arrived in 3.4 - late enough that a Boot 3 codebase may never have typed
  it - and 4.0 deprecated it in favour of the plural
  `spring.http.clients.*`. The 4.1.1 metadata still carries the singular
  names, each marked deprecated with its replacement spelled out; deprecated
  binds, removal does not. A 4.1.1 property file that says the 3.4
  spellings works - and advertises a migration the codebase forgot to
  finish.
- **The third kind is an arrival.** Everything in the retry and bulkhead
  rows' Boot 4 column did not exist anywhere in Boot 3: not in the framework
  at 6.5, not behind a starter Boot ships. Framework 7 grew
  `org.springframework.resilience` (the annotations, their post-processors
  and the event) and - in spring-core, the jar in every build -
  `org.springframework.core.retry` with a `RetryTemplate` and a
  `RetryPolicy`. The Boot 2 instinct is to reach for spring-retry or
  Resilience4j; in a 4.1.1 POM that line is now a duplicate: the framework
  ships one, and it is two jars you already resolve.

## How it works now

### Virtual threads: one property, three layers

The whole switch is a property, and its default is worth reading twice:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

The default is `false` in 4.1.1, as in every release since the 3.2 line -
the read is off the `Threading` enum's property binding in the base
`spring-boot` jar. With it set, three independent pieces of the
auto-configuration exchange their beans, each gated by
`@ConditionalOnThreading(Threading.VIRTUAL)`:

1. **The web server.** In `spring-boot-tomcat`, the factory customizer
   installs Tomcat's `VirtualThreadExecutor`
   (`org.apache.tomcat.util.threads.VirtualThreadExecutor`) as the protocol
   handler's executor, with thread name prefix `tomcat-handler-` - we read
   the prefix off the customizer's bytecode. Tomcat's own documentation of
   the executor is blunt about what that means: *a new virtual thread to
   execute each task*. The 200-thread pool stops governing the worker
   count. What still governs is what the connector governs:
   `max-connections` 8192 and `accept-count` 100 are real across all three
   generations we checked (2.7.18, 3.5.16, 4.1.1) - the difference is that
   a request is now cheap to *hold*, not merely cheap to *create*, so the
   pool ceases to be the capacity model. What once throttled concurrency
   by accident - 200 platform threads, every one of them held when the
   availability service was slow - no longer throttles anything, which is
   exactly why the bulkhead row exists further down this page.
2. **The application task executor.** Where the platform condition builds
   a `ThreadPoolTaskExecutor` pool (core size 8, prefix `task-`), the
   `VIRTUAL` condition on `TaskExecutionAutoConfiguration` - still in
   `spring-boot-autoconfigure`, the jar that did *not* move when M10's
   Security auto-configuration did - swaps it for a
   `SimpleAsyncTaskExecutor` built from Boot's
   `SimpleAsyncTaskExecutorBuilder`. The `spring.task.execution.pool.*`
   tuning belongs to the platform form; the virtual form's knobs are the
   simple ones - `spring.task.execution.simple.concurrency-limit` and
   `reject-tasks-when-limit-reached`, present since 3.2.
3. **The scheduler.** The scheduling side has the same pairing: under
   `VIRTUAL`, the auto-configured `taskScheduler` is a
   `SimpleAsyncTaskScheduler` (Boot builds it through
   `SimpleAsyncTaskSchedulerBuilder`, whose beans sit next to the pooled
   ones in 4.1.1's `TaskSchedulingConfigurations`). Spawns rather than
   pools - the same shape as the executor above it.

One property, three beans - and no Java in the change. The cost is
conceptual, and the delta table's first row already carries it: code that
reads the executor as a queue - counts its pool, sizes batches by it, times
work against its saturation - reads a bean that no longer has a stable
thread count, because a virtual executor has none to report. What used to
bound concurrency accidentally now bounds nothing, so deliberate caps
become policy rather than insurance: the connector's above, the bulkhead
annotation's below. The migration concern that made JDK 21 rollouts cautious is gone
with JEP 491 - on this course's JDK 25, blocking inside `synchronized` no
longer pins the carrier - and what remains unmigrated is narrow: the call
whose stack bottoms out in a native frame. For a codebase like this library
- a blocking `RestClient` behind MVC, M08's database transactions - that is
not a list you have to go looking for.

The one property *around* the switch is `spring.task.execution.mode`:

```yaml
spring:
  task:
    execution:
      mode: force
```

`auto`, the default, means Boot provisions its executor only when nothing
competes: define an `Executor` bean of your own and the auto-configured
`applicationTaskExecutor` backs off. `force` means regardless: the
auto-configured bean wins everywhere Boot would have used it - `@Async`
execution, the `bootstrapExecutor` for task startup - and no custom
`SimpleAsyncTaskExecutor` someone declared changes that. Boot 3.5
introduced the mode property (the 3.4.13 metadata has none); 4.1.1 keeps it
and adds one more, `spring.task.execution.propagate-context` - new in 4.1,
absent from 4.0.8's metadata - deciding whether context propagation rides
each task for you. Both sentences are reads of the 4.1.1 property metadata,
which is what a review of a *properties* migration reads rather than
reconstructs from memory.

The observation that separates a real switch from a decorative one is a
thread dump, and M09's management socket is where we took ours: the 4.1.1
application we booted for this Module (Tomcat 11.0.24) came up with the
flag on, and the `/actuator/threaddump` M10's runtime-behaviour section
already taught to authenticate listed **22 threads and not one
`tomcat-handler-` worker** - no idle guard pool waits warm, because a
virtual handler exists for the length of the request and exists for nothing
after. There was nothing in-flight at dump time; on a platform-pool boot
the same surface shows the idle workers parked. The Lab asks for exactly
this switch to be turned and the idle-pool absence to be observed, which is
a runtime claim, not a compile-time one - and it is why the flag must be
*on* in the change's `application.yml`, where a reviewer's first read finds
it.

### Timeouts: an `HttpClientSettings` bean and two spellings

The availability call is the fragile edge because it waits, and waiting
without a bound is the defect that turns an upstream outage into an
unavailability of the library itself. The Boot 2 fix was factory-local: a
`SimpleClientHttpRequestFactory`'s `setReadTimeout` on one `RestTemplate`
bean. Boot 3.4 gave the setting a property home (`spring.http.client.*`);
4.0 deprecated that spelling; 4.1 carries the plural - and this is a read,
not a reconstruction: the module's `spring-configuration-metadata.json`
lists both, the singular entries each carrying

```json
"deprecation": {"replacement": "spring.http.clients.read-timeout", "since": "4.0.0"}
```

and the plural entries carrying none. The 4.1 spellings are the ones the
course's own lab already carries in its `application.yml` - the property
migration was made here before it was taught here:

```yaml
spring:
  http:
    clients:
      connect-timeout: 500ms
      read-timeout: 1s
```

What makes the property worth more than its two lines is where it lands. In
4.1.1 the HTTP client's auto-configuration has its own module:
`HttpClientAutoConfiguration` (spring-boot-http-client, package
`org.springframework.boot.http.client.autoconfigure`) exposes an
`HttpClientSettings` bean - a record in `org.springframework.boot.http.client`
holding the timeouts plus the redirect policy (`HttpRedirects`) and cookie
handling (`HttpCookieHandling`) - and `spring-boot-restclient`'s
`RestClientAutoConfiguration` consumes it in its
`restClientBuilderConfigurer`. That is why the `RestClient.Builder` the
M04 code injects picks up the two lines above *without anyone wiring a
factory*: the builder's configurer carries the settings, so every
`RestClient` built from the injected builder times out the same way. The
Boot 2 review question was "which of the three `RestTemplate` beans set a
read timeout and which forgot?"; the 4.1.1 question is "is the property
there or not", because it now applies to everything the builder builds. The
same reach has an edge, and it is the edge the review searches for: a
`RestClient` built *around* the injected builder - constructed by hand, or
from a builder whose configurer was replaced - carries none of the
settings, and is the one call base in the codebase that still waits
forever. The review's search is the builder's provenance, not the factory
arguments.

### Retries: the framework ships one now

A retry is not a loop, it is a *policy*, and Framework 7 gives the policy a
home in two surfaces at once:

- The annotation surface: `org.springframework.resilience.annotation`, in
  spring-context 7.0.9 - absent from the 6.2.19 jar, which is the bytecode
  proof this is a Framework 7 arrival and not a Boot 4 repackaging.
- The programmatic surface: `org.springframework.core.retry`, in
  spring-core 7.0.9 - a jar every Spring build already resolves - likewise
  absent from 6.2.19.

The annotated form is the one the Lab uses, and it is two declarations
underneath the method M04's fragile edge lives in:

```java
@Configuration
@EnableResilientMethods
public class ResilienceConfiguration {

}
```

```java
@Service
public class AvailabilityChecker {

    private final AvailabilityClient client;

    AvailabilityChecker(AvailabilityClient client) {
        this.client = client;
    }

    @Retryable(
            includes = ResourceAccessException.class,
            maxRetries = 2,
            delay = 100,
            jitter = 50,
            multiplier = 2,
            maxDelay = 1000)
    Availability availability(String copyId) {
        return this.client.availability(copyId);
    }
}
```

Every attribute is a 7.0.9 fact, read off the annotation's constant pool and
its published contract:

- `includes` names what the retry **may** retry. The Lab's fragile edge
  fails on the wire, and the wire failure under an MVC client is
  `ResourceAccessException` - the M04 contract's own exception for "could
  not connect" and "connection dropped". The match is generous: the
  annotation's contract matches the thrown exception *and its nested
  causes*, so a wire failure wrapped by another layer still retries.
  `excludes` and a `predicate` (a `MethodRetryPredicate`) are the two tools
  for refusing retries of what a retry cannot fix. The default of
  `includes` is empty - and empty does **not** mean inert: the contract is
  explicit that the default "leads to a retry attempt for *any* exception",
  which is why naming types is the deliberate act and going without is
  silent generality. `value` is `includes` by another name.
- The geometry is arithmetic a reviewer can check on paper. `maxRetries`
  counts *retries*: the contract's own sentence is that total attempts are
  1 + `maxRetries` - so the default of **3** means four invocations, and
  the Lab's `2` means three. `delay` is the base pause after the initial
  attempt, default **1000** (in `timeUnit`'s default of
  `MILLISECONDS`); the Lab sets 100ms because the availability service
  blips for fractions of a second, not for an afternoon. `jitter` is random
  slack that is subtracted *or added* to each calculated delay - between
  `delay - jitter` and `delay + jitter`, never below the base delay, the
  default **0** - and it exists because a retry *synchronised* is a retry
  *amplified*: a service that recovers in 200ms and 50 borrows that all
  relaunched on the identical schedule arrive together again. `multiplier`
  (default **1.0**, a fixed delay) grows the delay per attempt - ours is
  2, so 100ms, 200ms - and `maxDelay` (default unlimited,
  `Long.MAX_VALUE`) is its ceiling; ours caps at 1000ms, which this
  schedule never reaches.
- `timeout` bounds the *whole* retry program: the initial invocation and
  every retry attempt including the delays. The default is **0**, which
  the contract reads as "no timeout applied" - an unbounded retry program,
  the exact shape the availability call should not be unless somebody
  decided it.
- The `maxRetries`, `timeout`, `delay`, `jitter`, `multiplier` and
  `maxDelay` attributes each have a String sibling - `delayString` and
  friends - which accept `${...}` placeholders and SpEL: the property-driven
  form is built in, and a non-empty string overrides its twin number.

The wiring behind the annotation is the mechanism every proxy annotation in
this course has shared, with one order fact the javadoc states and a
review should quote: the advice runs **outside** `@Transactional` and
`@Cacheable` (each retry attempt starts a fresh transaction or re-checks
the cache) and **inside** `@Async` (all attempts run on the async
executor's thread). For the library that ordering is the load-bearing one:
the borrow's transaction is the M08 boundary's, and a retried read outside
it opens *each attempt's* transaction fresh rather than holding one across
the waits - the same contract that says a retried *write* has to be written
to survive being executed twice. The trap is the same one M01's module map
named for proxy annotations everywhere: **the retry rides the proxy**. A
call that enters from another bean crosses the `SimpleRetryInterceptor` the
`RetryAnnotationBeanPostProcessor` installed, and retries. A call inside
the same class - `this.availability(copyId)` - never crosses the proxy, so
the annotation is dead source and the wire failure propagates as if no one
had written it. The Lab's borrow already holds the client through the
container, the way M04's design required; the change must not introduce a
call path that bypasses it.

The programmatic form is the same policy without the proxy - spring-core's
`RetryTemplate`, for the call site whose shape would put the annotation
where no one enters from outside:

```java
@Component
public class BudgetedReader {

    private final AvailabilityClient client;

    BudgetedReader(AvailabilityClient client) {
        this.client = client;
    }

    Availability read(String copyId) throws RetryException {
        RetryTemplate template = new RetryTemplate(RetryPolicy.withMaxRetries(2));
        return template.execute(() -> this.client.availability(copyId));
    }
}
```

`RetryPolicy.withMaxRetries(2)` draws on the same arithmetic as the
annotation (its `builder()` carries `delay`, `jitter`, `multiplier`,
`maxDelay` and the includes/excludes lists). What `execute` throws when the
budget runs out is a `RetryException` - caught at the boundary, where M08's
problem-detail handling already catches what the borrow refuses - and it
carries the record a reviewer would ask for: `getRetryCount()` is how many
attempts the budget spent, `getLastException()` is the one the caller must
see. The lambda implements `Retryable<R>` (`R execute() throws Throwable`),
and its throws clause is the point: a retry cannot catch what the callee
refuses to throw.

A retry without an *observer* is a silent deficit, and Framework 7 wires
the observer into the event mechanism the application already has. Each
exception retry processing encounters publishes a `MethodRetryEvent` - it
extends `MethodFailureEvent`, which extends `ApplicationEvent`, so the M09
listening shape is all it takes:

```java
@Component
public class RetryMeter {

    private final MeterRegistry registry;

    RetryMeter(MeterRegistry registry) {
        this.registry = registry;
    }

    @EventListener
    void onMethodRetryEvent(MethodRetryEvent event) {
        this.registry.counter("library.availability.retried", "method",
                event.getMethod().getName()).increment();
    }
}
```

This is M09's metric lesson re-run one process outward:
`library.loans.issued` answers *how many Loans*,
`library.availability.retried` answers *how many times the library asked
again* - a count an operator reads as an incident signature, because a
spike in retries *is* the availability outage, seen from the Member's
side. `event.getMethod().getName()` names the annotated method, so one
listener serves every resilient method in the application;
`isRetryAborted()` distinguishes an attempt the policy made from the
failure the policy refused to retry; and `getFailure()` - on the event and
its parent alike - lets the counter be labelled by cause, so the wires that
fail and the refusals that were refused are not folded into one number.

### The bulkhead is an annotation now

Virtual threads withdrew the *pool* as the capacity model - "the library is
serving four thousand concurrent requests" is now a sentence about the
connector, not about a ceiling - which makes the bulkhead, the *local* cap
on a self-inflicted stampede, load-bearing in a way it never was when 200
platform threads throttled everything by accident. The annotation is two
attributes long and sits beside `@Retryable`:

```java
@Service
public class ShelfSearcher {

    private final AvailabilityClient client;

    ShelfSearcher(AvailabilityClient client) {
        this.client = client;
    }

    @ConcurrencyLimit(limit = 2, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
    Availability search(String copyId) {
        return this.client.availability(copyId);
    }
}
```

- `limit` (its synonym `value` takes the same int) caps the concurrent
  executions of the annotated method.
- `policy` chooses what to do at the cap: **`BLOCK`** - the default, read
  off `ThrottlePolicy.BLOCK` - makes the caller wait; **`REJECT`** refuses
  with `InvocationRejectedException`, and the point of that class is its
  parentage: it extends `java.util.concurrent.RejectedExecutionException`,
  so the refusal surfaces *through the type the executor contract already
  speaks* - the one `CompletableFuture` failure handling and `@Async`'s
  task-queue mechanics already answer to - rather than through a new
  exception nobody's handler expects.

The wiring is the sibling of the retry's:
`ConcurrencyLimitBeanPostProcessor`, in the annotations' own package,
`org.springframework.resilience.annotation` (the interceptors the two
post-processors install, `SimpleRetryInterceptor` and friends, live in
`org.springframework.resilience.retry`). Where an
executor-level cap is the right tool - library work driven *through* the
task executor, rather than through a request-driven use case - the 3.2
property still does that job on the simple executor the virtual switch
installs: `spring.task.execution.simple.concurrency-limit` with
`reject-tasks-when-limit-reached`, and
`spring.task.scheduling.simple.concurrency-limit` does it for the scheduler
since the same generation. For the borrow use case the annotation on the
resilient method is the cap the caller *meets* rather than the one the
*pool* used to trade on, and the rejection maps onto the problem-detail
surface M08 already built: a saturated dependency answers a problem detail,
not a stack trace - the exact status is the diagnostics' line item, not the
annotation's.

The cost to name is the BLOCK default's trade: latency for service. A
caller that waits is a request still occupying its thread - now a cheap
one. Under a Boot 2 pool that wait ate one of 200 scarce workers, and a
stampede that blocked on a slow availability service could *starve the
pool* and take unrelated calls down with it; under virtual threads the wait
is affordable, and BLOCK vs REJECT becomes the considered choice it should
always have been: hold the Member's borrow for the availability service's
response, or refuse it promptly and let it come again.

### Graceful shutdown: the default moved one generation last

The delta table's last row is the one with a *date*. The Boot 2-era
sentence "graceful shutdown is something you configure" was written because
the default said `immediate`: we read the binding across 2.7.18, 3.2.12 and
3.3.12 and each answers `immediate`; across 3.4.13, 3.5.16 and 4.1.1 each
answers `graceful`. **The flip happened in Boot 3.4.** A 3.4-or-later (and
4.1.1) application stops *accepting* requests at shutdown, lets in-flight
ones complete, and gives the `SmartLifecycle` beans
`spring.lifecycle.timeout-per-shutdown-phase` - default **30s**, unchanged
across every generation we opened - to finish in; a bean whose work is done
early can cut the window short through its `stop(Runnable)` callback. The
*absence* of `spring.lifecycle.timeout-per-shutdown-phase` in the Lab's
`application.yml` is not a gap in the change: the **default** (`graceful`,
30s) is what the absence means, and naming the window the application is
actually willing to promise is the part of the change that was a decision.

What Boot 4 did do here is the packaging move this course watched in M10
and is the reason the row says what it says: `ServerProperties` left
`spring-boot-autoconfigure` for the new `spring-boot-web-server` module,
package `org.springframework.boot.web.server.autoconfigure` - the same
migration the security auto-configuration took, applied to the web server.
A review that searches for a class-named property (`spring.autoconfigure.exclude`,
or any FQCN in a property value) answers to the new spellings, while the
*behaviour* it configures never moved.

## The Lab

The Lab is the library application M10 left: the borrow commits atomically,
the [Copy](../GLOSSARY.md) guard is one statement, the
[Member](../GLOSSARY.md) is the principal, the probes answer and the metric
is published. What it does not have is any of what this Module taught. The
availability client M04 introduced asks the other service whether a
[Copy](../GLOSSARY.md) is on the shelf, and it waits unbounded, retries
nothing and shares its concurrency with nothing else's; the threads that
serve the borrows are the 200-strong platform pool; shutdown answers
SIGTERM by dying mid-borrow.

Your job on the Start module is the three moves this Module taught, in this
order:

1. **Put the requests on virtual threads.** Set
   `spring.threads.virtual.enabled=true` and *observe* what it switched -
   on the thread dump surface M09 authenticated and M10's runtime behaviour
   re-used: with the flag on, no idle `tomcat-handler-` worker appears in
   `/actuator/threaddump`, because the guard pool Boot 2 taught is gone.
   No Java changes; the property does not exist in the Boot 2 era at all,
   which is why this row is about a property, not a code style.
2. **Bound and retry the availability call.** The timeouts go on the
   property (`spring.http.clients.*`, the plural spellings, applied
   wherever the M04 `RestClient.Builder` - the injected one, whose
   configurer carries the settings - builds a client); the retry goes on
   the method the borrow calls: `@EnableResilientMethods` once in a configuration,
   `@Retryable` on the availability method with `includes` naming the wire
   failure (`ResourceAccessException`), and `maxRetries`/`delay`/`jitter`/
   `multiplier`/`maxDelay` set by deliberation, not left defaults - and the
   call entering from outside its bean, so the proxy sees it. The borrow's
   failure when the budget exhausts answers on the M08 problem-detail
   surface, not as a stack trace and never as a hang.
3. **Make shutdown drain.** The default is already `graceful` in 4.1.1 -
   so the move is proving it: a test that keeps a [Loan](../GLOSSARY.md)
   in flight across the stop and asserts it lands, and naming the drain
   window the application is actually willing to promise
   (`spring.lifecycle.timeout-per-shutdown-phase`) rather than accepting
   thirty seconds nobody discussed.

The [Reservation](../GLOSSARY.md) endpoints will inherit all three: a
Reservation is a Member's wait on an upstream outcome - a [Title](../GLOSSARY.md)
they cannot borrow yet - and putting a wait under a timeout, a retry and a
cap is the shape the three moves teach, whatever the outbound call turns
out to be. The Lab's diagnostics do not test the Reservation feature; the
shape they teach is the shape it will use.

The requirement is stated by Diagnostic tests in the `diagnostics` package
- the tests that fail on the Start module (which waits unbounded, retries
nothing and dies mid-borrow) and pass on the Solution module, which is
written from scratch rather than patched. The command that states the
requirement, the one the [lab index](../labs/README.md) entry teaches:

```bash
./mvnw -f labs -pl lab-11-resilience-and-concurrency-start verify -Pdiagnostics
```

Read the failure messages the way this course reads every delta table row:
each names the behaviour that is missing - a wire failure that is retried
nowhere, a drain that leaves a [Loan](../GLOSSARY.md) dangling, a call that
never gets its timeout. The same tests pass against
`lab-11-resilience-and-concurrency-solution` (and, in CI, against every
Solution module at once with
`./mvnw -f labs verify -Pdiagnostics-solutions`); read that module last,
to see how small the resilient story is once the timeout lives in a
property and the retry lives on one annotation.

## Quiz

Five questions about the delta table above and the resilience arithmetic it
equips you to review. Answer first; the answers say why, not just what.

1. **About the request-thread row: the upgrade memo for the library
   proposes `spring.threads.virtual.enabled=true` and carries three
   promises. (1) "The borrow stops holding a scarce worker while it waits
   on the availability service - under the 200-thread pool, a slow
   upstream emptied the library's serving capacity along with the
   threads." (2) "The rollout audit the JDK 21 era demanded is settled
   for us: on this JDK nothing pins any more." (3) "Under the flag, the
   connector can accept far more connections than the 8192 we configured
   in the Boot 2 era." Which promise survives the Module's read, what does
   each of the others get wrong, and what did the flag actually flip?**

   Promise (1) is the Module's thesis and it survives. In Boot 2's model
   one in-flight request held one of `server.tomcat.threads.max`'s 200
   platform workers from first byte to last, so the borrows that *waited*
   - and the availability call waits - were what emptied the pool; a
   stampede blocked on a slow availability service could starve it and
   take unrelated calls down with it. The flag makes a held request cheap
   - cheap to create, cheap to *hold* - so the pool stops being the
   capacity model.

   Promise (2) is half right, and the half that fails is the sentence.
   What this course's JDK 25 retired is one pinning *cause*: JEP 491
   (shipped in JDK 24) stopped blocking inside `synchronized` from pinning
   the carrier - the caveat Boot 3-era virtual rollouts worked around.
   What remains is narrow but real: the call whose stack bottoms out in a
   *native* frame still pins. For a codebase like this library - a
   blocking `RestClient` behind MVC, M08's database transactions - the
   audit is short ("not a list you have to go looking for"); but "nothing
   pins any more" is not a sentence the Module lets you write.

   Promise (3) is false by pegging the promise to the wrong bean. The
   property - introduced with the 3.2 line (it is not in the 2.7.18 jar at
   all), read in 4.1.1 by `Threading` in the base `spring-boot` jar,
   default still `false` - changes the *executors*, not the connector:
   `@ConditionalOnThreading(Threading.VIRTUAL)` makes spring-boot-tomcat's
   customizer install Tomcat's `VirtualThreadExecutor` as the protocol
   handler's executor - a new virtual thread to execute each task, name
   prefix `tomcat-handler-` - and the same condition swaps the application
   task executor for a `SimpleAsyncTaskExecutor` and the scheduler for a
   `SimpleAsyncTaskScheduler`. The promise's arithmetic reads the old
   model back: with the executor a spawner, `server.tomcat.threads.max`
   caps a pool that no longer exists - a dead line a review should strike,
   not a ceiling to trust. What still bounds *arriving* work is what the
   connector has always held: `server.tomcat.max-connections` (default
   8192) caps accepted connections and `server.tomcat.accept-count` (100)
   caps the queue behind them - both unchanged in the 2.7.18, 3.5.16 and
   4.1.1 jars we read. The consequence is the Module's thesis in one
   line: capacity management moves from the accidental throttle (200
   platform threads) to deliberate caps on the *work* - the connector's
   above, the retry row and the bulkhead row below.

2. **About the timeout row: the team copies a Boot 3.4 blog post and sets
   `spring.http.client.read-timeout` on the 4.1.1 application; meanwhile a
   `RestClient` built from the injected `RestClient.Builder` times out
   though nobody can name where the values came from, and a review memo
   declares the two lines "the fix that bounds every outbound call this
   library can make". Name the two spellings as 4.1.1's metadata presents
   them, explain the mechanism that makes the injected builder carry the
   timeouts without per-client wiring, and name what the memo's sentence
   misses.**

   The singular form is the blog's key and the deprecated one;
   `spring.http.client.*` first appeared in the 3.4 metadata (3.3.12's has
   no such key) and 4.1.1's metadata still carries it - each of which cites
   `"since": "4.0.0"` and names its replacement -
   so the blog's value still resolves, and the *typed* form is the
   **plural**: `spring.http.clients.connect-timeout` /
   `spring.http.clients.read-timeout`. The mechanism behind the builder is
   the second one behind the property: `HttpClientAutoConfiguration`
   (spring-boot-http-client, package
   `org.springframework.boot.http.client.autoconfigure`) publishes an
   `HttpClientSettings` bean - a record in
   `org.springframework.boot.http.client` carrying the timeouts plus
   redirect policy and cookie handling - and `spring-boot-restclient`'s
   `RestClientAutoConfiguration` consumes it in
   `restClientBuilderConfigurer`, so the injected builder shows up with
   the settings applied to everything it builds. The same reach is the
   fence the memo's sentence falls inside: "every outbound call" overreaches,
   because the settings ride the builder's *configurer* - a `RestClient`
   built *around* the injected builder (a hand-`new`ed factory, or a
   builder whose configurer was replaced) skips the configurer, carries
   none of the settings and still waits forever - so the review's search
   is the builder's provenance, not the factory arguments. And a timeout
   is a *bound*, not a repair: the lines stop the wait, they replay
   nothing - a call that met its bound still fails the borrow, which is
   why the replay of a failed ask is a declaration of its own on the
   retry row, and the Lab's second move is bound *and* retry, plural.

3. **About the retry row: a method is annotated
   `@Retryable(includes = ResourceAccessException.class, maxRetries = 2,
   delay = 100, multiplier = 2, maxDelay = 1000)`. Three reactions cross
   the review: a colleague who called it with
   `this.availability(copyId)` from the same class; an upgrade memo that
   says the annotation "must have an `includes` or it retries nothing";
   and a junior who expects a bare `@Retryable` to retry a method that
   throws `IllegalStateException`. Who is right, how many attempts run,
   and with what delays?**

   The self-caller is the mistake; the other two readings are right, and
   the arithmetic answers the third question. Attempts are attempts, not
   rounds: `maxRetries` counts retries and the contract's own sentence is
   that total attempts are 1 + `maxRetries` - so this annotation means
   **three total attempts**: the first invocation, then retries 100ms and
   200ms later (multiplier 2 grows the delay; `maxDelay=1000` is the
   ceiling this schedule never reaches, and `jitter`, whose unset default
   is 0, would have added the per-attempt slack had the schedule needed
   it). The memo states the opposite of the contract: a bare `@Retryable` (default
   `includes`, empty) does **not** retry nothing - the annotation's
   contract says the default "leads to a retry attempt for *any*
   exception" - which is why the junior is right and why the deliberate
   naming matters: `includes` (and its twin `value`), `excludes` and a
   `predicate` exist to narrow a default that is otherwise *generous*,
   matching the thrown exception and its nested causes. The self-call is
   the proxy trap: the wiring is a `RetryAnnotationBeanPostProcessor` that
   wraps the bean and a `SimpleRetryInterceptor` that only sees calls which
   *cross the proxy* - `this.availability(...)` never crosses, so the
   annotation is dead source and the wire failure propagates as if no one
   had written it. The fix is what M04's design already had: the caller
   holds the client through the container. One more order fact the
   annotation's contract states and the borrow review should check: the
   retry advice runs *outside* `@Transactional` and `@Cacheable` (each
   attempt gets a fresh transaction) and *inside* `@Async` (all attempts
   on the executor's thread).

4. **About the bulkhead row: a `@ConcurrencyLimit(limit = 2,
   policy = REJECT)` on the availability check starts refusing a burst of
   concurrent borrows. The operator asks for a saturation gauge and is
   handed the Boot 2 dashboard's busy-worker count; a colleague proposes
   logging the refusals at debug as noise. With which type does the cap
   refuse, why does that type's *parentage* matter, what does the
   busy-worker gauge read on this boot, and where does the saturation
   actually surface?**

   The refusal is `org.springframework.resilience.InvocationRejectedException`,
   read off the 7.0.9 class file, and what matters is what it extends:
   `java.util.concurrent.RejectedExecutionException` - the unchecked type
   the executor contract already speaks for "the work does not fit". That
   parentage is why the colleague's log line is the wrong call twice
   over: the refusal composes with the plumbing the application already
   has - `CompletableFuture` failure handling, `@Async`'s task-queue
   mechanics, and (here) the problem-detail surface M08 built, which maps
   a task-shaped refusal of a saturated resource to an answer the
   [Member](../GLOSSARY.md) reads rather than a stack trace - so a
   saturated dependency answered in problem details *is* the observable
   contract, and a debug line suppresses the one signal the cap produces.
   The diagnostics go at exactly that: the exhausted budget answered on
   the M08 surface, not as a stack trace and never as a hang.

   The gauge reads nothing, and that reading is itself the row's
   consequence. With the flag on, the task executor is not a pool: the
   same `@ConditionalOnThreading(Threading.VIRTUAL)` that swapped the
   Tomcat workers swaps the pooled `ThreadPoolTaskExecutor` for a
   `SimpleAsyncTaskExecutor` that spawns rather than pools - and a
   virtual executor has no stable thread count to report. A dashboard
   that reads the executor as a queue - counts its pool, sizes batches by
   it, times work against its saturation - measures a queue that no
   longer exists; the same holds on the request side, where
   `/actuator/threaddump` lists no idle `tomcat-handler-` worker to
   count. What surfaces instead is what the Module instrumented: the
   cap's refusals, typed above and answered as problem details, and one
   row up the retry counter - `library.availability.retried`, counted on
   the `MethodRetryEvent` the lab's solution listens for - because a
   spike in asked-again is the upstream outage, seen from the
   [Member](../GLOSSARY.md)'s side rather than the executor's.

   The BLOCK default - which a bare `@ConcurrencyLimit(2)` gets,
   `ThrottlePolicy.BLOCK` being the annotation's own default and
   `{BLOCK, REJECT}` the whole enum the 7.0.9 constant pool carries -
   trades *latency for service*: the waiting caller holds its request,
   and the borrow answers late rather than not at all. Under a Boot 2
   pool that wait was expensive - the request held one of 200 scarce
   platform threads, and a stampede that blocked on a slow availability
   service could starve the pool and kill unrelated calls; the Module's
   virtual-thread row changes the price of the wait to a cheap thread,
   which is what makes BLOCK vs REJECT the considered choice this Module
   presents rather than a reflex. The executor analogue -
   `spring.task.execution.simple.concurrency-limit` with
   `reject-tasks-when-limit-reached` - caps work that runs *through* the
   task executor; the annotation caps work that runs through the use case
   the caller reaches.

5. **About the graceful-shutdown row: in a post-mortem, a colleague writes
   "Boot 4 finally turned graceful shutdown on for us." State what the
   sentence gets right and where it goes wrong (the release is part of
   the answer), how this course checked that attribution, and what Boot 4
   *itself* changed in the area.**

   Right: the 4.1.1 application *does* drain without anyone having set
   `server.shutdown=graceful` - at shutdown the server stops accepting,
   in-flight requests complete, the `SmartLifecycle` beans get their
   window, and `spring.lifecycle.timeout-per-shutdown-phase` (default
   **30s**, unchanged across every generation we opened) bounds it, with a
   bean able to cut the window short early. Wrong: the release. The flip
   happened in **Boot 3.4**, and this course's claim is an enumeration of
   jars, not a memory: 2.7.18, 3.2.12 and 3.3.12 answer `immediate`;
   3.4.13, 3.5.16 and 4.1.1 answer `graceful`. A "finally in Boot 4"
   narrative would work because the codebase came from somewhere below
   3.4 - and would be credited to the wrong release. What Boot 4 *itself*
   did in the area is the packaging move M10 taught and this row spells:
   `ServerProperties` left `spring-boot-autoconfigure` for the new
   `spring-boot-web-server` module (package
   `org.springframework.boot.web.server.autoconfigure`), so a review that
   names the class - in an exclude, a class-named property - answers to
   the new spelling while the behaviour never moved. The clause the
   change should still write is not a flag but a read:
   `spring.lifecycle.timeout-per-shutdown-phase` set to what the
   application's in-flight work can promise, rather than a 30-second
   default nobody remembers accepting.

## Checklist

What a reviewer applies to a solution of this Lab to judge it
production-shaped rather than merely working. Every item is observable -
checkable by reading the change or running a command - and none of them
repeats a gate the build already enforces.

- **The switch is one line, and what it switched is observed.** The change
  carries `spring.threads.virtual.enabled: true` in `application.yml` and
  nothing else turns the threading on: no `Executors.newVirtual...` in
  Java, no thread factory, no pool re-built by hand. The observation that
  proves what it reached is a runtime one, taken on the M09 socket the
  course already authenticates: `/actuator/threaddump` with the flag on
  lists no idle `tomcat-handler-` workers, because the guard pool Boot 2
  taught is gone and a virtual handler exists only for the length of its
  request - while the connector caps (`max-connections`, `accept-count`)
  remain the bounds the connector always held. Verified by reading the
  property and by the runtime claim the dump carries.
- **The availability call is bounded, in the 4.1 spellings, and the bound
  is proven, not configured.** The timeouts are values on
  `spring.http.clients.connect-timeout` / `read-timeout` - the plural
  spellings; the singular is deprecated-since-4.0 - reachable through the
  injected `RestClient.Builder`'s configurer (the `HttpClientSettings`
  bean), so the review searches for a `RestClient` built *around* that
  builder: a hand-`new`ed client or a configurer-bypassing builder is the
  call base that waits forever while the property applies to everything
  around it. The boundedness is observable, not plausible: the diagnostics
  go at an upstream that does not answer and the failure surfaces the way
  M04's failure model says wire failures surface - a problem detail, not
  a `500` stack dump, and never a hang.
- **The retry is on the method whose bean the caller holds, and its
  geometry is stated, not left.** `@EnableResilientMethods` appears once on
  a configuration; `@Retryable` sits on the bean method *the borrow calls*
  - no self-invocation, the proxy trap (`this.availability(...)` never
  crosses the interceptor) - and `includes` names the wire failure
  (`ResourceAccessException`, M04's own contract), with `maxRetries`,
  `delay`, `jitter`, `multiplier` and `maxDelay` set to values a sentence
  in the change can justify ("the service blips: three attempts, 100ms,
  200ms"), not the annotation's defaults. The arithmetic is checkable:
  1 + `maxRetries` attempts, growth bounded by `maxDelay`, slack per
  attempt by `jitter` - the delta table's retry row read against the
  change. The budget exhaustion carries its record (`getRetryCount()` /
  `getLastException()`) to the boundary, where M08's problem-detail
  handler takes over. Verified by reading the annotation, its caller and
  the diagnostics.
- **The retry's repetitions are observable, not folk memory.** A
  `MethodRetryEvent` listener (the event extends `MethodFailureEvent`,
  which extends `ApplicationEvent`) need not exist for the retry to work,
  but the production-shaped change says *how many*: a counter the operator
  can read beside M09's `library.loans.issued` - like the
  `library.availability.retried` shape, labelled at least by method name,
  so one listener serves every resilient method in the application. An
  unobserved retry is an outage nobody owns: the count is what turns "the
  service was down for a moment" into a closed observation. Verified by
  reading the listener and by running the diagnostics, which go at exactly
  the failure the event fires on.
- **Shutdown drains in flight, and the window is a decision.** The drain
  diagnostic (a [Loan](../GLOSSARY.md) begun across the stop completes; a
  request refused at the accept-refusal point is refused) rides a default
  that is a fact of the jar and not of the change - 4.1.1 answers
  `graceful` - so the *change's* shape is around the window the default
  implies: `spring.lifecycle.timeout-per-shutdown-phase` named to what the
  application's in-flight work can actually promise, so the review reads a
  number someone chose rather than thirty seconds nobody discussed.
  Verified by reading the property and by running the drain diagnostic,
  which holds the in-flight borrow case in both directions.
- **The vocabulary is the glossary's in the names, not just in the prose.**
  The person who borrows is a [Member](../GLOSSARY.md), the thing on the
  shelf is a [Copy](../GLOSSARY.md), the row is a
  [Loan](../GLOSSARY.md), the ceiling is the
  [Loan limit](../GLOSSARY.md) - not "user", "book", "task" or "worker" -
  so the diagnostics' messages, the metric's labels and the retry's
  configuration comments all speak the same language as the domain.
  Verified by reading the names.
- **No resilience is built twice.** The framework now ships the retry and
  the cap, and the change resolves *only* it: no `spring-retry` artifact
  (whose `@Retryable` shares the name, not the meaning), no Resilience4j
  starter alongside the annotations, no hand-rolled `Thread.sleep` retry
  loop and no `Semaphore`-based cap - each a second copy of a policy the
  delta table's Boot 4 column provides in the jars the build already
  resolves, and the conflict between two `@Retryable`s is a question the
  codebase should never have to answer. Verified by reading the POM and
  by grepping the diff.
