# Library Lending Course

The Spring Boot 4 course and the sample application it is taught through: a
library that lends physical items to members. This glossary fixes the vocabulary
used in the module docs, the labs and the reference solutions.

## Library lending

**Title**:
A work the library holds, described independently of the physical items it lends.
_Avoid_: Book, Publication, Work

**Copy**:
A physical item of a Title that can be lent to a Member. A Title may have many
Copies, and each Copy is lent on its own.
_Avoid_: Volume, Item, Edition

**Member**:
A person who borrows from the library.
_Avoid_: Patron, Borrower, Customer, User

**Loan**:
An active borrowing of one Copy by one Member, with a due date.
_Avoid_: Borrowing, Checkout, Rental

**Reservation**:
A Member's hold on a Title they cannot borrow yet. A Member may hold at most one
Reservation per Title.
_Avoid_: Hold, Request, Backorder

**Loan period**:
The number of days a Loan stays open before it falls due. Fourteen days.
_Avoid_: Loan duration, Term

**Active loan**:
A Loan whose Copy has not been returned yet.
_Avoid_: Current loan, Open loan

**Overdue loan**:
An Active loan whose due date has passed. An overdue loan carries no fine in this
course.
_Avoid_: Late loan, Delinquent loan

**Due date**:
The date on which the Copy of a Loan must be back on the shelf: the day the Loan
started plus the loan period.
_Avoid_: Return date, Expiry

**Loan limit**:
The maximum number of Active loans a Member may hold at once. Three.
_Avoid_: Borrowing limit, Max loans

**Available copy**:
A Copy of a Title that is not currently on loan.
_Avoid_: Free copy, Free book

## Course delivery

**Module**:
One of the thirteen units of the course, each with a doc, a lab, a quiz and a
checklist.
_Avoid_: Lesson, Chapter, Section

**Track**:
A named group of Modules that build on each other.
_Avoid_: Part, Stage, Level

**Lab**:
The hands-on exercise attached to a Module, delivered as a pair of Maven modules:
a start module and a solution module.
_Avoid_: Exercise, Assignment, Homework

**Start module**:
The lab module holding the deliberately imperfect code the learner starts from. It
compiles, and its tests pass, but the behaviour the lab asks for is missing.
_Avoid_: Skeleton, Starter, Template

**Solution module**:
The lab module holding the reference implementation of the lab.
_Avoid_: Answer, Reference

**Diagnostic test**:
A test that states the behaviour the lab requires and therefore fails until the
learner implements it. Diagnostics are excluded from the default build so that
start modules stay green.
_Avoid_: Failing test, Broken test, Pending test

**Quality gate**:
An automated check that fails the build when the code does not meet it.
_Avoid_: Lint, Check, Rule
