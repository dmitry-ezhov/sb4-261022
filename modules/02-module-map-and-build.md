# M02 - The module map and a multi-module build

M01 was about what changed in the *code*. This Module is about the build
around it: which artifact hands you which responsibility, and how a repository
of many modules keeps one version of the truth. By the end of it you will be
able to read any Spring Boot POM and say, out loud, where each version came
from and who decided it.

## What you knew

A Spring Boot 2 application was one directory and one POM. You inherited from
`spring-boot-starter-parent`, which inherited from `spring-boot-dependencies`
and gave you a managed version for every artifact Boot knew about. You added
`spring-boot-starter-web` and got Spring MVC, Tomcat, Jackson and the
`jackson-datatype-jsr310` module in one move. You set `java.version` in
`<properties>` and let the parent's compiler configuration pick it up. You ran
`mvn package` and got a fat jar you started with `java -jar`, because
`spring-boot-starter-parent` bound the `repackage` goal for you.

If you had more than one application, you had more than one POM, and you
copied things between them: a version here, a plugin block there. That copying
was the disease, and the cure is the module map.

You are assumed to know all of that fluently. You are **not** assumed to know
Maven's three different mechanisms - parent, aggregator, BOM import - and how
they differ, nor how Boot 4's modularised starters change the arithmetic of
what a POM should contain.

## What changed

|  | Spring Boot 2 | Spring Boot 3 | Spring Boot 4 |
|---|---|---|---|
| Web starter | `spring-boot-starter-web` | `spring-boot-starter-web` | `spring-boot-starter-webmvc` - the old name still resolves, but it is deprecated |
| JSON starter | `spring-boot-starter-json`, inside the web starter | `spring-boot-starter-json`, inside the web starter | `spring-boot-starter-jackson` - renamed, and on its own |
| Web test slice | inside `spring-boot-starter-test` | inside `spring-boot-starter-test` | `spring-boot-starter-webmvc-test` |
| Where `@AutoConfigureMockMvc` lives | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| HTTP client starter | none | none | `spring-boot-starter-restclient` - new in Boot 4 |
| What the web starter pulls in | `spring-boot-starter`, `spring-boot-starter-json`, `spring-boot-starter-tomcat`, `spring-web`, `spring-webmvc` | the same five | `spring-boot-starter`, `spring-boot-starter-jackson`, `spring-boot-starter-tomcat`, `spring-boot-http-converter`, `spring-boot-webmvc` |
| A repository of modules | one POM per application; a parent was a way to share versions | one POM per application; a parent was a way to share versions | the starters are per-responsibility, so a repository now has a *parent* (shared versions and shared build decisions) and an *aggregator* (the list of modules) - and one POM can be both |

## How it works now

### The module map: one responsibility, one starter

Boot 4's starters are small and named after what they provide. Here is the
whole map you need for this course, read straight off the published POMs of
Boot 4.1.1:

| You need | Add | And you get |
|---|---|---|
| A servlet web application | `spring-boot-starter-webmvc` | `spring-boot-starter`, `spring-boot-starter-jackson`, `spring-boot-starter-tomcat`, `spring-boot-http-converter`, `spring-boot-webmvc` |
| JSON, and nothing else | `spring-boot-starter-jackson` | `spring-boot-starter`, `spring-boot-jackson` |
| To test that web application | `spring-boot-starter-webmvc-test` | `spring-boot-starter-test`, `spring-boot-starter-webmvc`, `spring-boot-webmvc-test`, `spring-boot-resttestclient`, `spring-boot-starter-jackson-test` |
| An HTTP client | `spring-boot-starter-restclient` | `spring-boot-starter`, `spring-boot-starter-jackson`, `spring-boot-restclient` |
| Persistence | `spring-boot-starter-data-jpa` | `spring-boot-starter`, `spring-boot-starter-jdbc`, `spring-boot-data-jpa`, `spring-boot-jdbc` |

Read the third row again: **the test starter pulls the whole web stack in at
compile scope.** That is not an accident - a module that wants to test its web
layer wants the web layer - but it is also the reason you should not add
`spring-boot-starter-test` and hope the web test slice came with it. In Boot 4
it did not; the slice is its own artifact, and the starter is its own name.

Two habits follow, and they are the whole lesson:

1. **Depend on the narrowest starter that gives you what you use.** A
   JSON-only consumer does not need the web starter, and a batch job does not
   need Tomcat.
2. **The starter's name tells you what you got.** `spring-boot-starter-webmvc`
   is the web starter; `spring-boot-starter-webmvc-test` is its test slice.
   When a dependency's name changes, the thing it brings usually does too.

### Parent, aggregator, BOM: three mechanisms, not one

Maven gives you three ways to share build decisions, and they are constantly
confused with each other. They are not the same thing:

| Mechanism | Where it is written | What it does |
|---|---|---|
| **Parent** (`<parent>`) | in the child POM | the child *inherits* the parent's configuration: properties, dependency versions, plugin configuration, and the parent's own `<dependencies>` |
| **Aggregator** (`<modules>`) | in the parent's POM | the parent *builds* the listed modules, in one reactor, in the right order |
| **BOM import** (`<scope>import</scope>` on a `pom` dependency) | in `<dependencyManagement>` | the listed artifacts get their *versions* from the imported BOM, and nothing else |

A POM can be a parent and an aggregator at once - that is the normal shape of
a repository - and this course's repository is exactly that:

- the root POM is the **parent** of everything and the **aggregator** of the
  labs. It owns every dependency version, every plugin version and every
  Quality gate;
- `labs/pom.xml` is the **aggregator** that lists the lab modules. It
  configures nothing, because every decision it could make is already made
  once, in the root POM;
- each lab module declares **only its own dependencies**, with no versions.

The rule that makes this work: **the parent is where decisions live, the
aggregator is where the list lives, and the child is where the usage lives.**

### Managed versus inherited

Two words that look alike and are not:

- **Managed** means the version is *known* to the parent. `<dependencyManagement>`
  in the parent says "if you declare `spring-boot-starter-webmvc`, it is
  version 4.1.1". It does **not** put the dependency on your classpath.
- **Inherited** means the dependency is *on your classpath*. The parent's
  `<dependencies>` block adds it to every child, whether the child mentions it
  or not.

This course's root POM does exactly two things by inheritance:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.jspecify</groupId>
    <artifactId>jspecify</artifactId>
  </dependency>
</dependencies>
```

Every lab module gets the test starter and the null-safety annotations without
asking. A child that declares them again is not being explicit - it is
duplicating a decision that has already been made, and the next version bump
will only reach the copy if you remember to update both.

The other half of the rule: **a child never repeats a version.** If a lab
module writes `<version>4.1.1</version>` on a dependency, the version bump
that the parent was designed to make in one place now needs to be made in
every place. That is the copying disease from the Boot 2 days, and it is the
single most common thing a reviewer will catch in a POM.

### BOM imports

Boot's own BOM is not imported - it is *inherited*. `spring-boot-starter-parent`
4.1.1's parent is `spring-boot-dependencies`, and that is where every managed
version comes from. Nothing in your POM mentions it.

A BOM import is for the things Boot does not manage. This course imports the
Testcontainers BOM explicitly:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-bom</artifactId>
      <version>${testcontainers.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

`<type>pom</type>` plus `<scope>import</scope>` is the whole mechanism: the
imported POM's `dependencyManagement` is merged into yours, and nothing else
about it is inherited. Imports are resolved in the order they appear, and the
first definition of a version wins.

### Packaging

The packaging of a Boot application has not changed since Boot 2, and it is
worth knowing exactly where it comes from, because "why is my jar executable?"
is a question every Boot 2 developer asked once.

`spring-boot-starter-parent` binds `spring-boot-maven-plugin`'s `repackage`
goal in its plugin management. Every module that inherits from it therefore
gets an executable jar without declaring a plugin at all: the `package` phase
writes a plain jar, and `repackage` rewrites it with the application's classes
under `BOOT-INF/classes` and its dependencies under `BOOT-INF/lib`, with
`JarLauncher` as the entry point in the manifest. `java -jar target/app.jar`
works because of that, not because of anything in your POM.

If you do declare the plugin - to set `mainClass`, or to turn the
repackaging off - declare it **without a version**, and let the parent's plugin
management supply the version. A version written into your POM is a version
that will still be there when the parent moves on.

Every Lab module in this course is an application - each has an application
class - so `./mvnw -f labs package` produces an executable jar per Lab, and
the manifest of one of them says exactly what the paragraph above describes:

```text
Main-Class: org.springframework.boot.loader.launch.JarLauncher
Start-Class: com.example.library.LibraryApplication
Spring-Boot-Classes: BOOT-INF/classes/
Spring-Boot-Lib: BOOT-INF/lib/
```

Note the launcher package, because it moved twice: Boot 2 shipped
`org.springframework.boot.loader.JarLauncher`, Boot 3 shipped that one *and*
a new `org.springframework.boot.loader.launch.JarLauncher`, and Boot 4 ships
only the new one. You will meet that in a stack trace long before you meet it
in a POM.

## The Lab

The Lab is the same library application you finished in M01 - the catalogue of
[Titles](../GLOSSARY.md) and [Copies](../GLOSSARY.md), the
[Members](../GLOSSARY.md) who borrow them and the
[Reservations](../GLOSSARY.md) they hold. Its *behaviour* is done. Its
*POM* is not: it is written the way a Boot 2 build was written, with versions
pinned in the child, plugins pinned in the child, dependencies re-declared that
the parent already provides, and the deprecated web starter.

Four Diagnostic tests say what the Lab asks for. They are the first Diagnostics
in the course that read a POM rather than an HTTP response, because M02 is the
one Module whose subject is the build and not the application:

1. **No dependency declares a version.** Every version comes from the parent's
   management. A dependency may still use `${project.version}` - that is a
   reference, not a pin - but a literal version is a pin.
2. **No plugin declares a version.** Plugin versions come from the parent's
   plugin management.
3. **The module declares only what it uses.** `spring-boot-starter-test` and
   `jspecify` are inherited from the root POM; declaring them again is
   duplication.
4. **The starters are the Boot 4 ones.** The module depends on
   `spring-boot-starter-webmvc`, and it does not depend on the deprecated
   `spring-boot-starter-web`.

Run them:

```bash
./mvnw -f labs -pl lab-02-module-map-and-build-start verify -Pdiagnostics
```

Each failure names the dependency or the plugin that has to change. The
Solution module shows what a correct child POM looks like - and it is short:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
  </dependency>
</dependencies>
```

No versions. No plugin block. Nothing that the parent has already decided.

## Quiz

Five questions, with answers that explain themselves. Authored with the quiz
ticket that follows this one.

## Checklist

What a reviewer applies to a solution of this Lab. Authored with the checklist
ticket that follows this one.
