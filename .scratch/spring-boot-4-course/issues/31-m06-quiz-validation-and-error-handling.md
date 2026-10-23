# 31: M06 quiz: Validation and error handling

**What to build:** The quiz for M06: five questions that test whether the reader understood what belongs in an input constraint rather than a domain rule, which layer maps the error, and why the two kinds of failure should not be merged, each with an answer that explains itself.

**Blocked by:** 30

**Status:** resolved

- [x] five questions with answers
- [x] every question has exactly one defensible answer
- [x] each answer explains why, not only what
- [x] the questions test understanding of the delta, not trivia
- [x] no question is answerable by guessing from the wording alone

## Answer

The quiz was drafted with the doc (ticket 28); this ticket graded it against
its criteria and rebuilt the two questions that failed the grade.

- **Grading jury (the ticket's named understandings):** the drafted quiz
  tested the delta table well but never asked the ticket's three
  understandings. *What belongs in an input constraint rather than a domain
  rule* - the Lab's built-around mistake - was untested: question 1 asked
  about the starter's modularisation and question 4 about the `javax`
  package, *both* on the same implementation row, while the rule-as-constraint
  mistake went without a question. *Which layer maps the error* was never
  asked as ownership. *Why the two kinds of failure should not be merged*
  lived only inside question 3's delete-the-handlers thought experiment.
- **Question 1 was also the guessability failure** the criteria name: an
  artifact-list question - "the POM has the API ... what is missing?" - whose
  answer (the implementation, from the starter) telegraphs from the wording,
  to a reader who skipped the module entirely.
- **Question 1 rebuilt** (rule as constraint, the no-row part of the table):
  a colleague "finishes the Lab early" by putting the
  [Loan limit](../../../GLOSSARY.md) on `LoanRequest` as a constraint; the answer
  names both independent reasons bean validation cannot express the rule - a
  constraint value must be a compile-time constant, and the limit is
  `library.loan.limit` configuration the loan feature reads off
  `LoanProperties` (so `@Max(3)` hard-codes a three the rule does not own);
  and the count the rule compares - the Member's other Active loans - is not
  on the record at all, no field to sit on. Claims checked against the
  Solution module: `LoanRequest` carries `@NotBlank memberId, @NotBlank
  copyId`, `LoanProperties` is `@ConfigurationProperties(prefix =
  "library.loan")` with `limit`, `LoanService` counts against
  `properties.limit()`.
- **Question 5 rebuilt** (layer ownership): a reviewer objects that
  `LoanService` already holds the refusal's facts, so let the service build
  the `ProblemDetail` itself. The answer gives what the move costs and what
  it undoes - rendering's vocabulary imported into the `loan` package, the
  status decided per throw site (`409` for the Loan limit, `404` for an
  unknown Title, per feature), the exception lost as non-HTTP callers'
  contract, rule tests needing the boundary, and the two failure kinds
  ceasing to be distinguishable at the boundary - the merge the Module argues
  against. Claims checked against the Solution module: the `ProblemDetail`
  import sits in the `api` package alone (`ApiExceptionHandler` - verified by
  grep on both lab halves), `TitleNotFoundException` is answered `404` and
  `LoanLimitExceededException` `409` by their handler methods.
- **Questions 2, 3 and 4 kept**: verified under ticket 28, one defensible
  answer each, none guessable from wording. Question 2 anchors the
  method-parameter row, question 3 the problem-details row (and carries the
  not-merged lesson through the switch's blindness to a domain exception),
  question 4 the implementation row's annotations half.
- The old `ErrorResponse` question was dropped to make room - the same trade
  ticket 26 made - matching its M05 precedent for dropping a redundant delta
  question.

## Comments

- **2026-10-23 · agent** — Graded the ticket-28 quiz against the
  acceptance criteria; two questions failed the jury (the rule-vs-constraint
  understanding untested, and question 1 guessable) and were rebuilt with
  claims checked off the resolved lab code. Doc edit is the resolution; no
  code changed.
