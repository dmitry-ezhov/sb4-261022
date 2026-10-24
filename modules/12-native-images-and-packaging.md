# M12 - Native images and packaging

M11 closed with the resilience move written in: the requests run on
virtual threads, the availability call ahead of every borrow is bounded
and retried, the exhausted budget answers problem details, and shutdown
drains under a window the application names. The code in your editor is
finished. What is not settled yet is the form it travels in: the jar Boot
4.1.1 packages, the JVM switches it may boot under, the container image
that carries it, the AOT cache that makes an all-JVM deployment start
cheap, and the GraalVM native image for the service where startup is
worth a build-time bet. This Module is the packaging treatment of
everything M01 through M11 built. It covers what ahead-of-time processing
actually is and *when it reads your application*, what the
`spring.aot.enabled` switch turns a jar into, what Boot 4 removed from
the jar it inherited (the classic loader, the launch scripts, the
`layertools` spelling), how the container-image defaults moved across
the three generations, and what a native build itself costs - the
minutes, the megabytes and the version pin you accept. By the end you can
review any packaging diff on this codebase and say which lines are Boot 3
carry-over and which are the 4.1.1 spellings. One boundary this course
draws, and this Module is the one that must say it: the *depth* a
native-image specialist wants - profile-guided optimization, `-H:`
reachability flags, GraalVM internals - is the sibling course's subject;
this Module teaches the mechanism, the Spring-side API and what the build
decides, and stops there.

## What you knew

In Boot 2 the deployment story was one command and one artifact, and you
could recite it. `spring-boot:repackage` rewrote `target/*.jar` in place,
nested every dependency jar inside it, installed the loader as the
manifest's main class, and `java -jar` was the whole run ceremony. The
2.7.18 repackage descriptor carries two params that were part of the
jar's identity: `executable` and `embeddedLaunchScript` (plus
`embeddedLaunchScriptProperties` - all read off the descriptor). Set
`executable` and the jar itself became a shell script an init system
could start, the launch script riding in front of the zip content - the
"fully executable jar". The loader came in two roles, both read off the
2.7.18 `spring-boot-loader` jar (35 classes, package
`org.springframework.boot.loader`): the default nested-jar `Launcher`
and `PropertiesLauncher`, the externalized-directory layout you chose
from the manifest. Jarmode tooling existed in one exact spelling:
`layertools` - the 2.7.18-era artifact is
`spring-boot-jarmode-layertools`, and no `spring-boot-jarmode-tools`
was published at that version - exploding the jar into layers for a
Dockerfile to spread across images. Startup cost was a JVM-side story,
and a hand-wired one: if you chased the JDK's AppCDS you wrapped
`-XX:ArchiveClassesAtExit` around your own training run, exited it
yourself, and shipped the `.jsa` beside the jar by your own means.

What the build did *not* do was decide anything at build time. Boot 2
resolved every bean definition, every condition and every reflection
call at run time: the 2.7.18 plugin descriptor has no `process-aot`
goal, the 2.7.18 `spring-boot` jar carries zero AOT machinery, and
`spring.aot.enabled` is a property that version never heard of. A native
image lived on the ecosystem's frontier: `spring-native`, an
*experimental* project outside Boot with its own hint files and
annotation processor - the ancestor of everything in this Module's Boot
4 column, and it never shipped a single Boot starter.

You are assumed to know all of that fluently. You are **not** assumed to
know that `process-aot` and `process-test-aot` arrived with the **3.0**
line (both goals are in the 3.0.13 descriptor, neither in 2.7.18's),
that Boot 3.2 rewrote the loader and *also* shipped the old one in a
separate `spring-boot-loader-classic` artifact behind a new
`loaderImplementation` param, that 4.1 removed that artifact and the
param, that 4.1 removed `executable` and `embeddedLaunchScript` from the
repackage mojo, that 3.3 renamed `layertools` to `tools` (the
`jarmode-tools` artifact exists from 3.3.13, absent at 3.2.12), that the
buildpack default builder moved twice inside Boot 3 to
`builder-jammy-java-tiny` (3.4) and then `builder-noble-java-tiny`
(3.5) - both read off the jars, that 3.5.16 pins GraalVM Native Build
Tools **0.10.6** while 4.1.1 pins **1.1.8**, that a Boot 4.1.1 native
build requires a GraalVM distribution in version **25** (the docs' own
prerequisite, quoted below), that the packaging doc page Boot 3.5 titled
"Class Data Sharing" is retitled **"AOT Cache"** in 4.1 (the old URL
redirects), and that the AOT cache is a JVM feature - JEP 483, introduced
in JDK 24, sharpened by JDK 25 - not a GraalVM one.

## What changed

The Boot 4 column is the one this course's build resolves - Spring Boot
4.1.1 over Spring Framework 7.0.9 - every cell read off the artifacts
the build pulls or their published parents: `unzip -l` and `javap`
across the `spring-boot`, `spring-boot-loader`,
`spring-boot-loader-tools` and `spring-boot-buildpack-platform` jars of
2.7.18, 3.0.13, 3.2.12, 3.4.13, 3.5.16 and 4.1.1 (versions the build
does not resolve itself were downloaded from Maven Central for the
read), the `META-INF/maven/plugin.xml` of spring-boot-maven-plugin at
all six of those versions, the published `spring-boot-dependencies`
POMs of 2.7.18, 3.5.16 and 4.1.1, and the `spring-boot-starter-parent`
POMs of 2.7.18, 3.0.13, 3.5.16 and 4.1.1 for the native-profile row.
Where a *behaviour* cannot be bytecode-proven -
what the AOT engine generates, the GraalVM version the build requires,
the deployment forms the docs recommend - the cells lean on the 4.1
reference docs, and the doc cites them where it does. The Boot 2 column
is the last Boot 2 (2.7), the Boot 3 column the last Boot 3 (3.5). The
runtime is JDK 25 throughout - this course's root POM sets
`<java.version>25</java.version>`.

| | Spring Boot 2 (2.7) | Spring Boot 3 (3.5) | Spring Boot 4 (4.1.1) |
|---|---|---|---|
| Ahead-of-time processing | none: every bean definition, condition and reflection lookup resolved at run time; no `process-aot` goal in the 2.7.18 descriptor, zero AOT classes in the `spring-boot` jar | `process-aot` / `process-test-aot` from **3.0** (both in the 3.0.13 descriptor): code, bytecode and hint JSON generated at build, the application *run* at build time under a closed world - mandatory for native, opt-in on the JVM | the same two goals in 4.1.1's descriptor; generated content lands in `target/spring-aot/main/{sources,classes,resources}` (the plugin page's own defaults); behind it Framework 7 reorganized the writer machinery (`LambdaHint` is new in 7.0.9, the 6.2.19 per-family writers consolidated into one `RuntimeHintsWriter`), and the docs give the JVM-AOT form its own packaging page |
| The runtime switch | the property does not exist | `spring.aot.enabled=true` runs the jar on its generated initializer; Framework's `AotDetector.AOT_ENABLED` reads the flag through `SpringProperties` (the constant's `ldc` string is exactly `spring.aot.enabled`, read in 6.2.19 and 7.0.9 alike) | the name is unchanged and *jar-backed* in 4.1.1: the constant-pool sentence "remove the system property 'spring.aot.enabled'" sits in `AotInitializerNotFoundFailureAnalyzer` - run an AOT-optimized jar with no initializer present and Boot's diagnostic names the switch by its exact spelling |
| CDS / the AOT cache | not managed by Boot: AppCDS was your own training run around a `java -jar` | from **3.3** (the `packaging/class-data-sharing.html` page exists at 3.3 and is absent at 3.2), titled "Class Data Sharing"; training run on the extracted form, `spring.context.exit=onRefresh` ends it after refresh; the flag itself enters Framework **6.1** (present in 6.1.14's `DefaultLifecycleProcessor`, absent from 6.0.23) | the page's whole subject is now **"AOT Cache"**: "Spring Boot supports the AOT cache for Java 25 and above" (the page's own sentence); the training run commands `-XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh` and the run flag `-XX:AOTCache=app.aot`; the old 3.5 page URL redirects - the title changed, the mechanism's bytecode did not |
| GraalVM native images | not a Boot feature: the ecosystem's experimental `spring-native` project carried it | built in from 3.0: the starter-parent's `native` profile (present at 3.0.13, absent from the 2.7.18 starter-parent) wires `process-aot` with the Native Build Tools plugin; 3.5.16's dependencies POM pins `native-build-tools-plugin.version` **0.10.6** | same profile, same commands (`mvn -Pnative spring-boot:build-image` / `mvn -Pnative native:compile-no-fork`), but the pin jumps to **1.1.8** (the 4.1.1 dependencies POM) and the GraalVM distribution must be version **25** - the docs' prerequisite sentence, quoted in "How it works now" |
| The jar's launcher | `Launcher` and `PropertiesLauncher` ship in *one* artifact (2.7.18 `spring-boot-loader`, 35 classes), chosen from the manifest | 3.2 rewrote the loader (`org.springframework.boot.loader.launch`, 66 classes at 3.5) *and* kept the old one behind a `loaderImplementation` param (the param is absent from the 3.0 descriptor, present from 3.2) plus a separate `spring-boot-loader-classic` artifact (present at 3.5.16, absent at 2.7.18) | the classic is **removed**: 4.1.1 publishes no `spring-boot-loader-classic` and the 4.1.1 repackage mojo has no `loaderImplementation` param - while `org.springframework.boot.loader.launch.PropertiesLauncher` still lists in the 4.1.1 loader jar: the *choice* is gone, not the launcher |
| Fully executable jar / launch scripts | `executable` and `embeddedLaunchScript` are repackage params: the jar is itself a start script an init system can run | the same params survive the whole 3.x line (the 3.0.13, 3.4.13 and 3.5.16 descriptors all read `executable` and `embeddedLaunchScript`) | both params are **gone** from the 4.1.1 repackage mojo; the 4.0 migration guide records the removals ("Classic Uber-Jar Loader Support", embedded launch scripts) - the jar is a jar again |
| Jarmode tooling | `jarmode=layertools`, backed by `spring-boot-jarmode-layertools` (present at 2.7.18; no `jarmode-tools` published) | 3.3 renamed it: `jarmode=tools`, backed by `spring-boot-jarmode-tools` (absent at 3.2.12, present from 3.3.13) | the `tools` jarmode carries forward with the same verbs - we ran `help` against the lab-11 artifact built on 4.1.1 and got exactly `extract`, `list-layers`, `help`; since 4.0 a manifest entry `Spring-Boot-Jar-Type: development-tool` labels a jar so a consuming uber jar can exclude it (the release notes' addition) |
| Container images | `build-image` since 2.3 (the mojo page's own "Since" column), default builder `paketobuildpacks/builder-jammy-base:latest` (read off the 2.7.18 buildpack-platform bytecode) | the default builder moved twice inside 3.x: 3.4 sets `builder-jammy-java-tiny:latest`, 3.5 sets `builder-noble-java-tiny:latest` (all read off the `spring-boot-buildpack-platform` jars of 2.7.18, 3.0.13, 3.2.12, 3.4.13 and 3.5.16); `trustBuilder` and `imagePlatform` params arrive in 3.4 (absent from the 3.0/3.2 descriptors) | byte-for-byte the 3.5 arrangement: same `noble-java-tiny` default (the 4.1.1 bytecode), same `trustBuilder`/`imagePlatform` params, same goals (`build-image`, `build-image-no-fork`) - what moves here is the pinned toolchain version, not your POM |

Eight rows, and the sum is one sentence: **the jar Boot 4 ships is
smaller in choices and larger in build-time commitments.** Three kinds
of change hide in that sentence:

- **The first kind is a removal.** Boot 4 deleted three parallel regimes
  the 3.x line had kept alive for upgrade's sake: the classic loader
  (its artifact and its param), the launch scripts, and the
  `layertools` spelling. Nothing in your application loses behaviour
  the 4.1.1 jar cannot still provide - the `tools` extraction covers
  what `layertools` covered - but a 3.x-era Dockerfile layered by
  `layertools`, a CI profile selecting `loaderImplementation=CLASSIC`,
  or an init.d unit pointing at a fully executable jar will not
  translate; each is a line the migration review strikes, not
  reconfigures.
- **The second kind is a version jump invisible in source.** The Native
  Build Tools pin moved from 0.10.6 to 1.1.8, and with it the GraalVM
  floor: the native build *requires* a version-25 compiler. A diff of
  the POM shows one property; the machine that runs the native build
  shows a swapped Java. No code changed - exactly the shape of upgrade
  item a review is most likely to miss, which is why it gets its own
  row.
- **The third kind is an expansion: deployment forms go first-class.**
  What Boot 3 built as means to an end (make native images possible)
  became in 4.x documented *ends in themselves*: an AOT-processed jar
  run on the JVM under `spring.aot.enabled`, a training-run-produced
  AOT cache under `-XX:AOTCacheOutput` for services that stay on
  Java 25, own packaging pages each. The reader's cost is conceptual:
  the build-time environment is now part of the deployment contract,
  because every one of these forms evaluates conditions at *build*
  time and freezes them into the artifact.

## How it works now

### AOT processing: the build reads your application once

The core of the Module is one sentence of understanding: **ahead-of-time
processing is your application's container, walked through once at build
time, so its decisions can be harvested.** `spring-boot:process-aot`
does that walk right there in the build: an `ApplicationContext` is
refreshed in an AOT-specific mode, and the conditions are evaluated
against the *build-time* environment - the how-to page's own sentence:
ahead-of-time processing "optimizes the application and evaluates
`@Conditional` annotations based on the environment at build time".
Profile name mis-remembered at build time *is* the deployment you ship
from then on. What this refresh is and is not is Framework's own
javadoc-grade sentence: "Refresh an ApplicationContext for AOT
processing. Contrary to a traditional refresh, this version only creates
bean definitions, not bean instances." The processors walk the bean
definitions, and write down what they saw:

- **Java source** - each configuration class becomes a generated
  `...__BeanDefinitions` class, and the run's entry plays back through a
  generated `ApplicationContextInitializer` Boot applies instead of the
  reflective startup path. That initializer is the whole mechanism: a
  run *without* it under the AOT switch is the failure the analyzer in
  the delta table names, and the arrangement it replaces - the reflective
  startup path - is not something you hand-write; the source is the
  container's own, generated.
- **Bytecode** - proxy classes and friends GraalVM must see directly.
- **GraalVM hint JSON** under
  `META-INF/native-image/{groupId}/{artifactId}/`:
  `reflect-config.json`, `resource-config.json`, `proxy-config.json`,
  `jni-config.json` and `serialization-config.json` - the exact five
  files the docs enumerate. What the experimental `spring-native` asked
  you to *author* by hand, the 4.1.1 build records because it watched
  the application run.

The defaults land in `target/spring-aot/main/sources`,
`target/spring-aot/main/classes` and `target/spring-aot/main/resources`
(the plugin page's own parameter table), and the packaged jar includes
them without your hand doing anything.

The limits are the closed world, and the native-image chapter's own
list is the one to read as a review checklist:

> The application classpath is fixed at build time and cannot change.
> There is no lazy class loading. The Spring `@Profile` annotation and
> profile-specific configuration have limitations. Properties that
> change if a bean is created are not supported (for example,
> `@ConditionalOnProperty` and `.enabled` properties).

The sentences from that list worth the reader's whole attention, because
they answer what breaks in code that *compiles*: the Boot how-to page's
plain consequence - "the beans which are created while ahead-of-time
processing at build time are then always created when running the
application and can't be switched off" - and Framework's own AOT chapter
adds the two registrations a build-time container cannot transform, read
verbatim from its reference: "Bean definitions with instance suppliers
(lambdas or method references) cannot be transformed ahead of time", and
"Beans registered as singletons (using `registerSingleton`...) cannot be
transformed ahead of time either". The registration a Boot 2 codebase
did habitually straight onto the `BeanFactory` is now invisible to the
build; the pattern the framework names instead is
`BeanDefinitionRegistry`-based registration via an
`ImportBeanDefinitionRegistrar`. Where a build-time profile must ride
along, the how-to page shows it as a `process-aot` execution
configuration:

```xml
<execution>
  <id>process-aot</id>
  <configuration>
    <profiles>production,eu</profiles>
  </configuration>
</execution>
```

Profiles which only set configuration properties that *do not influence
conditions* survive freely (the same page's sentence), so the review is
ranking which of yours actually decide something.

Running an AOT-processed jar on the JVM is one system property:

```bash
java -Dspring.aot.enabled=true -jar target/my-app.jar
```

One build detail the packaging page states and a review should quote:
the jar is only built *with* the AOT code when the build carries
`-Pnative` ("For Maven, this means that you should build with
`-Pnative` to activate the native profile... `mvn -Pnative package`") -
which is why `mvn -Pnative package` is itself one of the deployment
forms Boot 4 documents even for an image that will never be native.

And it pays in startup because the container does far less at boot: the
generated initializer supplies what a reflective startup computes from
classpath scanning. The switch's spelling is a jar fact in the strictest
sense this Module can offer: `org.springframework.aot.AotDetector` in
spring-core 7.0.9 declares `public static final String AOT_ENABLED`, and
its static initializer's `ldc` instruction loads exactly
`spring.aot.enabled` (read with `javap -c`). On the *wrong* jar - a jar
built without its AOT initializer - Boot 4.1.1 responds with a
diagnostic that names the spelling, not a stack trace:
`AotInitializerNotFoundFailureAnalyzer` extends
`AbstractFailureAnalyzer<AotInitializerNotFoundException>` and its
constant pool carries "Did you build the application with enabled AOT
processing?" and "If you want to run the application in regular mode,
remove the system property 'spring.aot.enabled'". The property that
gates the form and the failure that explains it agree on one spelling,
read off two different jars - the same rigor this course applies to its
own diagnostic tests.

The hints that *remain your problem* have one small API, and its
annotations are spread across exactly three packages (all read off the
7.0.9 jars): `RuntimeHints` and the hints model live in spring-core's
`org.springframework.aot.hint`, `@ImportRuntimeHints` lives in
spring-context's `org.springframework.context.annotation` (its `value()`
being `Class<? extends RuntimeHintsRegistrar>[]`), and the
`@Reflective`/`@RegisterReflection`/`@RegisterReflectionForBinding`
annotations live beside the hints model, in
`org.springframework.aot.hint.annotation`. The registrar form, compiled
against the 7.0.9 classpath with the exact text below:

```java
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(ReservationRulesRuntimeHints.class)
public class NativeConfiguration {

}
```

```java
class ReservationRulesRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection().registerType(ReservationRules.class,
                MemberCategory.INVOKE_DECLARED_METHODS);
    }
}
```

`ReservationRules` is the record holding the [Reservation](../GLOSSARY.md)'s
hold-limit - a [Member](../GLOSSARY.md)'s maximum outstanding holds on a
[Title](../GLOSSARY.md) they cannot borrow yet - and the registrar's one
override is its whole surface, consumed at build time by the AOT engine
and written into the `reflect-config.json` the packaged jar carries. The
annotation form is the one-line sibling, also a 7.0.9 read:

```java
@RegisterReflectionForBinding(ReservationRules.class)
```

on any configuration or component class: the annotation's processor
(`RegisterReflectionReflectiveProcessor`, read in the jar) turns a
*binding* declaration into reflection plus binding registration for the
same type. Choose the registrar for logic, the annotation for a
one-line registration of a binding - both compile into the same hint
files.

### The jar Boot 4 ships: same layout, fewer choices

The 4.1.1 jar's layout is the layout Boot 2 taught: nested jars, a
launcher named on the manifest, the application's classes one entry in.
What surrounds the layout moved, and the docs are now direct about the
preferred run: "running the application from an exploded structure is
faster and recommended in production" - the packaging page's own
sentence - reached with the current jarmode spelling:

```bash
java -Djarmode=tools -jar my-app.jar extract
java -jar my-app/my-app.jar
```

That jarmode is `tools`, the 3.3 rename - not the 2.7-era `layertools`
of the same command shape. We ran `java -Djarmode=tools -jar
lab-11-resilience-and-concurrency-solution.jar help` against a real
4.1.1-built artifact and got exactly
`extract`, `list-layers`, `help`: the verbs carried across the 3.5/4.1
line unchanged, so the command habit you have is current by both
*spelling* and *behaviour*. The default `extract` layout is what makes
the AOT cache work at all in Boot's version of it - the efficient
deployments page's sentence is exact: the default layout "is AOT cache
(and CDS) friendly" - because the cache's class-path requirements are
the extracted layout's `lib/` directory shape.

The removals are the delta table's story and they are one-way:

- the 4.1.1 repackage mojo has no `loaderImplementation`, and Central
  publishes no `spring-boot-loader-classic` at 4.1.1 - the *choice* the
  3.2-era param offered between the rewritten loader and the classic
  one is closed. `PropertiesLauncher` itself is not a removal: the
  4.1.1 `spring-boot-loader` jar lists
  `org.springframework.boot.loader.launch.PropertiesLauncher`, so the
  externalized layout survives; what is gone is the classic
  implementation regime behind it.
- the repackage mojo at 4.1.1 reads no `executable` and no
  `embeddedLaunchScript` param (the 2.7.18, 3.0.13, 3.4.13 and 3.5.16
  descriptors all do): the launch script is removed, and the jar
  administrable by an init system is no longer a feature of the jar.
- the `Spring-Boot-Jar-Type: development-tool` manifest entry is a 4.0
  *addition* (release notes): a jar so labelled is a development-only
  artifact, excluded for you when it ends up inside an uber jar's
  classpath - a jar-classification concept with no 3.x analog.

The goals set is worth one closing read: the 4.1.1 descriptor is
`repackage`, `run`, `start`, `stop`, `build-image`,
`build-image-no-fork`, `build-info`, `process-aot`, `process-test-aot`
- exactly the 3.5 set, except that `repackage`'s parameter rows shrunk
by the two removed names above.

### Native images: what the profile wires and what it costs

The wiring to re-read when a native build is in front of you is in the
**starter-parent**: the `native` and `nativeTest` profiles exist there
at 3.0.13, 3.5.16 and 4.1.1 (the 2.7.18 starter-parent mentions native
zero times), and at 4.1 they bind two things: the Boot plugin's
`process-aot` execution into the build, and the
`org.graalvm.buildtools:native-maven-plugin`, whose version the
published `spring-boot-dependencies` POM manages - the plugin property
`native-build-tools-plugin.version` reads **0.10.6** in 3.5.16 and
**1.1.8** in 4.1.1. The declaration a Boot 4.1.1 application carries:

```xml
<plugin>
  <groupId>org.graalvm.buildtools</groupId>
  <artifactId>native-maven-plugin</artifactId>
</plugin>
```

(the docs' own snippet, versionless because the starter-parent's managed
pin answers it). Build:

```bash
mvn -Pnative spring-boot:build-image        # container image via buildpacks
mvn -Pnative native:compile-no-fork         # bare native-image compile
```

And the prerequisite the docs pin as hard as the course pins JDK 25:
the GraalVM distribution itself, its how-to page's version is the floor,
and the quote is worth writing in full because the version number is the
change:

> To build a native image using the Native Build Tools, you'll need a
> GraalVM distribution on your machine. ... On Windows, follow these
> instructions to install either GraalVM or Liberica Native Image Kit
> in version 25, the Visual Studio Build Tools and the Windows SDK.

(the how-to page's prerequisite, with the version number explicit). For
macOS or Linux the same page installs `25.r25-nik` through SDKMAN!. The
compiler floor is JDK 25 - the same floor the build already has, so the
Java compiled is the Java the course resolves; what changed is *who
compiles*: the machine performing the native build needs an SDK that
carries `native-image`, which ships with a GraalVM distribution and not
with the standard JDK.

Now the section's honest paragraph: the build itself costs.

- **Time.** `native-image` performs whole-program analysis and
  compilation - the docs' phrasing is "statically analyzing your
  application code from its main entry point", unreachable code dropped
  - so where a JVM `package` of this codebase spends minutes, the
  native tooling adds a same-order block of work on top. The docs'
  sample is the counterpoint, and its own output: "Started MyApplication
  in 0.08 seconds (process running for 0.095)" - the ~0.08 the Module
  promised the reader is *bought per build*: compute spend becomes
  part of CI's packaging budget, where it never was.
- **Memory.** The system requirements' sentence is blunt - Docker
  memory of "at least 8GB" recommended on macOS, the WSL 2 backend on
  Windows - machine requirements, not suggestions, on top of what the
  mainstream build already wanted.
- **A ledger of commitment.** Everything the image can do is answered
  before the tool runs: conditions evaluated, profiles baked in (the
  `process-aot` section's configuration snippet above), reflection
  legitimate only where the hints recorded it. An environment-sized
  behavioural difference cannot exist in an artifact that fixed its
  environment at build time.
- **Reproducibility, as a release review.** The native artifact is as
  immutable as its build-time inputs - the same guarantee the AOT cache
  documents for its `.aot` file applies - a changed input *rebuilds*;
  there is no incremental half-state.

What you receive is the docs' sentence: "A GraalVM Native Image is a
complete, platform-specific executable. You do not need to ship a Java
Virtual Machine in order to run a native image", at the price of that
executable containing no lazy loading and none of the dynamicity the
JVM form defers to boot (the closed-world list quoted in the AOT
section above). The stops beyond that - profile-guided optimization,
advanced reachability, `-H:` flags - the sibling course owns; this
Module stops at the build the reader scripts, the artifact the reader
packages, and the diagnostics the Lab runs on it.

### The AOT cache: fast startup for the JVM you already have

The reader who does not ship a native image still has a 4.x-class
deployment option, and it is a JVM feature, not a GraalVM one. Framework
7's Integration chapter states it directly: "The ahead-of-time cache is
a JVM feature introduced in Java 24 via JEP 483"; JDK 25 sharpens it
(the same chapter: on Java 25 the cache stores "method profiling
information", so a production-shaped training run is worth a workflow
replay, not just a single boot). Boot 4.1's packaging page carries the
form, quoted:

> Spring Boot supports the AOT cache for Java 25 and above. ... To use
> the AOT cache feature, you should first perform a training run on
> your application in extracted form:

```bash
java -Djarmode=tools -jar my-app.jar extract --destination application
cd application
java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar my-app.jar
java -XX:AOTCache=app.aot -jar my-app.jar
```

The training run's exit-behaviour is Spring's own: with
`-Dspring.context.exit=onRefresh` the process ends itself once the
`ApplicationContext` has refreshed - Framework's docs sentence: at that
point "all non-lazy initialized singletons have been instantiated, and
`InitializingBean#afterPropertiesSet` callbacks have been invoked; but
the lifecycle has not started, and the `ContextRefreshedEvent` has not
yet been published" - the exact moment a cache that optimizes startup
should retain: the beans, not the running lifecycle. The flag's home is
a bytecode read of the exact class the docs' mechanism names:
`DefaultLifecycleProcessor` in spring-context 7.0.9 carries the
`spring.context.exit` string, and - the delta-table caveat - so does
6.1.14's and 6.2.19's, while 6.0.23's has none: the switch enters
Framework 6.1 (the Boot 3.2 line), predating Framework 7 by two lines.
The deployment-matrix trims are the page's own: the cache file is
usable "as long as the application is not updated and the same Java
version is used", and "You have to use the cache file with the
extracted form of the application, otherwise it has no effect" -
earlier-than-25 JVMs get the CDS section of the same page instead.

### Container images: same goals, moving defaults, no moving parts

Everything `spring-boot:build-image` owned at 2.3 (the mojo's "Since"
column) it still owns at 4.1, and the history a review misremembers
- the *default builder* - is one we can enumerate off the jars, one
release at a time:

| | 2.7.18 | 3.0.13 | 3.2.12 | 3.4.13 | 3.5.16 | 4.1.1 |
|---|---|---|---|---|---|---|
| default builder | `paketobuildpacks/builder-jammy-base:latest` | same | same | `paketobuildpacks/builder-jammy-java-tiny:latest` | `paketobuildpacks/builder-noble-java-tiny:latest` | same as 3.5 |

every cell read off the `spring-boot-buildpack-platform` bytecode of
that version (the `:latest`-tagged strings are the defaults' actual
constants; the untagged `jammy-*`/`noble-*` variants the 3.4+ jars
carried are the alternative builder names the docs enumerate). The
params follow the same pattern: `imageBuilder` (the builder override,
owner property `spring-boot.build-image.builder`) exists from 2.3 in all
six descriptors; `trustBuilder` and `imagePlatform` (builder
registry-trust and multi-arch platform pin) are 3.4 arrivals - absent
from the 3.0/3.2 descriptors, present from 3.4.13 through 4.1.1. So the
review question on container-image config is narrow and jar-checkable:
the builder default is whatever the *pinned Boot version's bytecode*
says, and the only params of this surface that have ever changed are
the two 3.4 arrivals - which carry unchanged into 4.1.1.

The two commands the *course's* reader wants, in the form the docs
teach (maven-plugin page: the starter-parent's `native` profile plus
the goal double):

```bash
mvn spring-boot:build-image          # JVM form: layered image, container-friendly
mvn -Pnative spring-boot:build-image # native form: AOT-processed, native-image built
```

The second is the Module's whole taxonomy in one line: the JVM form, the
native form - both container images, both produced by a `build-image`
mojo, and the difference between them is *whose packaging phase* did the
work first. The layer jar story ties the two sections together through
one file: the jar's `layers.idx`, the index the jarmode `list-layers`
verb reads out, is supported directly by the Paketo Spring Boot
buildpack - the cloud-native-buildpacks page's sentence, identical in
the 3.5 and 4.1 docs: "The Paketo Spring Boot buildpack supports the
layers.idx file, so any layer customization that is applied to it will
be reflected in the image created by the buildpacks" - so a jar-level
layer decision rides the layers index into the image without Dockerfile
work. The Lab's diagnostics insist on what this Module teaches: the
packaged forms are run and proven, not only each exploded one.

## The Lab

The Lab is the library application M11 left: the borrow commits
atomically, the [Copy](../GLOSSARY.md) guard is one statement, the
[Member](../GLOSSARY.md) is the principal, the probes answer, the
resilience move is written in. What the Lab extends it with is its
*deployment forms*: the application packaged (the plain jar and the
extracted form), the application with its AOT generation actually
processed (`process-aot` run at build time, the JVM run under
`spring.aot.enabled=true`), an AOT cache built against the extracted
form - and the diagnostic edge the Lab's tickets name exactly: the
*packaged* form is where the diagnostics run, so an application that
boots in the editor but not from its packaged entrance shows red, as
does a jar whose AOT switch never came on.

The [Reservation](../GLOSSARY.md) surface will inherit the shape: a
Reservation is a Member's hold on a [Title](../GLOSSARY.md) they cannot
borrow yet - and a deployment that keeps that wait cheap at cold start,
whether on the JVM's AOT cache or inside a native image, is the shape
the packaged-form diagnostics teach. The Lab's diagnostics do not test
the Reservation feature; the shape is the one it will use.

The requirement is stated by Diagnostic tests in the `diagnostics`
package - the tests that fail on the Start module (whose packaged forms
proceed only from their exploded state) and pass on the Solution
module, which is written from scratch rather than patched. The command
that states the requirement, the one the lab index entry teaches:

```bash
./mvnw -f labs -pl lab-12-native-images-and-packaging-start verify -Pdiagnostics
```

The pair is `lab-12-native-images-and-packaging-start` /
`lab-12-native-images-and-packaging-solution`. One known state of the
index at the time this Module was written: the lab index's row 12 still
reads `lab-12-capstone-*`, a stale entry from before this Lab's naming
was decided - the start/solution module names above are the ones this
Module's tickets author. Read the failure messages the way this course
reads every delta table row: each names the form that is missing - a
packaged run nobody proved, an AOT switch that never came on, a cache
nobody trained against the extracted form. The same tests pass
against `lab-12-native-images-and-packaging-solution` (and, in CI,
against every Solution module at once with
`./mvnw -f labs verify -Pdiagnostics-solutions`); read that module last,
to see how small the packaging story is once the artifact owns its
build-time decisions - then M13 is where every Module's surface deploys
as one.
