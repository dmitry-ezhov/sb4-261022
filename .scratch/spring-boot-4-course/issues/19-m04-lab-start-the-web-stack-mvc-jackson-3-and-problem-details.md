# 19: M04 lab start: The web stack: MVC, Jackson 3 and problem details

**What to build:** The Start module for M04: the application as it stands after the previous Module, extended so that it does not yet do what this Lab asks for. The requirement is stated as Diagnostic tests that fail with messages naming what is missing. The Lab covers serving the catalogue as typed records through Jackson 3, returning problem details from a dedicated handler, and calling an outbound service through the declarative client.

**Blocked by:** 18

**Status:** resolved

- [ ] the Start module exists and is listed in the labs aggregator
- [ ] it compiles, is formatted, passes every Quality gate except the coverage floor, and passes its own tests
- [ ] its diagnostics fail, and each failure message names what is missing
- [ ] the default build stays green; the diagnostics run is red
- [ ] the application is the one the previous Module left, extended rather than rewritten

## Answer

Built in commit `435490b`.

- **The Start module** is the application M03 left, with the availability
  service's type added and the service deliberately not consulted: every copy
  is assumed to be on the shelf. The Lab's defect is a missing outbound
  dependency, which is the first one in the course.
- **Two diagnostics**, with the service stubbed by WireMock on a dynamic port
  and its base URL handed to the application through
  `@DynamicPropertySource`:
  - the service says the copy is not on the shelf: the application must
    answer `409 Conflict` with a problem detail - it answers `201 Created`,
    and the failure says `Status expected:<409> but was:<201>`;
  - the service does not know the copy: the application must answer `404`
    with a problem detail - it answers `201 Created`, and the failure says
    `Status expected:<404> but was:<201>`.
- **The wiremock-standalone artifact** is the one the parent manages, for
  the reason the parent POM records: the plain jar mixes two Jetty majors and
  breaks unrelated auto-configuration on the test classpath.
- **Verified**: the default build is green; the diagnostics run is red with
  two named failures.
