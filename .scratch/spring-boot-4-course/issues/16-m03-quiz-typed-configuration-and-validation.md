# 16: M03 quiz: Typed configuration and validation

**What to build:** The quiz for M03: five questions that test whether the reader understood how typed binding differs from a raw value, when validation runs relative to startup, and where an invalid configuration surfaces, each with an answer that explains itself.

**Blocked by:** 15

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

Built in commit `19278a9` (together with the checklist, ticket 17).

Five questions on configuration, each with an answer that explains why:

1. Which binding style Boot picks for a class with one constructor, and when
   you still need `@ConstructorBinding`.
2. A missing `Duration` and a missing `int` are different failures - and for
   an `int`, "not configured" and "configured as zero" are the same value.
3. The constraints are Jakarta's; the implementation is Hibernate Validator,
   and without it the annotations are decoration.
4. A bare number in a `Duration` is a number of milliseconds. **The doc said
   otherwise and was corrected**: the source of `DurationStyle` shows that
   the shorthand pattern allows an empty unit suffix and that the default
   unit is the millisecond, so `120000` is two minutes rather than an
   error.
5. `@DefaultValue` applies only when the property is not found, and its
   value is a constant rather than a placeholder.
