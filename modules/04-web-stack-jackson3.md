# M04 - The web stack: MVC, Jackson 3 and problem details

M03 was about the rules the application obeys. This Module is about the
application's edges: the JSON it reads and writes, the failures it reports, and
the service it calls. By the end of it you will know what Jackson 3 actually
changed, how to report a failure the way the HTTP specification asks, and how
to call another service without writing the plumbing.

## What you knew

Boot 2's web stack was one starter. `spring-boot-starter-web` brought Spring
MVC, Tomcat, Jackson 2 and the message converters, and you wrote
`@RestController` classes with `@GetMapping` and `@PostMapping`, took bodies
with `@RequestBody`, and returned records or `ResponseEntity`. When something
went wrong, you threw an exception and let Boot's `BasicErrorController`
render it into a JSON body with a timestamp, a status and an `error` string.
You called other services with a `RestTemplate` you configured by hand, and
you wrote the interface, the URL, the body and the error handling yourself.

You are assumed to know all of that fluently. You are **not** assumed to know
what changed inside Jackson beyond the package root, how to report a failure
as an RFC 9457 problem detail, how the declarative HTTP client works, or that
the framework grew API versioning.

## What changed

|  | Spring Boot 2 | Spring Boot 3 | Spring Boot 4 |
|---|---|---|---|
| The web starter | `spring-boot-starter-web` | `spring-boot-starter-web` | `spring-boot-starter-webmvc` - the old name still resolves, but it is deprecated |
| The JSON library | Jackson 2, `com.fasterxml.jackson.databind.ObjectMapper` | Jackson 2 | Jackson 3, `tools.jackson.databind.json.JsonMapper` |
| The JSON annotations | `com.fasterxml.jackson.annotation.*` | the same | the same - **they did not move** |
| The MVC message converter | `MappingJackson2HttpMessageConverter` | the same | `JacksonJsonHttpMessageConverter` - the Jackson 2 converter is still there |
| How you build a mapper | `new ObjectMapper()` | the same | `JsonMapper.builder()` |
| A JSON `null` for a primitive field | silently `0` | silently `0` | **an error** - `FAIL_ON_NULL_FOR_PRIMITIVES` is on by default |
| Failures | a JSON body from `BasicErrorController` | `ProblemDetail` is available | `ProblemDetail`, served as `application/problem+json` |
| Calling another service | `RestTemplate`, by hand | `RestTemplate`, by hand | the declarative HTTP Service client: `@HttpExchange` interfaces, `spring-boot-starter-restclient` |
| API versioning | whatever you wrote | whatever you wrote | built in: `ApiVersionResolver` and friends |

Two rows are worth a second look, because they are the ones people get
wrong. **The annotations did not move**: `jackson-annotations` is still
`com.fasterxml.jackson.core:jackson-annotations`, still version 2.21 inside
a Jackson 3 application, still the package `com.fasterxml.jackson.annotation`.
And **the `null` behaviour changed**: in Jackson 2, a JSON `null` for an
`int` field quietly became `0`; in Jackson 3 it is an error, unless the
application turns the feature off.

## How it works now

### The web starter

`spring-boot-starter-webmvc` brings `spring-boot-starter`,
`spring-boot-starter-jackson`, `spring-boot-starter-tomcat`,
`spring-boot-http-converter` and `spring-boot-webmvc`. Five artifacts, one
responsibility each, and the JSON half of it is a starter you could take on
its own if you were not building a web application.

### Jackson 3, past the package root

The package root moved: `com.fasterxml.jackson.databind.ObjectMapper` is now
`tools.jackson.databind.json.JsonMapper`, and the Maven groupId moved with
it. The annotations stayed where they were. So a Jackson 3 class mixes two
roots, and the import that tells you which Jackson you are on is not the
annotation import.

The behaviour changes that survive Boot's configuration are the ones worth
knowing:

- **A JSON `null` for a primitive is an error.** `FAIL_ON_NULL_FOR_PRIMITIVES`
  is enabled by default in Jackson 3 and was disabled in Jackson 2. A request
  body with `{"count": null}` for an `int count` now fails to bind instead of
  quietly becoming zero. Boot does not turn this one off.
- **Unknown properties are still ignored.** Jackson 3 changed the default of
  `FAIL_ON_UNKNOWN_PROPERTIES` - it was enabled in Jackson 2 and is disabled
  in Jackson 3 - but Boot disables the feature explicitly, so in a Boot
  application a request body with an extra field is still accepted. If you
  relied on that in Boot 2, you have not lost it.
- **Dates and durations are written as text.** Boot disables
  `WRITE_DATES_AS_TIMESTAMPS` and `WRITE_DURATIONS_AS_TIMESTAMPS`, so a
  `LocalDate` is `"2026-01-05"` and a `Duration` is `"PT720H"`, not a number.
  That is the same as in Boot 2, and it is why the JSON in this course is
  readable.

### Problem details

RFC 9457 is the specification for saying "here is what went wrong" in an HTTP
response, and Spring has a type for it: `ProblemDetail`, in
`org.springframework.http`. It is a map with a status, a title, a detail and
whatever else you put in it, and it is meant to be served as
`application/problem+json`.

The shape of a failure, done the Boot 4 way:

```java
ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
problem.setTitle("Title not found");
problem.setDetail("No title with id " + id);
```

A dedicated handler turns your domain exceptions into them:

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TitleNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleTitleNotFound(TitleNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Title not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
```

Three things make it a problem detail rather than a JSON object that happens
to have a status:

- the media type is `application/problem+json` ✓;
- `status` is in the body, not only in the response line ✓;
- `title` says what failed, in a form a human can read ✓.

The alternative - `ResponseEntity.notFound().build()` - answers with the right
status and an empty body, which tells the caller nothing at all. That is the
defect this course's first Lab was about.

### The declarative HTTP client

Writing an outbound HTTP call by hand is three things you have to get right:
the URL, the body, and what to do when it fails. Spring's **HTTP Service
client** lets you write the first two and lets the framework do the third.
You declare an interface:

```java
@HttpExchange("/api/availability/{copyId}")
public interface AvailabilityClient {

    @GetExchange
    Availability availability(@PathVariable String copyId);
}
```

and the framework gives you an implementation of it. The URL lives in the
annotation, the body is a method parameter, and the return type is the
response body. The starter is `spring-boot-starter-restclient`, and the
proxy is created for you when the interface is in the context.

What the framework does with the *response* is the part worth knowing: a 4xx
or 5xx becomes a typed exception - `HttpClientErrorException.NotFound` for a
404 - rather than a string you have to check. That is what turns "the service
says no" into a problem detail in your own API, without a status check in
every caller.

### API versioning

Framework 7 grew API versioning: `org.springframework.web.accept` has
`ApiVersionResolver`, `ApiVersionHolder` and the rest. A version can come
from a header, from a path segment or from a media type, and the resolution
is a strategy you write rather than a filter chain you write. Boot 4 adds the
plumbing to put a version into an outbound request, so the version a client
sends is the version your service calls.

## The Lab

The Lab is the library application you finished in M03, with one new
ingredient: the library asks another service whether a [Copy](../GLOSSARY.md)
is on the shelf before it lets a [Member](../GLOSSARY.md) borrow it. That is
the availability service - the other libraries' catalogue, or the branch
system - and it is a real dependency, with real failure modes.

Your job is the Start module. It compiles, it is formatted, it passes every
Quality gate, and its tests pass - and it does not ask anybody. It assumes
every copy is on the shelf, so it lends copies that are not there, and it
never has to deal with what the service says.

Two Diagnostic tests say what the Lab asks for:

1. **A copy the availability service says is on loan is refused.** The
   service answers "not available", and the application must answer `409
   Conflict` with a problem detail - not `201 Created`.
2. **A copy the service does not know about is reported as a problem
   detail.** The service answers `404`, and the application must answer `404`
   with a problem detail - not a `500` with a stack trace in the body, and
   not a loan.

Run them:

```bash
./mvnw -f labs -pl lab-04-web-stack-jackson3-start verify -Pdiagnostics
```

The Solution module shows what the answer looks like: an `@HttpExchange`
interface for the availability service, its base URL in the configuration,
and an exception handler that turns the client's typed exceptions into
problem details.