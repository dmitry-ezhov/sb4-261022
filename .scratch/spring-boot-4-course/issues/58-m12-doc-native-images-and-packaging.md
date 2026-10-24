# 58: M12 doc: Native images and packaging

**What to build:** The Module doc for M12: what the reader knew in Boot 2, what changed in Boot 3 and Boot 4, and how it works now in the version this course builds against. It covers GraalVM native images in Boot 4, ahead-of-time processing, what breaks at build time rather than at run time, container images and layer jars, and what a native build costs, uses the glossary vocabulary, and points at the Lab that follows it.

**Blocked by:** 57

**Status:** resolved

- [x] the doc states the Boot 2 to Boot 3 to Boot 4 delta as a table, covering ahead-of-time processing, native images and packaging
- [x] every claim in the delta table is checked against the version in use, not remembered
- [x] the terms it introduces are the glossary's terms, and are linked
- [x] it names the Lab, and the command that runs the Lab's diagnostics
- [x] every snippet in it compiles against the version in use
- [x] it states what the reader is assumed to know and what they are not

## Answer

The Module doc (`modules/12-native-images-and-packaging.md`, through
"The Lab" as this ticket scoped it - the quiz and checklist are tickets
61 and 62) was written in the M10/M11 shape, with every delta cell read
off resolved or downloaded artifacts.

- **The eight rows split into three kinds of change, each verified its
  own way.** The *removals*: the 2.7.18, 3.0.13, 3.2.12, 3.4.13, 3.5.16
  and 4.1.1 `META-INF/maven/plugin.xml` descriptors were all read -
  `executable`/`embeddedLaunchScript` present at 2.7.18 and gone from
  4.1.1's repackage mojo, `loaderImplementation` arriving in 3.2 and
  gone at 4.1, `trustBuilder`/`imagePlatform` arriving in 3.4 and
  carrying into 4.1 - and the artifact level says the same thing:
  `spring-boot-loader-classic` exists at 3.5.16 and at neither 2.7.18
  nor 4.1.1, `spring-boot-jarmode-tools` exists from 3.3.13 and no
  `jarmode-tools` at 2.7.18 (the `layertools` artifact is 2.7's), and
  `spring-boot-loader`'s 4.1.1 jar still lists
  `org.springframework.boot.loader.launch.PropertiesLauncher` (66
  classes) - the *choice* was removed, not PropertiesLauncher. The
  *version jump* (source-invisible): `native-build-tools-plugin.version`
  reads **0.10.6** in 3.5.16's dependencies POM and **1.1.8** in
  4.1.1's, and the build-version prerequisite (quoted from the how-to
  page) is a GraalVM Native Image Kit **in version 25**. The
  *expansion*: AOT processing exists from 3.0 (`process-aot` and
  `process-test-aot` in the 3.0.13 descriptor, absent from 2.7.18),
  Framework
  7.0.9's `RuntimeHintsWriter`/`ReflectionHintsAttributes` replace
  6.2.19's four per-family writers and `LambdaHint` is new (the comm
  diff of both jars' `org/springframework/aot` packages), and the
  Boot 3.5 packaging doc's "Class Data Sharing" page is retitled "AOT
  Cache" in 4.1 (the 3.5 URL 302-redirects; the 4.1 page's own sentence
  is "Spring Boot supports the AOT cache for Java 25 and above"). The
  buildpack default builder was enumerated off one jar at a time:
  `spring-boot-buildpack-platform` bytecode reads `builder-jammy-base:
  latest` at 2.7.18/3.0.13/3.2.12, `builder-jammy-java-tiny:latest` at
  3.4.13, `builder-noble-java-tiny:latest` at 3.5.16 and 4.1.1.
- **The runtime switch is a jar fact, not a remembered one.**
  `AotDetector.AOT_ENABLED` in spring-core 7.0.9 is a constant whose
  static initializer's `ldc` instruction loads exactly
  `spring.aot.enabled`, and 4.1.1's `spring-boot` jar (846 classes)
  carries the string in exactly one class -
  `AotInitializerNotFoundFailureAnalyzer`, whose constant pool holds
  "Did you build the application with enabled AOT processing?" and "If
  you want to run the application in regular mode, remove the system
  property 'spring.aot.enabled'". The strictest switch spelling the
  bytecode can offer, plus the failure behaviour that names it.
- **Restrictions are quotes, and where they come from is named.**
  What the build reads once and freezes: the closed-world list
  (classpath fixed, no lazy class loading, `@Profile` limitations,
  "properties that change if a bean is created are not supported") from
  the native-image chapter; "the beans which are created while
  ahead-of-time processing at build time are then always created when
  running the application and can't be switched off" (Boot how-to) -
  note the process-aot refresh "only creates bean definitions, not
  bean instances" (Framework's AOT chapter, quoted); and the two
  conditions AOT cannot transform - instance suppliers and
  `registerSingleton` beans - verbatim from that chapter, with the
  `BeanDefinitionRegistry`/`ImportBeanDefinitionRegistrar` pattern it
  names instead.
- **The AOT cache story is triple-sourced on purpose.** Framework 7's
  "JVM AOT Cache" chapter (JEP 483, Java 24; JDK 25 keeps "method
  profiling information" in the cache), Boot 4.1's "AOT Cache" page
  (the training-run and run commands quoted exactly), and the bytecode
  read of the flag's home: `spring.context.exit` is carried in
  `DefaultLifecycleProcessor` of spring-context 7.0.9, 6.2.19 and
  6.1.14 but is absent from 6.0.23 - the switch enters Framework 6.1
  (the Boot 3.2 line), predating Framework 7 as the Boot 3 cell's
  caveat says.
- **The behaviour-only cells are labelled as doc reads.** What the docs
  cannot be bytecode-proven (what `process-aot` generates and where -
  the plugin page's `target/spring-aot/main/{sources,classes,resources}`
  defaults; the GraalVM version floor; the extracted layout being
  "AOT cache (and CDS) friendly"; the Paketo buildpack's `layers.idx`
  support, whose sentence is identical across the 3.5 and 4.1 pages)
  are cited inline where the doc leans on them, and the how-to page
  quote is quoted in full because the version number is the change.
- **Snippets.** The two Java snippets (`@ImportRuntimeHints` +
  `RuntimeHintsRegistrar`, and the standalone
  `@RegisterReflectionForBinding` declaration) were compiled with
  `javac` against spring-core/spring-context/spring-beans 7.0.9 -
  note that the `org.springframework.aot.hint.annotation` package
  (`Reflective`, `RegisterReflection`,
  `@RegisterReflectionForBinding`) lives in **spring-core**, not
  spring-context, which the doc states as a jar read; `ImportRuntimeHints`
  is in spring-context, and its `value()` signature is read off the
  class with `javap`. Every shell/XML/JSON snippet is a command or
  config taken from the verified artifacts or the 4.1 docs' own
  instructions (`java -Dspring.aot.enabled=true`,
  `-Djarmode=tools extract`, `-Pnative spring-boot:build-image`,
  `native:compile-no-fork`, the `process-aot` profiles execution from
  the how-to page).
- **Runtime observation as a ticket claim:** `java -Djarmode=tools -jar
  lab-11-resilience-and-concurrency-solution.jar help` was run against
  a real 4.1.1-built artifact: the verbs are exactly `extract`,
  `list-layers`, `help` - the 3.5/4.1 `tools` jarmode carried the verb
  set forward unchanged.
- **The Lab section** names
  `lab-12-native-images-and-packaging-start`/
  `-solution` (tickets 59/60's module names, matching the ticket slugs,
  with the Lab index's stale `lab-12-capstone-*` row 12 called out as a
  leftover), the diagnostics command
  `./mvnw -f labs -pl lab-12-native-images-and-packaging-start verify
  -Pdiagnostics`, and the `-Pdiagnostics-solutions` CI form; the
  glossary terms (`Title`, `Copy`, `Member`, `Loan`, `Reservation`)
  are used and linked to `../GLOSSARY.md`.

## Comments

- **2026-10-24 · agent** — The doc was written through "The Lab" only
  (the skeleton's Quiz/Checklist sections are tickets 61/62), with the
  evidence recorded in this Answer and left in the repo's build cache
  `/tmp/spring-boot-*` POM snapshots and the resolved jars for
  re-checking. A lab-index correction note was not needed in
  `labs/README.md` itself - that is the lab-authoring tickets' job.

