# M00 — Welcome

This module is not part of the graded track. It gets you from "I have never
opened this repository" to "I have run a lab and understood why it was red".

## 1. Environment setup

You need a JDK 25. Everything else comes with the repository.

```bash
# With mise (recommended) - reads the repo's mise.toml automatically
cd <repo>
mise install          # installs JDK 25 and Maven 3.10.0

java -version         # must report 25
./mvnw -v             # must report 3.10.0
```

Without mise: install a JDK 25 (Temurin is fine), put `JAVA_HOME` on your path,
and ignore the Maven requirement — the wrapper downloads Maven 3.10.0 itself.

**Docker is optional.** Only the Testcontainers labs (M05, M07, M08) use it.
Every other lab runs on H2 and needs nothing but a JVM.

Check your setup by building the repository:

```bash
./mvnw -f labs verify
```

A green build means your toolchain is right. If it is red, read the error: the
message names the gate that failed.

## 2. How this repository is organised

```
pom.xml        the parent POM - it owns every build decision
labs/          the lab modules, one pair per module
modules/       the module docs you are reading
config/        shared PMD, Checkstyle and SpotBugs configuration
tasks/         the backlog this course was built from
docs/adr/      decisions worth remembering
GLOSSARY.md    the vocabulary this course uses
```

`labs/pom.xml` configures nothing. It exists only to list the lab modules. If
you want to change a plugin version, a dependency version or a quality gate, you
change the root `pom.xml` and every module follows.

## 3. How a lab works

Every lab is a pair of modules:

- the **start module** — the code you begin with. It compiles, it is formatted,
  it passes every static analysis rule and it passes its own tests. What it does
  *not* do is the thing the lab is about.
- the **solution module** — the reference implementation.

The behaviour the lab requires is written down as **diagnostic tests**. They live
in `src/test/java/.../diagnostics/` and are excluded from the default build, so
the start module stays green while its behaviour is still wrong:

```bash
./mvnw -f labs -pl lab-01-boot2-to-boot4-start verify -Pdiagnostics
```

That command is red on the start module and green on the solution module. Your
job is to make it green on the start module.

The workflow for every lab:

1. Read the module doc.
2. Run the diagnostics. Read the failure messages — they say what is missing.
3. Fix the start module.
4. Run the diagnostics again until they are green.
5. Run the plain `verify`. Every gate must pass, including the coverage floor.
6. Read the solution module and compare.

## 4. The quality gates

| Gate | What it means for you |
|---|---|
| Spotless (Palantir) | The formatting is decided. Do not argue with it — run `./mvnw -f labs spotless:apply`. |
| PMD | Rules that catch real defects. If one fires, understand it before suppressing it. |
| SpotBugs | Bytecode-level bug patterns. |
| Checkstyle | Naming, correctness, structure. Not formatting — Spotless owns that. |
| JaCoCo | 60% line and 50% branch coverage on solution modules. |
| Enforcer | JDK 25, Maven 3.9.11+. |

If you are mid-task and only want to compile, use the escape hatch:

```bash
./mvnw -f labs verify -Pquality-lenient
```

It turns the gates into warnings. Finish with a plain `verify` — a green lenient
build is not a green build.

## 5. The vocabulary

The course teaches a sample application: a library that lends physical items to
members. The words are fixed, and the important ones are:

- a **Title** is a work; a **Copy** is a physical item of a title
- a **Member** borrows; a **Loan** is an active borrowing of one copy
- a **Reservation** is a member's hold on a title they cannot borrow yet

The full list, including the business rules, is in
[`GLOSSARY.md`](../GLOSSARY.md). Use those words in your code, your test names
and your commit messages.

## 6. Suggested pacing

| Track | Modules | Time |
|---|---|---|
| A — The bridge | M01–M04 | ~10h |
| B — Data & testing | M05–M08 | ~12.5h |
| C — The platform | M09–M12 | ~13.5h |

Roughly 35 hours in total. Each module is written to be taken in one sitting of
two to four hours; the labs are where the time goes.
