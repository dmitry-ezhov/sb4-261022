# M01 - Boot 2 to Boot 4: what changed

Spring Boot 4 is the third major version you have not shipped. This Module is
the bridge: it names every thing that changed between the Boot 2 you remember
and the Boot 4 you will build, and it does it in a way you can check rather than
believe. By the end of it you will know which of your habits are still correct,
which are quietly wrong, and which of your dependencies will not resolve.

## What you knew

In Boot 2, a web application was one starter and one annotation. You added
`spring-boot-starter-web`, it dragged in Tomcat, Jackson and Hibernate
Validator, and you wrote `@SpringBootApplication`. Configuration went into
`application.properties` or `application.yml`, and you bound it with
`@Value`, or with `@ConfigurationProperties` when you wanted it typed.

Your imports were `javax.persistence.*` and `javax.validation.*`. Your JSON
was `com.fasterxml.jackson.*`. Your tests used `@MockBean` from
`org.springframework.boot.test.mock.mockito`. Your auto-configuration
registered itself in `META-INF/spring.factories`. Outbound HTTP was a
`RestTemplate` you configured by hand, and if it failed you handled the
failure yourself.

You are assumed to know all of that fluently. You are **not** assumed to know
anything about Jakarta, Jackson 3, the modularised starters, the declarative
HTTP client, or the resilience and API versioning that Framework 7 brings.

## What changed

|  | Spring Boot 2 | Spring Boot 3 | Spring Boot 4 |
|---|---|---|---|
| Java baseline | 8 | 17 | 17 (this course uses 25) |
| Jakarta EE | `javax.*` | `jakarta.*` | `jakarta.*` |
| Jackson | 2.x, `com.fasterxml.jackson` | 2.x, `com.fasterxml.jackson` | 3.x for core and databind (`tools.jackson`); the annotations stay behind at `com.fasterxml.jackson.annotation` |
| Web starter | `spring-boot-starter-web` | `spring-boot-starter-web` | `spring-boot-starter-webmvc` - the old name still resolves, but it is deprecated |
| JSON starter | inside the web starter | inside the web starter | `spring-boot-starter-json`, on its own |
| Web test slice | inside `spring-boot-starter-test` | inside `spring-boot-starter-test` | `spring-boot-starter-webmvc-test` |
| Mock bean annotation | `@MockBean` | `@MockBean`, deprecated in 3.4 | `@MockitoBean` |
| Auto-configuration registration | `META-INF/spring.factories` | `META-INF/spring/…AutoConfiguration.imports` | same |
| Declarative HTTP client | none - `RestTemplate` by hand | `@HttpExchange` interfaces, auto-configured by `spring-boot-starter-restclient` | same |
| Resilience | Resilience4j, or your own | Resilience4j | built in: `@Retryable`, `@ConcurrencyLimit` |
| API versioning | whatever you wrote | whatever you wrote | built in: `ApiVersionResolver` and friends |
| Null safety | none | none | JSpecify annotations on the framework's own API |

## How it works now

### The starters are modules now

Boot 3 split the old monolithic starters into one artifact per technology, and
Boot 4 finished the job. The web starter is now `spring-boot-starter-webmvc`,
and the Jackson stack is `spring-boot-starter-json` rather than something the
web starter happened to include.

The old name still resolves - `spring-boot-starter-web` was **deprecated**, not
removed - so a Boot 2 POM keeps building. That is the trap: a deprecated
starter is a warning, not an error, and it will still be there when you next
upgrade. Change it deliberately:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

The rule to take away: depend on the narrowest starter that gives you what you
use. A JSON-only consumer does not need the web starter.

### The web test slice moved out of the test starter

This one costs every Boot 2 developer an afternoon. In Boot 3,
`spring-boot-starter-test` brought the web test slice with it. In Boot 4 it
does not. The slice lives in its own starter and its own package:

```java
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
```

The package is `org.springframework.boot.webmvc.test.autoconfigure`, not the
`org.springframework.boot.test.autoconfigure.web.servlet` you remember. And the
slice needs its own dependency:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webmvc-test</artifactId>
  <scope>test</scope>
</dependency>
```

Miss the dependency and the annotation does not exist. Miss the import and it
does not compile. Both failures are at least loud.

### Jackson 3 has a new package root

Jackson 3 is not a Jackson 2 with a version bump. The package root moved:
`com.fasterxml.jackson.databind.ObjectMapper` is now
`tools.jackson.databind.json.JsonMapper`, and the Maven groupId moved with it
(`com.fasterxml.jackson.core` is now `tools.jackson.core`).

**The annotations did not move.** This is the part that catches people out.
Jackson 3 still uses the Jackson 2 annotations artifact - in the version this
course builds against, `com.fasterxml.jackson.core:jackson-annotations` is
still at **2.21**, and the package is still `com.fasterxml.jackson.annotation`.
So a Jackson 3 class mixes two roots:

```java
import com.fasterxml.jackson.annotation.JsonProperty;   // Jackson 2 annotations
import tools.jackson.databind.json.JsonMapper;           // Jackson 3 databind
```

If you find yourself importing `tools.jackson.annotation.JsonProperty`, you
are guessing - the annotation is where it has always been.

The important consequence is about the libraries around you, not about your own
code. Jackson 2 still exists and is still widely used - WireMock 3, for one,
speaks Jackson 2. Because the package roots differ, the two coexist on the
same classpath without conflict. What does not work is mixing them
purposefully: do not serialize your own types with a Jackson 2 mapper when the
application is on Jackson 3, and expect the annotations you wrote to be
honoured.

### Jakarta

Boot 3 moved the whole stack from `javax.*` to `jakarta.*`. The framework's
own API is `jakarta.*` now: validation constraints, persistence annotations,
servlet APIs. If your own code touches those namespaces, you migrate it. If it
does not - a plain service that never imports `javax.persistence` - it does
not.

The migration is mechanical, but it is not optional: `javax.validation`
constraints are not read by a framework that looks for `jakarta.validation`
ones, and your constraints quietly stop firing.

### The framework grew things you used to buy

Framework 7 brought two things that used to be separate products or custom
code.

**Resilience is now built in.** `org.springframework.resilience` gives you
`@Retryable` and `@ConcurrencyLimit`, enabled with
`@EnableResilientMethods`. There is no Resilience4j dependency to add, and no
interceptor to configure. The retry semantics are ordinary Spring semantics:
proxy-based, so a self-invocation inside the bean is not retried.

```java
import org.springframework.resilience.annotation.Retryable;
```

**API versioning is now built in.** `org.springframework.web.accept` has
`ApiVersionResolver`, `ApiVersionHolder` and friends, so a URL that carries a
version resolves through a strategy you write rather than through a filter
chain you write.

Both of these are new *in Framework 7*, which is Boot 4's framework. Neither
existed in the Framework 6 that Boot 3 shipped. If you have seen articles
about either, they were either about something else or about proposals.

### Null safety

Boot 4's own API is annotated with JSpecify annotations, and the course code
is too. It costs nothing - `jspecify` is a compile-scope dependency the
parent POM already gives every module - and it makes the nullability of a
framework method visible in the editor.

## The Lab

The Lab is the catalogue API of the library application: a [Title](../GLOSSARY.md)
is a work, a [Copy](../GLOSSARY.md) is a physical item of a title, and
[Members](../GLOSSARY.md) borrow them. It is in the repository twice - once
written the way a Boot 2 application would have written it, once written the
way Boot 4 wants it.

Your job is the Start module. It compiles, it is formatted, it passes every
Quality gate, and its tests pass. What it does not do is the thing the Lab is
about. Three Diagnostic tests say what that is:

1. The titles endpoint must return a JSON **array** of typed records, not a
   wrapper object with an `items` key.
2. The copies endpoint must return a JSON **array**, not a wrapper object.
3. An unknown title must be reported as an RFC 9457 **problem detail**, served
   as `application/problem+json`, with a `title` and a `status` in the body -
   not as an empty 404.

Run them:

```bash
./mvnw -f labs -pl lab-01-boot2-to-boot4-start verify -Pdiagnostics
```

Three failures, and each one names what is missing. The Solution module
shows what the answer looks like - a record payload, a domain exception, and a
`@RestControllerAdvice` that turns the exception into a `ProblemDetail`:

```java
import org.springframework.http.ProblemDetail;
```

Read the Solution module after you have solved the Lab, not instead of it.

## Quiz

Five questions, with answers that explain themselves. Authored with the quiz
ticket that follows this one.

## Checklist

What a reviewer applies to a solution of this Lab. Authored with the checklist
ticket that follows this one.
