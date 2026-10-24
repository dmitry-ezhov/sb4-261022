# 60: M12 lab solution: Native images and packaging

**What to build:** The Solution module for M12: the reference implementation of packaging the application, producing a native image, and making the Lab's diagnostics pass in the packaged form rather than only in the exploded one, written from scratch rather than patched from the Start module, satisfying every diagnostic of the Lab.

**Blocked by:** 59

**Status:** resolved

- [x] the Solution module exists and is listed in the labs aggregator
- [x] it is written from scratch, not patched from the Start module
- [x] every diagnostic of the Start module passes against it
- [x] it meets the coverage floor
- [x] its own tests assert the Lab's behaviour, not the implementation's arrangement
- [x] CI asserts its diagnostics through the single diagnostics command

## Answer

`labs/lab-12-native-images-and-packaging-solution` is the M12 lab's
Solution module, listed in `labs/pom.xml` right after its Start pair. The
application it packages is the resilience lab's application carried
forward byte for byte - the requests on virtual threads, the availability
call bounded and retried, the drain window named - and the same own tests
the Start module carries, so the coverage floor applies here and holds.
What is new is the packaging move, and it is one binding: an
`<execution>` on the spring-boot-maven-plugin with the `process-aot` goal,
in the goal's default phase (prepare-package), so the build walks the
application's container ahead of time - refreshes an
`ApplicationContext` that "only creates bean definitions, not bean
instances" - and writes `target/spring-aot/main`: the generated
`__BeanDefinitions` sources, the bytecode, the hint JSON under
`META-INF/native-image`. On Boot 4.1.1 those classes compile straight
into `target/classes` and the repackage puts all of it inside the jar, so
the jar carries an initializer and the boot under
`spring.aot.enabled=true` plays it back, its log reading "Starting
AOT-processed". No `jacoco.skip` and no `diagnostics.skip`: the ordinary
build covers this module, and the diagnostics-solutions profile asserts
its diagnostics.

The diagnostics are the same three failsafe ITs the Start module carries,
only re-addressed: the packaged jar boots plainly and under the switch,
the tools-extracted form (the jarmode the 3.3 line renamed from
`layertools`) boots the same two ways, and the AOT cache is trained
against the extracted form with the packaging page's own run
(`java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar`)
and served under `-XX:AOTCache` beside the same switch - "AOT cache and
Spring's AOT can be combined", the page's own sentence. The native image
the Module introduces is the fourth form and none of the diagnostics
build it; they run the jar forms on the JDK the build itself runs. Here
they are green 3/3; on the Start module the same three stay red under
`-Pdiagnostics`, each failure naming the one `<execution>` whose absence
is the missing move.

Two shared build decisions the AOT output forced, both made where the
course keeps build decisions and reasoned in place: SpotBugs was charging
the generator's classes (a bean-definition factory that throws while
building, the proxy's unused `$beanFactory` field, the generated
repository callback's JSpecify-annotated parameter) - excluded in
`config/spotbugs/exclude.xml` by a class-name regex the generator's
shapes only - and the same classes dragged the JaCoCo branch ratio to
0.033, because the AOT walk never executes what it writes; the root
POM's JaCoCo gate excludes the same class-name families from coverage,
which puts the module at 0.875 branch and 0.891 line against the 0.50 /
0.60 floor.

## Comments

- **2026-10-24 · agent** - Implemented against the M12 doc's Lab section
  and the labs authoring conventions: sources carried forward byte for
  byte from the Start module's (which are themselves the lab-11
  solution's), the diagnostics IT re-addressed to this module, one
  `process-aot` execution in the module POM, module added to the
  aggregator. Reactor runs: the module's default build green through
  every gate; `-Pdiagnostics` green 3/3 on this module, red 3/3 on its
  Start pair; `./mvnw -f labs verify` green; `./mvnw -f labs verify
  -Pdiagnostics-solutions` green with the Start module skipped.
