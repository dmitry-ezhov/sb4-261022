# 01: Module authoring template and shared test support

**What to build:** The structure a Module is authored in, so that authoring one is mechanical rather than a fresh design every time: a doc skeleton with its fixed sections, the quiz format, the checklist format, and a shared support module that publishes a fixed Clock as a test-jar for Labs that need deterministic time.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] the doc skeleton names every section a Module doc must have, in order
- [x] the quiz format states how many questions there are and what an answer looks like
- [x] the checklist format states what a reviewer applies it to
- [x] the shared support module publishes a fixed Clock as a test-jar, consumable by any Lab
- [x] a Module can be authored from the template without inventing a structure

## Answer

Built in commit `4944603`.

- **The authoring guide** (`docs/course/authoring.md`) fixes one structure for
  every Module: the doc skeleton with its seven sections in order, the rules
  for the Boot 2 to Boot 3 to Boot 4 delta table, the quiz format (five
  questions, answers that explain why, nothing answerable by guessing from the
  wording), the checklist format (observable properties only, nothing that
  restates a gate the build enforces), the Lab conventions for the Start and
  Solution modules, and the commands that prove a Module works.
- **The shared support module** (`labs-support`) publishes a deterministic
  clock as a test-jar. The fixture is `TestClocks.fixed()`: a clock frozen at
  a fixed instant in UTC. It lives in the test sources because a fixture may
  not mention the domain - every Lab carries its own copy of the domain
  classes - and is consumed with `<type>test-jar</type>`.
- **TDD**: the fixture's test was written first and failed to compile
  (`cannot find symbol: TestClocks`) before the fixture existed.
- **Consumability verified** against a throwaway consumer module outside the
  repository, which resolved `com.example:labs-support:0.0.1-SNAPSHOT:test-jar:test`
  and used the clock successfully.
- The full reactor build is green with every gate enabled.
