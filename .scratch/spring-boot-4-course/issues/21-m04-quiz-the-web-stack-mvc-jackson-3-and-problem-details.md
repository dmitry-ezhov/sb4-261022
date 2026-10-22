# 21: M04 quiz: The web stack: MVC, Jackson 3 and problem details

**What to build:** The quiz for M04: five questions that test whether the reader understood the media type of a problem detail, what changed in Jackson 3 beyond the package, and when the declarative HTTP client beats a hand-written client, each with an answer that explains itself.

**Blocked by:** 20

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

Built in commit `2e9dcc2` (together with the checklist, ticket 22).

Five questions on the web stack, each with an answer that explains why:

1. A JSON `null` for an `int` field is an error, because Jackson 3 turned
   `FAIL_ON_NULL_FOR_PRIMITIVES` on by default and Boot does not turn it
   off - and the point is that a value you did not send and a value you sent
   as `null` are now different mistakes.
2. An undeclared field in a request body is still accepted: Jackson 3
   flipped the default of `FAIL_ON_UNKNOWN_PROPERTIES`, but Boot disables
   the feature explicitly - which matters for anyone using Jackson outside a
   Boot application.
3. `ResponseEntity.notFound().build()` gives the client a category and no
   sentence; a problem detail gives it both.
4. The declarative client turns a 404 into a typed exception, so the error
   handling belongs to the framework and cannot be forgotten in a caller.
5. API versioning resolves through a strategy instead of a filter chain, and
   Boot 4 puts the version into the outbound request too.
