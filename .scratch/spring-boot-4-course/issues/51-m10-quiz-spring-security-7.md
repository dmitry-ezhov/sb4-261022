# 51: M10 quiz: Spring Security 7

**What to build:** The quiz for M10: five questions that test whether the reader understood filter chain against method security, what Boot 4 changed about the CSRF defaults, and how credentials are stored and compared, each with an answer that explains itself.

**Blocked by:** 50

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

The quiz was drafted with the doc (ticket 48); this ticket graded it against
its criteria and rebuilt the one question that failed the grade.

- **Grading jury (the ticket's named understandings):** the drafted quiz
  covered *what Boot 4 changed about the CSRF defaults* (question 2, the
  right-symptom-wrong-release note whose answer is "Boot 4 changed none of it
  - the `POST` endpoints needed the token before the upgrade too") and *how
  credentials are stored and compared* (question 4, the `{noop}` prefix as the
  delegating encoder's instruction rather than a comment, Boot's
  `NOOP_PASSWORD_PREFIX` prepend, and the two-line production shape). The
  named understanding *filter chain against method security* went untested as
  the contrast it names: question 1 tested the chain's two-release rewrite and
  question 3 tested the expression, each layer in isolation, and no question
  forced the split the doc's chain section argues for - "the URL rule answers
  *may this principal call the lending API at all*, the expression answers
  *whose Loan is this* ... Drop either one and a different caller gets
  through."
- **Question 3 was also the wording failure** the criteria name: it told the
  reader the verdict it asked them to judge ("say why the reviewer's
  conclusion is still the wrong one") - the telegraph tickets 26, 41 and 46
  removed from their drafts - so an answer beginning "the reviewer is wrong
  because" was available to a reader who had never read the Module.
- **Question 3 rebuilt** (the layers' division of labour, verdict neutral):
  the scenario reviews the solution module itself, so both propositions are
  half-true and the reader must find what each loss costs. A colleague
  proposes dropping the `hasRole("MEMBER")` rule for `/api/loans/**` ("the
  expression already refuses a caller who is not the
  [Member](../../../GLOSSARY.md) the borrow acts on") and dropping
  `@PreAuthorize` ("the
  controller passes `authentication.getName()`, so the expression compares
  the argument with itself - it checks nothing"). One defensible answer, two
  mechanisms, all the code's own claims: the rule's `403` is answered before
  the use case runs (the diagnostics assert exactly that), and method security
  reads arguments, not authorities, so the role-less authenticated principal
  walks past `anyRequest().authenticated()` with the comparison true by
  construction - the borrow lands instead of the `403`; and the annotation's
  drop is invisible on the happy path - it exists for the path that ever
  supplies a different id, without it the safety is one controller's
  discipline. The annotation half rides with the scenario's third turn: the
  colleague "keeps the check live" with `@EnableGlobalMethodSecurity`, which
  still compiles in the 7.1.1 jar - whose `prePostEnabled` defaults to
  **false**, so the expression survives in the source and runs nothing,
  against `@EnableMethodSecurity`'s default of true.
- **Questions 1, 2, 4 and 5 kept**: one defensible answer each, none
  guessable from wording - question 1 pins the two-release break (6.0's
  class, 7.0's deletions, the smallest 7.1.1 rewrite), question 2 the release
  attribution the colleague's note gets wrong, question 4 the
  prefix-selects-the-comparison mechanics and Boot's prepend, question 5 the
  token-versus-session argument with the 4.1.1 spellings. Claims in the
  rebuilt question were checked against the resolved solution module -
  `SecurityConfiguration`'s `/api/loans/**` rule and
  `anyRequest().authenticated()`, `LoanService.borrow`'s `@PreAuthorize` next
  to the controller's `authentication.getName()` pass,
  `MethodSecurityConfiguration`'s `@EnableMethodSecurity` - not written from
  memory.

## Comments

- **2026-10-24 · agent** — Graded the ticket-48 quiz against the acceptance
  criteria; question 3 failed the jury (the chain-against-method-security
  split untested, and the question stated the verdict it asked the reader to
  judge) and was rebuilt with claims checked off the resolved lab-10
  solution. Doc edit is the resolution; no code changed.
- **2026-10-24 · agent** — Review fix: the colleague's first proposition
  claimed the expression refuses "a caller who is not the Member named in
  the request", but the reviewed solution's `LoanRequest` deliberately
  names no Member - the premise described the pre-upgrade request shape,
  not the module under review; reworded to "the Member the borrow acts
  on", and the resolve pointer landed in `map.md`.
