# M06 - Validation and error handling

M05 put the library's catalogue in a database. This Module is about what the
library says when something goes wrong: the request that is malformed, the
request that is fine but asks for something the library will not do, and the
layer each of those answers belongs to. By the end of it the [Loan
limit](../GLOSSARY.md) will be a rule the loan feature enforces and reports as
a problem detail - and not a constraint bolted onto a request record, which is
the mistake the Lab is built around.

## What you knew

In Boot 2, bean validation at the boundary was `@Valid` on a `@RequestBody`
parameter, with constraint annotations on the payload type -
`@NotNull`, `@NotBlank`, `@Size` - and an optional check the service did
afterwards. There was a starter to remember: `spring-boot-starter-validation`
came free with the web starter until 2.3 and then moved out, so a new Boot 2
application that forgot it shipped constraints that did nothing at all. The
error a failed check produced was Boot's default JSON error body - a
timestamp, a status, an `error` string - and since 2.3 even the `message` in
it was blanked by default, so the client saw *that* something was wrong and
nothing about what. A constraint written directly on a controller method
parameter - `@RequestParam @NotBlank String id` - was a different thing: it
needed `@Validated` on the class to be noticed, and when it failed, it surfaced
as a `ConstraintViolationException` the framework had no plan for - a `500`
with a stack trace. The domain rules had no home at all: a hand-held `if` in
the controller after the `@Valid` gate, or a constraint on an entity that
Hibernate would enforce at flush time, with the flush happening far from the
request that caused it.

You are assumed to know all of that fluently. You are **not** assumed to know
Spring's `ErrorResponse` contract, that Framework 6.1 grew validation of
controller method parameters, that Boot 3 and 4 have an opt-in switch that
renders Spring's standard exceptions as problem details, or which of Boot 4's
modules owns the validation auto-configuration now that the
starters are modularised.

## What changed

The Boot 4 column is the one this course's build resolves - Spring Boot 4.1.1
over Spring Framework 7.0.9, every claim below checked against the resolved
artifacts, several of them by running applications of each generation and
reading off what a failed request answers (see the verified-versions table in
the [lab index](../labs/README.md)). The Boot 2 column is the last Boot 2
(2.7), the Boot 3 column the last Boot 3 (3.5).

|  | Spring Boot 2 (2.7) | Spring Boot 3 (3.5) | Spring Boot 4 (4.1.1) |
|---|---|---|---|
| Where the validation starter comes from | dropped out of `spring-boot-starter-web` in 2.3 - your own `spring-boot-starter-validation` from then on (we checked the published POMs of 2.2.13 and 2.3.12) | the same starter | the same name, one level deeper: the auto-configuration now lives in Boot's own `spring-boot-validation` module under it |
| The validation implementation | Hibernate Validator **6.2.5.Final**, `javax.validation.constraints.*` | Hibernate Validator **8.0.3.Final**, `jakarta.validation.constraints.*` | Hibernate Validator **9.1.3.Final** over Jakarta Validation **3.1.1**, still `jakarta.validation.constraints.*` |
| A constraint on a method parameter (not a body) | needs `@Validated` on the controller class, or the constraint is never read; a failed one is a `ConstraintViolationException` and a **500** | validated by the framework without `@Validated` since Framework 6.1 - `HandlerMethodValidationException`, a **400** | the same 400 |
| Spring's standard exceptions as problem details | nothing: no `ProblemDetail` type, no switch | opt-in: `spring.mvc.problemdetails.enabled=true` (default **false**) registers a default advice that renders them as `application/problem+json` | the same property, the same default; the auto-configuration now lives in the `spring-boot-webmvc` module |
| Spring's own exception contract | ad hoc: `MethodArgumentNotValidException` is a `BindException` with nothing to say about rendering | the standard exceptions implement `ErrorResponse` - a status and a body come *with* the exception | the same |

Two rows are worth a second look. **The method-parameter row** is the one that
silently changed behaviour: the same misplaced constraint that produced a
`500` in Boot 2 produces a `400` in Boot 3 and 4, because Framework 6.1 folded
parameter validation into the dispatch itself and gave it a typed exception
that declares its own status. And **the annotations row** repeats what M03
taught about configuration properties: `jakarta.validation.constraints.*` is
where the annotations live, they moved once (Boot 3), and they did not move
again. A `javax.validation` `@NotNull` kept after the upgrade is either a
compile error on this course's classpath or, when a legacy dependency still
ships the old API, decoration the validator never reads - never the constraint
it advertises.

Notice what the table has no row for: the domain rules - the
[Loan limit](../GLOSSARY.md), the fourteen-day
[Loan period](../GLOSSARY.md), no [Reservation](../GLOSSARY.md) for a
[Title](../GLOSSARY.md) the [Member](../GLOSSARY.md) already holds a
[Loan](../GLOSSARY.md) for - changed in none of the
three columns, because no version of Boot ever owned them. The table is the
boundary's: everything the framework validates, it validates about *input*.
What it says about a *rule* is only where to report one - `ErrorResponse`,
problem details - never what the rule is. That division is this Module's
subject, and the Lab is built around the mistake of making a rule look like a
row in this table.

## How it works now

### The starter is one dependency, and the module under it

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

The managing is the same as ever; the shape underneath changed. The starter is
`spring-boot-starter` plus Boot's own `spring-boot-validation`, and that
module brings Hibernate Validator 9.1.3.Final with the expression language it
interpolates its messages with. The same modularisation M05 showed for Flyway:
the auto-configuration beans used to be buried in `spring-boot-autoconfigure`;
they moved into the module of the same responsibility, and the starter is what
presents them. Declaring Hibernate Validator on its own does not bind Boot's
auto-configuration to it, any more than a bare `flyway-core` ran a migration.

### Invalid input: the boundary's job

The boundary validates *shape*: is the body the type you declared, are its
members present, are they blank, do they fit the bounds. It happens before
your code runs, and it is worth being explicit about what the framework does
with a failure when you add nothing: we measured a 4.1.1 application answering
a blank [Copy](../GLOSSARY.md) field with `400` and the default error body -

```json
{"timestamp":"2026-10-23T04:22:41.715Z","status":400,"error":"Bad Request","path":"/body"}
```

- not a problem detail,
- and with no field errors, which is the part a client actually needs.

The application's own handler (M04's `ApiExceptionHandler`) turns that into a
problem detail that lists the field errors, and that is the pattern - the
framework's default answer to a shape failure is the right status with the
wrong body.

Where the shape lives is the request record, and it already has it:

```java
public record LoanRequest(
        @NotBlank String memberId, @NotBlank String copyId) {}
```

Constraints also go on method parameters - a `@PathVariable` or
`@RequestParam` - and *this* is the part that changed under you since Boot 2.
Framework 6.1 validates controller method parameters in the dispatch itself:
no `@Validated` on the class, no proxy, so the check cannot be bypassed by
self-invocation, because there is no proxy to bypass. A failure is a
`HandlerMethodValidationException`, and it reports a `400` by default - the
same misplaced constraint that was a `500` in Boot 2. Your handler catches the
typed exception and says which parameter failed:

```java
@ExceptionHandler(HandlerMethodValidationException.class)
public ResponseEntity<ProblemDetail> handleInvalidParameter(HandlerMethodValidationException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Invalid request parameter");
    problem.setProperty("errors", exception.getParameterValidationResults().stream()
            .map(result -> result.getMethodParameter().getParameterName() + ": "
                    + result.getResolvableErrors().get(0).getDefaultMessage())
            .toList());
    return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
}
```

### A broken rule: the domain's job

The [Loan limit](../GLOSSARY.md) is not a shape. "A member may hold three
[Active loan](../GLOSSARY.md)s at once" says nothing about the *request* - the
request for a fourth is perfectly well-formed, it names a real
[Member](../GLOSSARY.md) and a real [Copy](../GLOSSARY.md) - and the library
refuses it anyway, because of the state the domain is holding: the other
loans. That distinction is the whole Module:

- an **invalid input** is a fact about the request; it fails validation and
  answers `400`;
- a **broken rule** is a fact about the domain; the request passes validation,
  and a feature still refuses the action it describes, answering `409`.

Two conclusions follow, and the second one is where the Lab's defect lies.

First, **the rule lives in the Loan feature's service**, not in the
controller. The controller's job was to pass the request through the gate;
the service is the thing that knows a [Member](../GLOSSARY.md)'s
[Active loan](../GLOSSARY.md)s and the limit configured in M03:

```java
@Service
public class LoanService {

    private final LoanRepository loans;
    private final LoanProperties properties;

    LoanService(LoanRepository loans, LoanProperties properties) {
        this.loans = loans;
        this.properties = properties;
    }

    public Loan borrow(String memberId, String copyId) {
        if (loans.countByMemberId(memberId) >= properties.limit()) {
            throw new LoanLimitExceededException(properties.limit());
        }
        // The Member is under the Loan limit; the borrowing itself is the rest of this method.
        return loans.save(memberId, copyId);
    }
}
```

The count reads whichever store the Loan feature has - in memory so far, in
the database from M08; the rule is the same shape either way.

Second, **the rule does not go on the request record as a constraint**, even
though bean validation can express limits in general. It cannot express this
one, for two reasons. A constraint is a constant, and the limit is a value the
library configured - `library.loan.limit`, bound in M03 - and a constant in an
annotation cannot read the configuration. And a constraint on `LoanRequest`
describes *one request*, when the thing being limited is a count the request
does not contain - the [Member](../GLOSSARY.md)'s other
[Active loan](../GLOSSARY.md)s. `@Max(3)` on a request record would be a rule
dress-wearing as a shape check: it ignores the configured limit entirely, and
it checks nothing a single request can know.

The domain exception is a domain type, named in the glossary's terms, carrying
the rule's wording:

```java
public class LoanLimitExceededException extends RuntimeException {

    public LoanLimitExceededException(int limit) {
        super("A member may hold " + limit + " Active loans at once");
    }
}
```

and the handler translates it - the same advice class M04 built, one method
per domain refusal:

```java
@ExceptionHandler(LoanLimitExceededException.class)
public ResponseEntity<ProblemDetail> handleLoanLimitExceeded(LoanLimitExceededException exception) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    problem.setTitle("Loan limit reached");
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
}
```

`409 Conflict` is the category M04 already chose for a Copy that is not on the
shelf, and the reason is the same: the request is valid, and it conflicts with
the state the domain holds - the Member's other loans, the Copy's loan. A
`400` would tell the client to fix the request, and there is nothing to fix:
the request to make later, after a copy is back, is this same request.

### Which layer owns which error

Put together, the ownership is three layers and one switch:

- **The controller owns shape.** A request record with constraints, `@Valid`
  on the body, constraints on parameters where they are needed - and a
  handler for the two validation exceptions, `400`, field and parameter
  errors listed in the problem detail.
- **The feature's service owns rules.** One domain exception per refusal,
  thrown where the rule is checkable: the Loan feature's service for the
  [Loan limit](../GLOSSARY.md), the availability client's caller for an
  unavailable [Copy](../GLOSSARY.md). It never formats JSON, never imports
  `ProblemDetail`.
- **The `api` package's handler owns translation.** One `@ExceptionHandler`
  per exception, choosing the status and the title, never deciding the
  rule.
- **The framework owns the rest, on a switch.** `spring.mvc.problemdetails.enabled=true`
  renders every standard exception as a problem detail - we measured it:
  `application/problem+json`, `title` `"Bad Request"`, `detail`
  `"Invalid request content."`. That is the right shape and the wrong
  words; the titles your clients read are the ones your own handler writes -
  "Loan limit reached", "Copy is not on the shelf" - so the switch is for
  the failures you did not anticipate, and the handler for the ones you did.

The cost of the discipline, then: one exception and one handler method per
rule. That is not ceremony; it is where the rule's name becomes part of the
API's vocabulary instead of a number in a field error "must be less than or
equal to 3" - which describes a beige limit the glossary has no word for.

## The Lab

The Lab is the library application you last touched in M05 - catalogue in the
database, availability service consulted before a [Copy](../GLOSSARY.md) is
lent. M04's handler already answers an invalid body with a `400` problem
detail, but look closely at what a borrow request's failure says: the handler
is shared, and its title is "Invalid reservation request" - the loan borrowing
the reservation's words. And the [Loan limit](../GLOSSARY.md) you bound in M03
sits in `LoanProperties.limit()` with nothing reading it: any
[Member](../GLOSSARY.md) can borrow forever.

Your job is the Start module: make the library answer its own failures, in its
own words. The requirement is:

1. a borrow request that does not validate is refused with a `400` problem
   detail that names the *loan's* failure - not the reservation's, and not the
   default error body;
2. a [Member](../GLOSSARY.md) at the [Loan limit](../GLOSSARY.md) asking for a
   further [Loan](../GLOSSARY.md) is refused with a `409` problem detail -
   the domain's exception, raised by the loan feature, translated by the
   handler, and never a constraint on the request record.

The diagnostic tests state this precisely - one per requirement - and fail
with messages naming what is missing:

```bash
./mvnw -f labs -pl lab-06-validation-and-errors-start verify -Pdiagnostics
```

The Solution module shows what the answer looks like: the boundary validations
on the borrow request's record and parameters, a `LoanService` that counts a
[Member](../GLOSSARY.md)'s [Active loan](../GLOSSARY.md)s against the bound
limit and throws the domain exception, and the handler methods that give both
failures problem details with titles the glossary would sign.

## Quiz

Five questions about the delta table above and the ownership it stops at.
Answer first; the answers say why, not just what.

1. **About the boundary the table stops at - the rules get no row: a
   colleague proposes finishing the Lab early, by giving the
   [Loan limit](../GLOSSARY.md) the same treatment every other check on the
   borrow request gets - a constraint on `LoanRequest`, next to the
   `@NotBlank`s. Why can bean validation not enforce that rule, for two
   reasons?**

   Because a constraint can express neither half of it. *First*, a
   constraint is a constant: `@Max(3)` bakes a number into an annotation
   value, and annotation values must be compile-time constants - the limit
   is `library.loan.limit`, configuration that M03 bound and the loan
   feature reads off `LoanProperties`, so the annotation reads nothing and
   hard-codes a three the rule does not own; change the configuration, and
   the constraint still says three. *Second*, the constraints on a record
   describe that one request - `@NotBlank` says something about the body -
   and the [Loan limit](../GLOSSARY.md) compares a count the request does
   not contain: the [Member](../GLOSSARY.md)'s other
   [Active loan](../GLOSSARY.md)s. There is no field on the record the
   count could sit on, so "validated at the boundary" is unavailable in
   principle, not just for this record's shape. Which is why the count is
   taken in `LoanService` against `properties.limit()` and the refusal is
   the domain exception the handler translates - input constraints for
   shape, the feature's service for the rule.

2. **About the method-parameter row: the Boot 2 codebase you just upgraded has
   `@Validated` on a controller and `@RequestParam @NotBlank String memberId`
   on a method. Two things changed about that constraint in Boot 3. What are
   they?**

   The `@Validated` is no longer doing the work, and the failure stopped
   costing `500`. Framework 6.1 folds parameter validation into the dispatch
   itself, so the constraint is read without any annotation on the class - the
   `@Validated` stays harmlessly, but the proxy you kept alive for it (the one
   self-invocation could bypass) is not the mechanism any more. And a failed
   parameter is a `HandlerMethodValidationException` whose own status is
   `400` - Framework 6's `ErrorResponse` contract declares status and body on
   the exception - where Boot 2 surfaced a `ConstraintViolationException`
   nobody owned. The status you get is now the one the failure deserves.

3. **About the problem-details row: a colleague has heard "Boot does problem
   details for you", deletes your `ApiExceptionHandler` methods, and sets
   `spring.mvc.problemdetails.enabled=true`. What does a client read now - for
   a blank request field, and for a [Member](../GLOSSARY.md) at the
   [Loan limit](../GLOSSARY.md)?**

   For the blank field, a problem detail with the wrong words: we measured
   `application/problem+json` with `title` `"Bad Request"` and `detail`
   `"Invalid request content."` - the right shape, no field errors, and a
   title that says nothing about the library. For the Member at the loan
   limit, nothing good at all: `LoanLimitExceededException` is the
   application's own exception, not one of Spring's; it implements no
   `ErrorResponse`, so the switch that renders *standard* exceptions does not
   know it, and it propagates to the default error handling - a `500`. The
   switch registers an advice for the framework's exceptions; your domain
   failures still need your handler, and your titles still need your words.

4. **About the implementation row: the same record now carries
   `javax.validation.constraints.NotNull` where it had a `jakarta` one. What
   does the application do with it?**

   Nothing - and how little depends on what the classpath still holds. The
   constraint annotations moved to `jakarta.validation.constraints.*` in Boot
   3 and did not move again, and 4.1.1's `jakarta.validation-api` 3.1.1
   contains no `javax.*` at all (we listed the jar), so on this course's
   classpath the old import does not resolve: the migration stops at compile
   time, loudly. The dangerous case is the dependency that still ships the old
   API transitively - then the import resolves and the constraint is inert:
   the validator scans for the `jakarta` annotation type, a `javax` annotation
   is a *different type* it does not know, and nothing warns. Either way, the
   invalid request the constraint would have rejected is not being rejected
   while the code looks validated.

5. **About which layer owns which error: a reviewer objects that
   `LoanService` already holds everything the refusal's answer needs - the
   count, the limit, the rule's wording - and proposes skipping the domain
   exception and the handler method entirely: the service builds the
   `ProblemDetail` itself and the controller returns whatever it is handed.
   What does that move cost the feature, and what does it undo?**

   It turns the [Loan](../GLOSSARY.md) feature into an HTTP responder. A
   `ProblemDetail` in `LoanService` imports the translation layer's
   vocabulary into the rule's package - the Solution keeps that import in
   the `api` package, exactly one layer - and it answers "who chooses the
   status" with "the throw site": `409` for the
   [Loan limit](../GLOSSARY.md), `404` for an unknown
   [Title](../GLOSSARY.md), decided per feature and per method, with HTTP
   enums under the rule's nose. The domain exception is the feature's half
   of the contract, and not only for HTTP callers: another part of the
   application calling `borrow()` cannot read a status code, but a
   `LoanLimitExceededException` says what the library refuses and by how
   much. The tests change shape with the body: a rule asserted through a
   response body can no longer be tested without the boundary rendering it.
   The service throws, in the domain's words; the handler translates, one
   method per refusal, choosing the status and the title. If the service
   builds bodies too, the two failures the Module separated - a `400` about
   the request, a `409` about the domain's state - stop being
   distinguishable at the boundary, which is the merge the Module argues
   against.

## Checklist

What a reviewer applies to a solution of this Lab to judge it production-shaped
rather than merely working. Every item is observable - checkable by reading the
change or running a command - and none of them repeats a gate the build already
enforces.

- **A bad shape answers `400` as a problem detail.** A borrow request with a
  blank or missing field is refused by the validation the framework runs, and
  the answer carries `application/problem+json` with the field errors listed
  and a `title` that names the *loan's* failure - not the default error body
  with its timestamp and `error` string, not the reservation's title, and no
  stack trace.
- **The rule is the service's.** The [Loan limit](../GLOSSARY.md) is enforced
  in the loan feature's service: the count of a [Member](../GLOSSARY.md)'s
  [Active loan](../GLOSSARY.md)s against the limit M03 bound
  (`properties.limit()`), a domain exception thrown where the check fails.
  No constraint on the request record expresses the limit, and the controller
  does not count.
- **A broken rule answers `409` as a problem detail.** A
  [Member](../GLOSSARY.md) at the limit asking for a further
  [Loan](../GLOSSARY.md) gets `application/problem+json` with a status of
  `409` and a title that names the refusal - nothing else in the application
  answers for it: no `500`, no default error body, no empty body.
- **The parameter constraints are honoured.** Any constraint written on a
  parameter of the Loan API fails the request with a `400` and the parameter
  named in the body - the Framework 6.1 way, with or without `@Validated` on
  the class - and the handler covers `HandlerMethodValidationException` like
  it covers the body exceptions.
- **Nothing unmapped is answered in invented words.** The handler enumerates
  the failures the loan feature can raise and covers them all; there is no
  `@ExceptionHandler(Exception.class)` choosing those answers instead. A
  failure it does not know - a malformed body, a standard exception the module
  did not anticipate - answers the way the framework answers it: at the status
  its type carries and, with the switch on, as a problem detail. The switch is
  for the failures nobody anticipated; a catch-all handler is a third layer
  the ownership table has no row for.
- **The translation lives in exactly one place.** A domain failure is mapped
  by exactly one `@ExceptionHandler`: one method per exception, all of them
  methods of the one handler class in the `api` package, and `ProblemDetail`
  imported nowhere else - the service throws domain exceptions, the
  controllers return payloads or raise them. A `ProblemDetail` built inside a
  service method, or a second mapping for an exception one method already
  answers, is the boundary leaking inward - and the next refusal to translate
  has nowhere to come from but the handler.
- **The vocabulary is the glossary's, in the failures too.** The exception
  names, the problem `title`s and the error messages say
  [Member](../GLOSSARY.md), [Loan](../GLOSSARY.md),
  [Loan limit](../GLOSSARY.md), [Active loan](../GLOSSARY.md),
  [Copy](../GLOSSARY.md) - never "checkout", "rental", "fine" or "book".
- **The two failures stay two - and the answer is run, not assumed.** The
  diagnostics report the shape failure and the rule failure separately, each
  with its own message, and both go green on your solution:
  `./mvnw -f labs -pl lab-06-validation-and-errors-solution verify -Pdiagnostics`.
