# 28: M06 doc: Validation and error handling

**What to build:** The Module doc for M06: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers bean validation at the boundary, translating domain failures into problem details, the difference between invalid input and a broken rule, and which layer owns which error, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 27

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering bean validation at the boundary versus domain rules
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

Built in this ticket's commit. The doc is [`modules/06-validation-and-errors.md`](../../modules/06-validation-and-errors.md).

**The delta table** has five rows - the starter's home, the validation
implementation, a constraint on a method parameter, Spring's standard
exceptions as problem details, and the `ErrorResponse` contract - and a
statement of what it deliberately has no row for: the domain rules, which no
version of Boot ever owned. The boundary-versus-domain split is the Module's
argument, shared with the Lab.

**Every claim was checked, not remembered**, three ways:

- *Published POMs*, fetched from Maven Central for the boundary versions:
  `spring-boot-starter-web` 2.2.13 still carries `spring-boot-starter-validation`;
  2.3.12 does not - that is the "dropped out in 2.3" claim. The starter POMs of
  2.7.18, 3.5.16 and 4.1.1 gave the dependency trees; 4.1.1's starter is
  `spring-boot-starter` plus Boot's new `spring-boot-validation` module, which
  brings Hibernate Validator 9.1.3.Final and `tomcat-embed-el` 11.0.24.
- *Dependency management*, read out of `spring-boot-dependencies` in the local
  repository: Hibernate Validator 6.2.5.Final / 8.0.3.Final / 9.1.3.Final and
  Jakarta Validation 2.0.2 / 3.0.2 / 3.1.1 across 2.7.18 / 3.5.16 / 4.1.1.
- *Behaviour, measured by running applications*: two throwaway apps outside the
  repository (Boot 2.7.18 and Boot 4.1.1, each probed over real HTTP). The Boot
  2 app answered a blank request-body field with `400` and the whitelabel body
  with no `message`, and a blank `@RequestParam` constraint with **`500`
  Internal Server Error**. The Boot 4 app answered the same parameter failure
  with **`400`** (no `@Validated` needed), the same body failure with `400` and
  the whitelabel body until `spring.mvc.problemdetails.enabled=true` turned it
  into `application/problem+json` with `title` `"Bad Request"` - and,
  importantly, left an uncaught application exception a plain `500` even with
  the switch on. The property's metadata was read from both `spring-boot-autoconfigure`
  3.5.16 and `spring-boot-webmvc` 4.1.1 (default `false`), and the auto-registered
  advice it wires up (`ProblemDetailsExceptionHandler extends ResponseEntityExceptionHandler`)
  was confirmed by bytecode in both versions; `ErrorResponse`'s presence in
  `spring-web` 6.2.19 and 7.0.9 and its absence from 5.3.31 were checked in the
  jars, along with `MethodArgumentNotValidException implements ErrorResponse`
  on 7.0.9 - breaking one remembered belief along the way: on 4.1.1's
  classpath a `javax.validation` import does not resolve at all (the
  `jakarta.validation-api` 3.1.1 jar contains no `javax.*`), so the old
  annotation is a compile error here and only inert when a legacy dependency
  still ships it.

**Every snippet compiles**: the doc's snippets - the `LoanRequest` record
verbatim of the application, the `LoanService` with the limit check, the
`LoanLimitExceededException`, and the two `@ExceptionHandler` methods - were
compiled in a throwaway module pinned to `spring-boot-starter-parent` 4.1.1,
against the application's own domain types copied from
`lab-05-persistence-solution`. The `HandlerMethodValidationException` API the
handler snippet walks (`getParameterValidationResults`,
`ParameterValidationResult.getResolvableErrors`) was read off the resolved
`spring-context` 7.0.9 jar.

**The claims the Lab depends on were read off the application as M05 left
it**: the shared `ApiExceptionHandler` already answers an invalid body with a
`400`, but its title is "Invalid reservation request" - a borrow request's
failure says the reservation's words - and `LoanProperties.limit()` is bound
and read by nobody. The Lab section states both defects, and the two
diagnostic requirements: a `400` problem detail that names the loan's
failure, and a `409` problem detail for a [Member](../../../GLOSSARY.md) at the
[Loan limit](../../../GLOSSARY.md), thrown by the loan feature and never a
constraint on the request record. The command named is
`./mvnw -f labs -pl lab-06-validation-and-errors-start verify -Pdiagnostics`,
per the lab index.

**Assumed and not assumed** are stated at the end of "What you knew": the
Boot 2 practices are fluent; `ErrorResponse`, `HandlerMethodValidationException`,
the problem-details switch and the `spring-boot-validation` module are not.
