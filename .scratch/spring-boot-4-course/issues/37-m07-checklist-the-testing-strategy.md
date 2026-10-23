# 37: M07 checklist: The testing strategy

**What to build:** The checklist for M07: what a reviewer applies to a solution of this Lab to judge it production-shaped rather than merely working.

**Blocked by:** 36

**Status:** resolved

- [x] the checklist names observable properties, not implementation steps
- [x] every item is verifiable by reading the change or running a command
- [x] it covers the removed mock-bean annotation appears nowhere
- [x] it covers one test asserts one behaviour
- [x] it covers tests assert observable behaviour, not arrangement
- [x] it covers the slowest test in the suite is the one that earns it
- [x] no item restates a Quality gate that the build already enforces

## Answer

Built in this commit, on the checklist draft the doc carried from
ticket 33. The draft already met four of the criteria as properties: the
slice / full-context / container placement, the observable-outcome item
(observable behaviour, not arrangement), verifiability by reading or
command, and no restatement of a build gate. Three criteria were open and
each got its own answer:

- **The removed mock-bean annotation appears nowhere.** The draft's stubs
  item named the annotations that *do* carry the substitution - `@MockitoBean`,
  `@MockitoSpyBean` - and left the removed pair implicit. The item is now its
  own line: no test imports `org.springframework.boot.test.mock.mockito`,
  `@MockBean` and `@SpyBean` are not deprecated in 4.1.1 but gone from the
  jars, and the carriers are Framework's pair at
  `org.springframework.test.context.bean.override.mockito` - which is exactly
  what makes "appears nowhere" readable rather than a hope.
- **One test asserts one behaviour.** Nothing in the draft said a test makes
  a single claim. New item: the name states one outcome, the body backs
  exactly that one, so a red has one lie to name - and a method that joins
  two outcomes with an And is named as the shape a reviewer flags.
- **The slowest test in the suite is the one that earns it.** The draft's
  costs item counted the boots and required evidence, but stopped short of
  the rule itself. The item now ends with it: the suite's slowest class is
  the one whose claim nothing cheaper can make - a boundary status decided
  before any engine is consulted is not re-paid on the container's clock,
  which is the container-tier rule the quiz's answer states from the other
  side.
