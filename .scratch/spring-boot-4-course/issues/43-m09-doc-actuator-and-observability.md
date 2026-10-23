# 43: M09 doc: Actuator and observability

**What to build:** The Module doc for M09: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers the actuator starter, liveness against readiness, Micrometer metrics, structured logging, OpenTelemetry, and what must never be exposed, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 42

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering actuator endpoints, probes and metrics
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

`modules/09-actuator-and-observability.md` written in M08's house shape:
intro, What you knew (with the assumed/not-assumed statement in M08's
phrasing), What changed (the nine-row delta table over 2.7.18 / 3.5.16 /
4.1.1), How it works now (starter contents, liveness vs readiness, metrics,
custom health, structured logging, OpenTelemetry, what must never be
exposed), The Lab, Quiz (5 Q&A), Checklist.

- **Delta table (criterion 1):** nine rows - the starter's dependency list,
  where endpoint code lives, the `httptrace`→`httpexchanges` endpoint, the
  probes, who may read what by default, the custom-health-indicator types,
  the metrics consent, structured logging, tracing and OpenTelemetry.
- **Every claim checked, not remembered (criterion 2):** each citation
  names the evidence. Read off published artifacts in the local repository:
  the 2.7.18 and 3.5.16 starters' POMs, the `spring-boot-dependencies`
  POMs of all three versions, `@Endpoint` counts by listing class files
  (23 in the 2.7.18 jar, 15 left in the 4.1.1 jar), the package moves
  (`org.springframework.boot.health.contributor` et al.), the
  `@ConditionalOnBooleanProperty(..., matchIfMissing = true)` on the 4.1.1
  `AvailabilityProbesAutoConfiguration` versus 2.7's `ProbesCondition` with
  its `CloudPlatform` "running on Kubernetes" half, and the exposure and
  show-details defaults in each jar's configuration metadata. Observed as
  behaviour on a scratch 4.1.1 application (classpath: exactly
  `spring-boot-starter-webmvc` + `spring-boot-starter-actuator`):
  `/actuator` lists only `self` and `health`; liveness and readiness answer
  `200` UP unconfigured; `management.endpoint.health.probes.enabled` was
  not written; the loan counter read back at
  `/actuator/metrics/library.loans.issued` with its `title` tag and `COUNT`;
  the custom `loan` indicator visible under `show-details=always` in tests;
  `http.server.requests` with the `uri` tag carrying the template
  `/titles/42`-shaped claim; structured logging set to the `logstash`
  format printing one JSON line from boot. The scratch project lives
  outside the repository and is not committed. The doc states what was
  checked per jar versus per behaviour in its table preamble.
- **Glossary terms (criterion 3):** thirteen links into the root
  `GLOSSARY.md` across `Available copy`, `Copy`,
  `Loan`, `Member`, `Title`; the metric name, the indicator's component
  key and the readiness vocabulary are glossary words.
- **Lab named with its command (criterion 4):** the Lab section names
  `lab-09-observability-start` / `-solution` per the lab index and gives
  `./mvnw -f labs -pl lab-09-observability-start verify -Pdiagnostics`,
  plus the `-Pdiagnostics-solutions` form.
- **Snippets compile (criterion 5):** `LoanCounter`
  (`registry.counter("library.loans.issued", "title", titleId)`),
  `LoanHealthIndicator` against the Boot 4 imports
  (`org.springframework.boot.health.contributor.*`), and the
  `AvailabilityChangeEvent.publish(...)` snippet were all compiled and
  exercised in the scratch 4.1.1 project; endpoint and metric JSON bodies
  quoted in the doc are its recorded output.
- **Assumed / not assumed (criterion 6):** the reader is assumed fluent in
  Boot 2's actuator story (starter shape, `MeterRegistry`, exposure line,
  2.3-era probes behind a property, structured logging as a third-party
  encoder, tracing absent) and **not** assumed to know the Boot 4 module
  split, the probes-on-by-default bytecode, the 3.0 `httpexchanges` rename,
  the 3.3 structured-logging keys, or Boot 4's OTel starter and namespace.
- No code changed; the doc edit (and this ticket) is the resolution.
  Snippets deliberately use `LoanRepository.count()` for the indicator
  detail so the health check is a count over data the M08 lab already owns,
  not a new dependency-direction claim.

## Comments

- **2026-10-23 · agent** — Drafted against the ticket's six criteria; every
  delta-table cell cited its verification (jar listings, bytecode reads,
  configuration metadata, and one booted 4.1.1 application with only the
  actuator and webmvc starters). Snippets compiled and run before being
  written into the doc. Lab names taken from the labs README index.
