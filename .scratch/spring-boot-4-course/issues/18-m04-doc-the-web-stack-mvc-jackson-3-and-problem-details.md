# 18: M04 doc: The web stack: MVC, Jackson 3 and problem details

**What to build:** The Module doc for M04: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers the webmvc starter, Spring MVC 7, what actually changed in Jackson 3 beyond the package root, RFC 9457 problem details, the declarative HTTP Service client, and API versioning, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 17

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering the webmvc starter, Jackson 3 payloads and problem details
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

Built in commit `HEAD~0` (the commit that added this doc).

- **The Module doc** `modules/04-web-stack-jackson3.md` covers the web
  starter, what Jackson 3 changed past the package root, RFC 9457 problem
  details, the declarative HTTP Service client, and API versioning.
- **Every claim in the delta table was checked against the versions in use**:
  - `jackson-databind` 3.1.5's own source says
    `FAIL_ON_UNKNOWN_PROPERTIES` is "disabled by default as of Jackson 3.0
    (in 2.x it was enabled)" and that `FAIL_ON_NULL_FOR_PRIMITIVES` is the
    other way round - so a JSON `null` for an `int` field is now an error;
  - Boot 4's `JacksonAutoConfiguration` disables
    `FAIL_ON_UNKNOWN_PROPERTIES` explicitly, so a Boot application still
    ignores unknown request properties. The Jackson 3 default change is
    masked by Boot, and the doc says so rather than claiming the opposite;
  - Boot also disables `WRITE_DATES_AS_TIMESTAMPS` and
    `WRITE_DURATIONS_AS_TIMESTAMPS`, so dates and durations are still
    written as text;
  - Framework 7 added `JacksonJsonHttpMessageConverter` and kept the
    Jackson 2 converter; Framework 6.2 has only the Jackson 2 one;
  - `spring-boot-restclient`, the HTTP Service client auto-configuration and
    the starter are all new in Boot 4 - the Boot 3.5 source tree has no such
    module;
  - API versioning is a Framework 7 addition, and Boot 4 adds
    `PropertiesApiVersionInserter`, which puts a version into an outbound
    request.
- **Every snippet resolves against the real classpath**: `@HttpExchange`,
  `@GetExchange`, `HttpClientErrorException.NotFound`, `ApiVersionResolver`,
  `ProblemDetail`, `JsonMapper` and `JacksonJsonHttpMessageConverter`.
- **The Lab** is described with its two Diagnostics: a copy the availability
  service says is on loan is refused with `409` and a problem detail, and a
  copy the service does not know about is reported as a `404` problem detail
  rather than a `500`.
