# 11: M02 quiz: The Boot 4 module map and a multi-module build

**What to build:** The quiz for M02: five questions that test whether the reader understood which starter owns which responsibility, when a parent is an aggregator, and when a dependency belongs in the parent rather than the child, each with an answer that explains itself.

**Blocked by:** 10

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

Built in commit `cc9e758`.

Five questions on the build, each with an answer that explains why:

1. What an explicit version does to a managed one - the module keeps working,
   which is exactly the problem.
2. Parent versus aggregator: inheritance versus the module list, unrelated
   mechanisms, and one POM that is both.
3. Why the webmvc test slice left the generic test starter - it drags the
   whole web stack onto the test classpath, and the starter shows it.
4. Where the `repackage` goal comes from, and that plugin management runs
   nothing on its own.
5. What an imported BOM does not give you: not the dependencies, not the
   plugins, and not the properties. That last claim was checked with a
   throwaway project - after importing Boot's BOM, `${java.version}` still
   resolves to the JVM's version, and `${spring-framework.version}` does not
   resolve at all.
