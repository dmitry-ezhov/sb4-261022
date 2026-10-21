# 06: M01 quiz: Boot 2 to Boot 4: what changed

**What to build:** The quiz for M01: five questions that test whether the reader understood which namespaces moved, which package root Jackson uses now, which starter replaced which, and where the MockMvc annotation lives, each with an answer that explains itself.

**Blocked by:** 05

**Status:** resolved

- [ ] five questions with answers
- [ ] every question has exactly one defensible answer
- [ ] each answer explains why, not only what
- [ ] the questions test understanding of the delta, not trivia
- [ ] no question is answerable by guessing from the wording alone

## Answer

Built in commit `39969eb`.

Five questions on the delta, each with an answer that explains why:

1. The `javax` constraint that is inert rather than broken.
2. The Jackson annotations that did not move with the rest of Jackson 3.
3. The starter that was deprecated rather than removed - and the one that was
   renamed, which is why one is quiet and the other is loud.
4. The bean override that moved from Boot into the framework, and why the
   import does not resolve.
5. The self-invocation that `@Retryable` will not retry - the same rule as
   `@Transactional`.
