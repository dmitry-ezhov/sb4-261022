# Authoring a Module

Every Module in this course is delivered the same way. This page is the
contract: the structure a Module doc has, the format a quiz has, the format a
checklist has, and the commands that prove a Module works.

## The Module doc skeleton

A Module doc has these sections, in this order. Do not add sections; do not
reorder them. A reader who has read one Module can navigate any other.

```markdown
# M<NN> - <Title>

<One paragraph: what this Module is about, and what the reader will be able to
do at the end of it.>

## What you knew

<The concept as it existed in Spring Boot 2, in one or two paragraphs. The
reader has this in their head; name it so they recognise it.>

## What changed

<A table with one row per change:>

| | Spring Boot 2 | Spring Boot 3 | Spring Boot 4 |
|---|---|---|---|
| <the thing that changed> | <the Boot 2 form> | <the Boot 3 form> | <the Boot 4 form> |

## How it works now

<The concept at the level a senior developer needs to reason about it - what
the framework does, what it costs, and when to reach for it. Not a tutorial
for a first-timer; the reader shipped Boot 2.>

## The Lab

<What the learner has to fix, in prose. The Diagnostic tests state the
requirement precisely; this section says what the Lab is about.>

```bash
./mvnw -f labs -pl lab-<NN>-<slug>-start verify -Pdiagnostics
```

## Quiz

<Five questions with answers. See "The quiz format" below.>

## Checklist

<What a reviewer applies to a solution of this Lab. See "The checklist
format" below.>
```

### Rules for the delta table

- One row per change. A row is a thing that is *different*, not a thing that is
  *new*.
- Every cell is checked against the version the course builds against. Not
  remembered - checked. The build's version is recorded in the Lab index.
- The Boot 4 cell is the one the learner will type. The Boot 2 cell is the one
  they already have in their head.
- If a thing did not change between Boot 2 and Boot 4, it does not get a row.

### Rules for "How it works now"

- Explain what the framework does, not how to type it. The reader shipped Boot 2;
  they can read an API reference.
- Say what it costs: a new dependency, a new concept, a behaviour they will
  have to change in their own code.
- Use the glossary's words for the domain. `Title`, `Copy`, `Member`, `Loan`,
  `Reservation`. Never a synonym.

## The quiz format

- **Five questions.** Not four, not six.
- Each question asks about one change from the delta table, and says which one
  it is about.
- Each answer explains *why*, not only *what*. "Because the starter was
  modularised in Boot 4 and the test slice moved into its own artifact" - not
  "the webmvc test starter".
- No question is answerable by guessing from the wording alone. If you can
  guess it from the shape of the question, rewrite the question.
- The quiz lives in the Module doc, under the `## Quiz` heading, as a numbered
  list of questions with the answer under each.

## The checklist format

A checklist is what a reviewer applies to a solution of this Lab to judge it
production-shaped rather than merely working.

- Every item is an **observable property**, not an implementation step. "Errors
  are problem details" is observable. "Create a class called ProblemHandler" is
  a step.
- Every item is **verifiable** by reading the change or running a command.
- No item restates a Quality gate the build already enforces. The build
  enforces formatting, the static analysis rules, the coverage floor and the
  environment guard; the checklist does not repeat them.
- Four items are always present: the change itself, the domain vocabulary, the
  failure behaviour, and the thing the Lab is actually about. The Module's own
  checklist decides what those four are.

## The Lab

A Lab is two Maven modules:

- `lab-<NN>-<slug>-start` - what the learner begins with. Green build, wrong
  behaviour.
- `lab-<NN>-<slug>-solution` - the reference implementation, written from
  scratch.

Both are listed in the Lab aggregator. A Lab module is added to the aggregator
by the ticket that authors it, and never pre-created.

### The Start module

- Compiles, is formatted, passes every Quality gate except the coverage floor,
  and passes its own tests.
- Its defect is behavioural. The requirement is stated by Diagnostic tests in
  `src/test/java/.../diagnostics/`, excluded from the default test run.
- Each Diagnostic failure message names what is missing. "No value at JSON path
  `$[0].name`" tells the learner what to build; "expected true but was false"
  does not.

### The Solution module

- Written from scratch, not patched from the Start module.
- Satisfies every Diagnostic of the Start module.
- Meets the coverage floor.
- Its own tests assert the Lab's behaviour, not the implementation's
  arrangement.

## Commands

```bash
# Build everything with every gate enabled.
./mvnw -f labs verify

# Run the Diagnostic tests - red on a Start module, green on a Solution module.
./mvnw -f labs -pl lab-<NN>-<slug>-start verify -Pdiagnostics

# Run the Diagnostic tests of every Solution module, and no Start module.
./mvnw -f labs verify -Pdiagnostics-solutions

# Gates as warnings, for when you are mid-task.
./mvnw -f labs verify -Pquality-lenient

# Format the code.
./mvnw -f labs spotless:apply
```

## Shared test support

`labs-support` publishes a **test-jar** with the fixtures that are not about the
domain - because every Lab has its own copy of the domain classes, a fixture
cannot mention them.

| Fixture | Use it for |
|---|---|
| `TestClocks.fixed()` | A deterministic "now", for Labs about due dates, loan periods and anything else that reads the clock. |

Consume it from a Lab with:

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>labs-support</artifactId>
  <version>${project.version}</version>
  <type>test-jar</type>
  <scope>test</scope>
</dependency>
```

The version is `${project.version}` because every module in the reactor shares
it - the Lab aggregator deliberately configures nothing, so there is no
dependency management entry to inherit.
