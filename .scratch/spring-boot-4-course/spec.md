Status: ready-for-agent

# Spring Boot 4 course — foundations and the Boot 2 → 4 bridge

## Problem Statement

A senior Java developer who shipped Spring Boot 2 applications a few years ago
cannot productively work on Spring Boot 4 today. The gap is not one API: it is
three majors of drift at once. `javax.*` became `jakarta.*`. Jackson 2 became
Jackson 3, on a different package root. The starters were modularised, one of
them was deprecated in favour of a new name, and the test slice moved to its own
starter. Spring Boot 3 never reached this developer, so the Spring Framework 6
and 7 changes - HTTP Service clients, declarative resilience, built-in API
versioning, the new observability stack - are simply unknown territory.

What exists today is scattered: release notes, migration guides, blog posts,
and other developers' war stories. There is no single path that takes a
developer from "my Spring Boot knowledge stopped at 2.x" to "I can build, test
and ship a Spring Boot 4 application, and I know what changed under me".

What such a path needs, and what no blog post provides, is a course that is
exercised rather than read. A developer must be able to take a deliberately
imperfect application, see it fail a precise, named requirement, fix it, and
watch the same requirement go green - while the whole thing runs under a build
that refuses to let ugly code through.

The developer also has to be able to trust the repository they are learning
from. If the sample code is unformatted, if the static analysis is noise, if
the coverage floor is aspirational, then every hour spent fighting the tooling
is an hour not spent on Boot 4.

## Solution

A self-paced text course with code labs, delivered as one Maven multi-module
repository, taught through a single sample application: a library that lends
physical items to members.

The course has twelve Modules in three Tracks. Track A is the bridge - what
changed between Boot 2, Boot 3 and Boot 4, the module map, typed configuration,
the web stack with Jackson 3 and RFC 9457 problem details. Track B is data and
testing - persistence, validation, the testing strategy, transactions and
consistency. Track C is the platform - observability, Spring Security 7,
resilience and virtual threads, native images and production.

Every Module is delivered the same way: a doc that states what the developer
knew, what changed, and how it works now; a Lab delivered as a pair of Maven
modules; a quiz; and a checklist. A Lab's Start module compiles, is formatted,
passes every Quality gate and passes its own tests, but does not yet do what
the Lab asks for. The missing behaviour is stated as Diagnostic tests, which
the default build excludes so that the Start module stays green. The Solution
module is the reference implementation, written from scratch.

Everything runs on one build whose configuration lives in a single place. The
root parent POM owns every dependency version, plugin version and Quality gate;
child modules inherit them and declare only their own dependencies. The gates
are Spotless with Palantir Java Format, PMD, SpotBugs, Checkstyle, a JaCoCo
coverage floor and a Maven Enforcer, with Sonar available behind a profile.
They fail the build by default and can be softened to warnings by an explicit,
named profile when a learner is mid-task.

The domain is fixed and small: a Title is a work, a Copy is a physical item of a
Title, a Member borrows, a Loan is an active borrowing of one Copy, and a
Reservation is a Member's hold on a Title. The rules are fourteen days per
Loan, three Active loans per Member, no Reservation for a Title the Member
already holds a Loan for, and no monetary penalty for an Overdue loan. This
vocabulary is written down once and used everywhere - in code, in test names,
in the docs and in the commits.

## User Stories

1. As a senior Java developer who last used Spring Boot 2, I want a first module that spells out exactly what changed between Boot 2, Boot 3 and Boot 4, so that I can rebuild my mental model instead of guessing.
2. As a senior Java developer who last used Spring Boot 2, I want to see the Jakarta namespace migration explained side by side with the code, so that I stop treating `jakarta.*` as noise.
3. As a senior Java developer who last used Spring Boot 2, I want to understand the Jackson 2 to Jackson 3 move, including the package root change, so that I know when I am looking at a third-party library that still speaks Jackson 2.
4. As a senior Java developer who last used Spring Boot 2, I want to know which starter names changed and which were deprecated, so that I can fix a POM without guessing.
5. As a senior Java developer who last used Spring Boot 2, I want to learn where the webmvc test slice moved in Boot 4, so that my slice tests compile on the first attempt.
6. As a senior Java developer who last used Spring Boot 2, I want to understand the Spring Framework 6 and 7 changes that reach into everyday code, so that the framework no longer feels like a black box.
7. As a learner, I want each Module to be self-contained and to take two to four hours, so that I can fit a Module into an evening.
8. As a learner, I want the Modules to build on one another in order, so that the application I am changing is the same application I changed last week.
9. As a learner, I want every Module to end in a Lab, so that I practise the concept instead of only recognising it.
10. As a learner, I want each Lab to start from code that compiles and passes its own tests, so that a red build always means "the Lab is not finished yet" and never "the repository is broken".
11. As a learner, I want the Lab's missing behaviour stated as failing Diagnostic tests with explicit messages, so that I know what to implement without reading the solution.
12. As a learner, I want to run the Diagnostic tests with one command, so that the feedback loop is short.
13. As a learner, I want the Solution module written from scratch rather than patched from the Start module, so that I read a real implementation instead of a diff.
14. As a learner, I want a quiz at the end of every Module, so that I find out what I absorbed before moving on.
15. As a learner, I want a checklist at the end of every Module, so that I can tell whether my solution is production-shaped or merely working.
16. As a learner, I want the build to enforce formatting automatically, so that I never spend review time on whitespace.
17. As a learner, I want static analysis rules that catch real defects without drowning in false positives, so that a violation is worth my attention.
18. As a learner, I want the coverage floor enforced, so that "it compiles" never quietly becomes "it is tested".
19. As a learner working mid-task, I want one named command that turns the gates into warnings, so that I can keep compiling while I think.
20. As a learner, I want the escape hatch to be visible in the command line rather than hidden in configuration, so that I never mistake a lenient green build for a real one.
21. As a learner, I want the build to fail with an actionable message when my JDK is too old, so that I am not left decoding a compiler error.
22. As a learner, I want the same Maven version on every machine, so that "works on my machine" stops being a category of bug.
23. As a learner without Docker, I want every lab to run on an in-memory database, so that I can work on the course anywhere.
24. As a learner with Docker, I want the persistence, testing and transaction labs to offer PostgreSQL through Testcontainers, so that I see real dialect and locking behaviour where it matters.
25. As a learner, I want one sample application with a small, fixed vocabulary - Title, Copy, Member, Loan, Reservation - so that every example builds on the last instead of starting over.
26. As a learner, I want the domain rules stated once and enforced in code - fourteen days, three Active loans, no self-Reservation - so that I learn where a business rule belongs.
27. As a learner, I want packages organised by feature rather than by layer, so that the code for one capability lives together.
28. As a learner, I want the code to use JSpecify null-safety annotations throughout, so that I absorb the Boot 4 null-safety practice without it being a separate lecture.
29. As a learner, I want the course to show Boot 4's own answer to a problem I solved by hand in Boot 2 - configuration binding, HTTP clients, resilience - so that I do not rebuild what the framework now does.
30. As a learner, I want the observability, security, resilience and production Modules to exist in this course, so that the whole path from first commit to production is covered.
31. As a course maintainer, I want every build decision to live in one parent POM, so that a version bump or a rule change reaches every Module at once.
32. As a course maintainer, I want a Lab module to be added only when it is authored, so that the repository never contains an empty placeholder Module.
33. As a course maintainer, I want the static analysis configuration curated and shared, so that every Module is held to the same standard and the exclusions are justified in one place.
34. As a course maintainer, I want each decision that is hard to reverse, surprising and contested to be recorded with its reasoning, so that a future reader does not undo it by accident.
35. As a course maintainer, I want the vocabulary of the course written down in one glossary, so that the docs, the code and the commit messages stop drifting apart.
36. As a course maintainer, I want CI to run the same commands a learner runs, so that a green repository and a green local build mean the same thing.
37. As a course maintainer, I want CI to assert that the Diagnostics of every Solution module pass, so that a broken reference implementation cannot ship.
38. As a course maintainer, I want the verified versions recorded with the date they were checked, so that "latest" never quietly means "unverified".
39. As a course maintainer, I want the work backlog kept as one ticket per unit of work, so that the next author knows exactly what is left.
40. As a learner who later joins a team on Boot 4, I want the habits this course builds - one parent POM, curated gates, feature-first packages, a shared glossary - so that they transfer to my day job.

## Implementation Decisions

**Repository and build shape**

- The root POM is both the parent and the aggregator of the whole repository. It owns every dependency version, plugin version and Quality gate.
- A second aggregator exists for the Lab modules. It configures nothing of its own; it only lists the Modules.
- Every Lab Module declares only its own dependencies and its own application class. No child repeats a version.
- The build is a two-level hierarchy: parent, then Labs. A third level is deliberately avoided.
- A Maven wrapper pins the Maven version; the JDK requirement is checked by the Enforcer.

**Toolchain**

- Spring Boot 4.1.1 - the latest stable release. Milestone releases are never used.
- Spring Framework 7.0.9 and Jackson 3.1.5 as managed by Boot.
- JDK 25 as the compilation target and the language level. The Enforcer requires a JDK 25 or newer and Maven 3.9.11 or newer, because the PMD plugin requires it.
- Maven 3.10.0, pinned by the wrapper.

**Dependency policy**

- The parent manages a catalogue of versions: the Boot starters, a Testcontainers BOM, WireMock, ArchUnit, REST Assured and MapStruct. The Testcontainers BOM is imported explicitly so the choice is visible and overridable in one place.
- The parent inherits exactly two dependencies to every child: the Boot test starter in test scope, and JSpecify in compile scope.
- WireMock is the standalone artifact, not the plain one. The plain jar mixes two Jetty majors, ships no server implementation, and breaks unrelated auto-configuration on the test classpath.
- WireMock stays test-scoped, and lab code never serializes application types with WireMock's mapper, because WireMock 3 speaks Jackson 2 while Boot 4 defaults to Jackson 3.

**Quality gates**

- Every gate is configured once, in the parent, and activated there so that every child inherits it. A child overrides a gate by setting a property, never by reconfiguring the plugin.
- Configuration that the plugin only accepts on one of its goals is bound to that goal, so no goal is ever handed a parameter it does not know.
- Where a plugin exposes its settings as user properties, the properties are used instead of inline configuration.
- The shared static-analysis configuration lives in one directory that every Module resolves identically, whether the build is started from the repository root, from the Labs directory, or from inside a Module.
- The rulesets are curated, not stock. Each exclusion carries a comment saying why it is excluded. An exclusion that matches no rule in the PMD version in use is treated as a defect in the ruleset.
- The gates fail the build by default. A single named profile softens all of them to warnings; it changes nothing else.
- The coverage floor applies to Solution modules. Start modules ship fewer tests by design and opt out of that one gate - every other gate still applies to them.
- Sonar runs only under its own profile. The host, the organisation and the token are supplied on the command line, so the same configuration works against SonarQube Cloud and against a self-hosted server.
- The build produces no plugin-parameter warnings. A warning here would teach the learner to ignore warnings.

**Course shape**

- Twelve Modules in three Tracks: the bridge (M01 to M04), data and testing (M05 to M08), the platform (M09 to M12).
- Each Module delivers a doc, a Lab, a quiz and a checklist. The doc states what the reader knew, what changed, and how it works now.
- Each Lab is a pair of Maven Modules: a Start module and a Solution module.
- A Start module is green: it compiles, is formatted, passes every gate except the coverage floor, and passes its own tests. Its defects are behavioural.
- The behavioural requirement is expressed as Diagnostic tests, kept in a diagnostics package, excluded from the default test run and executed by a dedicated profile.
- A Solution module is written from scratch, not patched from the Start module.
- The Diagnostic tests of a Start module are red until the learner implements the behaviour; the Diagnostic tests of a Solution module are green. CI asserts the second half.
- The course teaches through one application across all Modules. The application grows; it is not rebuilt per Module.

**Domain model**

- A Title is a work; a Copy is a physical item of a Title; a Member borrows; a Loan is an active borrowing of exactly one Copy; a Reservation is a Member's hold on a Title they cannot borrow yet.
- A Title may have many Copies. The split exists so that availability is a question about Copies rather than a nullable field on a Book.
- The rules are fixed: a Loan period of fourteen days, a limit of three Active loans per Member, no Reservation for a Title the Member already holds a Loan for, and no monetary penalty for an Overdue loan.
- The vocabulary is written down in one glossary, covering both the course delivery terms and the application domain terms. It states what each term is and which synonyms to avoid.
- Packages are organised by feature: one package per capability, holding that capability's controller, service and repository together.

**Boot 4 specifics the course relies on**

- The web stack uses the modularised starter, and the test slice uses its own starter. The Boot 4 test starter does not include the webmvc test slice.
- Errors are RFC 9457 problem details served with the problem media type, produced by a dedicated exception handler.
- Payload types are records, not untyped maps.
- The code uses JSpecify null-safety annotations throughout.

**Data strategy**

- The in-memory database is the default everywhere, so that no Docker daemon is required.
- The persistence, testing and transaction Labs additionally offer PostgreSQL through Testcontainers, and those Labs say explicitly where the two differ.

**Documentation and decision records**

- Decisions that are hard to reverse, surprising without context and the result of a real trade-off are recorded as decision records, one paragraph each, in a dedicated directory.
- The work backlog is kept as one ticket per unit of work, each naming its deliverables, its acceptance criteria and how to verify it.
- The verified versions of the whole stack are recorded in one table with the date they were checked.

## Testing Decisions

**What makes a good test here**

A good test asserts behaviour that a user of the application can observe: the
status code, the media type, the shape of the payload, the content of a problem
detail. It does not assert how the code is arranged. A Lab's Diagnostic test
states one requirement and names it in the failure message, so a failing
Diagnostic tells the learner what to implement rather than merely that
something is wrong.

**The seams**

Two seams, both at the highest level the repository has. No new testing
infrastructure is introduced.

- **Seam 1 - the reactor build.** A single command builds every Module, runs
  every Lab's own tests and applies every Quality gate, including the coverage
  floor. It is the seam that answers "is this repository shippable?". It is
  also the command a learner runs, so the learner and the build agree by
  construction.
- **Seam 2 - the diagnostics profile.** A single command runs only the
  Diagnostic tests. It is the seam that answers "does this Lab state a precise,
  achievable requirement, and does the Solution satisfy it?". The same command
  is red on a Start module and green on a Solution module - that asymmetry is
  the contract.

There is deliberately no third seam. An HTTP-level seam against a running
application would duplicate what the Lab's own tests already assert at the
MockMvc boundary, and would add a port, a client and a lifecycle to every Lab.

**What is tested**

- Every Lab Module, in both halves. The Start module is tested to prove it stays
  green; the Solution module is tested to prove it satisfies the Lab.
- Every Diagnostic test is tested twice - red on the Start module, green on the
  Solution module.
- The Quality gates are tested by making the build fail on a deliberate
  violation, and by confirming the lenient profile softens it.
- The environment guard is tested by building on a JDK older than 25 and
  confirming the message names the requirement.

**Prior art**

The lab 01 pair is the reference implementation of every convention: a Start
module whose behaviour is wrong but whose build is green, Diagnostic tests that
fail with messages naming what is missing, and a Solution module that passes
them. The exclusion of the diagnostics package from the default test run, and
the profile that runs only those tests, are established there.

## Out of Scope

- The content of the individual Modules beyond the framework described here. Each Module's subject matter, its examples and its quizzes are tracked as their own tickets in the work backlog, and are authored against this specification.
- The robustness, fault tolerance, resilience and security depth that the sibling course covers. This course teaches the mechanism and the Boot 4 API; the sibling teaches depth. The Modules that touch those areas say so and link across.
- Production deployment infrastructure, container images, and orchestration. The production Module covers packaging and native images, not running the service anywhere.
- A published website, a book, or any print artefact. The course is the repository.
- Gradle. The course is Maven only; a second build tool would double the maintenance of every quality gate for no teaching value.
- Multi-module splitting of the sample application itself. The application stays one module per Lab, so that a Lab can be read in one sitting.

## Further Notes

- The quality of the tool configuration is part of the curriculum. A learner
  should be able to read the parent POM and understand why each gate exists and
  why each rule is excluded - that is why the exclusions are commented rather
  than silently absent.
- The first Module's Lab is deliberately small. It exists to prove the
  mechanic, not to teach much; the teaching starts at the Jakarta migration and
  the starter renames.
- Two facts surfaced while building this and are worth stating plainly, because
  both contradict common advice: the deprecated web starter still exists in Boot
  4 rather than having been removed, and the Palantir formatter needs no extra
  JVM flags on JDK 25.
- Every version in the build was chosen against a stated requirement - a JDK
  support floor, a language level - rather than by taking whatever was newest.
  Where a newer version was available but not needed, the older verified one
  was kept.
