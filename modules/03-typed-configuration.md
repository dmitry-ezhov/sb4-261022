# M03 - Typed configuration and validation

M02 was about the build. This Module is about the two numbers the library is
configured with - how long a [Loan](../GLOSSARY.md) lasts and how many a
[Member](../GLOSSARY.md) may have at once - and about what the application
does when they are wrong. By the end of it you will have configuration that is
typed, named, validated at startup, and impossible to misread.

## What you knew

Boot 2 configured an application from `application.properties` or
`application.yml`, and you got the values in two ways. The quick way was
`@Value("${library.loan.period:14}")` on a field, with the default written
into the annotation. The typed way was `@ConfigurationProperties`, which bound
a group of properties onto a JavaBean through its setters. Somewhere in
Boot 2.2 an immutable way arrived: `@ConstructorBinding`, which bound through
a constructor instead, and records - which had just arrived in Java - could
be bound that way too. From Boot 2.3 you could put `@Validated` on the
properties class and JSR-303 constraints on its fields, and a bad value would
stop the application at startup instead of at the first request.

You are assumed to know all of that fluently. You are **not** assumed to know
how the binding rules work in detail - the naming, the relaxed binding, what
happens to a missing property - nor that Boot 3 changed the default binding
style, nor that the constraint annotations moved namespaces along with
everything else in the Jakarta migration.

## What changed

|  | Spring Boot 2 | Spring Boot 3 | Spring Boot 4 |
|---|---|---|---|
| Binding an immutable configuration | `@ConstructorBinding` is required - a record is the exception | constructor binding is the **default**: one constructor means constructor binding | the same |
| The constraint annotations on it | `javax.validation` | `jakarta.validation` | `jakarta.validation` |
| Where the validation implementation comes from | inside `spring-boot-starter-web` before 2.3, `spring-boot-starter-validation` from 2.3 on | `spring-boot-starter-validation` | `spring-boot-starter-validation` |
| Registering a properties type | `@EnableConfigurationProperties`, `@Component`, or `@ConfigurationPropertiesScan` (from 2.2) | the same three | the same three, plus `@ConfigurationPropertiesSource` for a type that lives in another module |

Two of these deserve a note. **The default binding style changed in Boot 3**:
a class with a single constructor is now bound through it, and
`@ConstructorBinding` is only there to choose between several constructors.
**The constraint annotations moved with everything else**: a Boot 2
`@NotNull` from `javax.validation` is not read by a Boot 3 or Boot 4
application, so the constraint becomes inert rather than wrong.

## How it works now

### Three ways to get a value, and when to use each

| | Use it for | What you get |
|---|---|---|
| `@Value("${...}")` | one value, and you want it in one place | a String, unless you ask for a conversion; the placeholder is resolved at startup, and if it cannot be resolved the application does not start |
| `@ConfigurationProperties` | a group of related values | a typed object, bound once, with the names in one namespace |
| `Environment` | values that are not known at compile time | Strings, looked up by key |

The typed one is the one you want for anything with a rule attached to it,
because the rule can live on the type.

### Configuration properties are records now

A configuration properties class in Boot 4 is a record:

```java
@ConfigurationProperties(prefix = "library.loan")
public record LoanProperties(Duration period, int limit) {}
```

Constructor binding is the default, so there is no annotation to remember, and
a record gives you immutability, `equals`, `hashCode` and `toString` for
free. Two rules come with it:

- **The class must be compiled with `-parameters`.** Otherwise the binder
  cannot tell which constructor parameter is which. You do not have to
  remember that: `spring-boot-starter-parent` configures the compiler plugin
  with `<parameters>true</parameters>`, and this course's parent POM sets
  `maven.compiler.parameters` to `true` as well.
- **The class must be registered.** Either `@EnableConfigurationProperties`
  on a configuration class, or `@ConfigurationPropertiesScan` on your
  application class, which scans the package the application lives in. A
  `@ConfigurationProperties` class that is neither scanned nor enabled is
  simply not bound, and nothing tells you.

### The naming rules

The name in the configuration file is not the name in the class. Boot binds
`library.loan.period` to the `period` component, `library.loan.max-active-loans`
to `maxActiveLoans`, and `library.loan.MAX_ACTIVE_LOANS` as well: the binding
is *relaxed*, and the canonical form is lower-case kebab-case.

The rules that catch people out:

- **A `Duration` is written in Boot's shorthand.** `14d` is fourteen days,
  `PT14D` is the same thing in ISO-8601, `240h` is ten days. The value in the
  file is a String; the conversion happens in the binder, so the type in the
  record is what decides the format.
- **A bare number is a number of milliseconds.** `120000` is two minutes, not
  an error and not "no unit": the shorthand (`2m`, `14d`) and the ISO form
  (`PT2M`) both carry a unit, and a bare number does not, so Boot picks the
  millisecond for it. Write the unit you mean - which is why the loan period
  in this course is written `14d`.
- **Lists and maps bind from indexed and keyed properties.**
  `library.loan.holidays[0]=...` binds to a `List<String>`,
  `library.loan.rooms[main]=...` to a `Map<String, String>`.

### Startup validation

A configuration value that is wrong is a bug that should be found before the
first request, not after. Put `@Validated` on the properties class, put
Jakarta constraints on its components, and the application refuses to start:

```java
@Validated
@ConfigurationProperties(prefix = "library.loan")
public record LoanProperties(@NotNull Duration period, @Min(1) int limit) {}
```

`@NotNull` says the property must be there; `@Min(1)` says the limit must be
at least one. If the limit is `0`, the application fails at startup with a
message that names the property and the value - and it never serves a
request, because there is no context to serve it with.

Two things to know:

- **A missing value and an invalid value are different failures.** A missing
  `Duration` binds to `null` and is caught by `@NotNull`. A missing `int`
  binds to `0`, which `@Min(1)` catches - but a `0` that was *meant* is
  indistinguishable from a missing value, which is why a `Duration` with
  `@NotNull` is the safer shape for a value that has no sensible zero.
- **The validation needs an implementation on the classpath.** The
  constraints are Jakarta's; the implementation is Hibernate Validator, and
  the starter that brings it is `spring-boot-starter-validation`. Without it
  the constraints are decoration.

### Defaults

`@DefaultValue` gives a constructor parameter a default when the property is
absent:

```java
public record LoanProperties(@DefaultValue("14d") Duration period, @DefaultValue("3") int limit) {}
```

The value is a constant - it is not a placeholder, so `${...}` inside it is
not resolved. It applies only when the property is *not found*; a property
that is present but empty still binds as empty.

Whether to use `@DefaultValue` or a plain default in the property file is a
judgement call worth making deliberately. A default in the code is visible to
whoever reads the type; a default in the file is visible to whoever
deploys the application, and can be changed without a rebuild.

## The Lab

The Lab is the library application you finished in M02, with one thing added:
the [Loan](../GLOSSARY.md). A [Member](../GLOSSARY.md) borrows a
[Copy](../GLOSSARY.md); the loan has a due date, and the rules say a loan
lasts fourteen days and a member may hold three at a time.

Your job is the Start module. It compiles, it is formatted, it passes every
Quality gate, and its tests pass - and the loan period is a constant in the
code, where nobody can change it without a rebuild and nobody can find it by
searching the configuration. There is no loan limit at all.

Two Diagnostic tests say what the Lab asks for:

1. **The loan period comes from the configuration.** With
   `library.loan.period=30D`, a loan created today is due in thirty days.
   Today the application says fourteen, because fourteen is a constant.
2. **An invalid loan limit stops the application at startup.** With
   `library.loan.limit=0`, the application must not start - and the failure
   must name the property that was wrong.

Run them:

```bash
./mvnw -f labs -pl lab-03-typed-configuration-start verify -Pdiagnostics
```

The Solution module shows what the answer looks like: a `@ConfigurationProperties`
record with the period and the limit, `@Validated` on it, the constraints on
its components, and the loan controller reading the period from the bound
properties rather than from a constant.

The loan period is a number of days - that is what the glossary says it is -
and the due date is a date, so the controller converts the bound `Duration`
to days. The configuration file writes `14d`, the application knows days.

## Quiz

Five questions about configuration. Read the answer after you have answered it -
the answers explain why, not only what.

1. **Your configuration record has one constructor and two components. Which
   binding style does Boot use, and what do you add when a class has two
   constructors?**

   Constructor binding. It has been the default since Boot 3 for a class with
   a single parameterized constructor; in Boot 2 you had to write
   `@ConstructorBinding` to get it, with a record as the exception. With two
   constructors, `@ConstructorBinding` says which one the binder should use.

2. **You put `@NotNull` on a `Duration` component and `@Min(1)` on an `int`
   component, and the configuration file omits both. What does the
   application do?**

   It refuses to start. The `Duration` binds to `null` and `@NotNull`
   rejects it; the `int` binds to `0` - the primitive's default - and `@Min(1)`
   rejects that. The difference is worth keeping in mind: for the `int`,
   "not configured" and "configured as zero" are the same value, so a missing
   limit is only caught because zero happens to be illegal. For the `Duration`
   they are different, and the constraint says which one it is.

3. **The constraints are on your class, and the application starts happily
   with a nonsense value. What is missing?**

   The validation implementation. The constraints are Jakarta's -
   `jakarta.validation.constraints.*` - and the implementation is Hibernate
   Validator, which arrives with `spring-boot-starter-validation`. Without it
   the annotations are decoration, and nothing warns you.

4. **You write `library.loan.period=120000` and expect two minutes. What does
   Boot bind?**

   Two minutes. A bare number is a number of milliseconds: the shorthand
   (`2m`, `14d`) and the ISO form (`PT2M`) both carry a unit, and a bare
   number does not, so Boot picks the millisecond for it. That is exactly why
   the loan period in this course is written `14d` - a reader should not have
   to know the default unit to know what the number means.

5. **You put `@DefaultValue("14d")` on the period component. When is it used,
   and what does it not do?**

   Only when the property is *not found*. A property that is present but empty
   still binds as empty, and does not get the default. And the value must be a
   constant: it is not a placeholder, so `${...}` inside it is not resolved.

## Checklist

What a reviewer applies to a solution of this Lab to judge it production-shaped
rather than merely working. Every item is observable - you can check it by
reading the change - and none of them repeats a gate the build already
enforces.

- **The rules are configuration.** The loan period and the loan limit are
  bound, not constants: there is no `14` in the Java code, and the rules can
  be changed without a rebuild.
- **The configuration class is a record**, immutable, and it is registered -
  by `@ConfigurationPropertiesScan` or by `@EnableConfigurationProperties`,
  but not by both, and not by neither.
- **The defaults live in the configuration file**, where whoever deploys the
  application can see them and change them.
- **The constraints are on the components**, and the validation implementation
  is on the classpath. `@Validated` on a class with no validator is
  decoration.
- **An invalid value stops the application at startup**, and the failure names
  the property that was wrong. A rule that is only checked when it is used is
  a rule that will be used at the wrong time.
- **The vocabulary is the glossary's.** [Loan](../GLOSSARY.md),
  [Loan period](../GLOSSARY.md), [Loan limit](../GLOSSARY.md),
  [Due date](../GLOSSARY.md), [Member](../GLOSSARY.md),
  [Copy](../GLOSSARY.md). Not "borrow", not "rental", not "due".
- **The clock is injected**, not called statically, so a test can decide what
  today is. `LocalDate.now()` in a controller is a test that cannot exist.
- **The failures are still problem details.** The Lab changed how the rules
  arrive, not how the API reports trouble.
