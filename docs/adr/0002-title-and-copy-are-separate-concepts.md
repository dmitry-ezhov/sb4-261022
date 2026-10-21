# A Title and a Copy are different things

The catalogue models a work (`Title`) separately from the physical items the
library lends (`Copy`). A Title has many Copies, and a Loan borrows exactly one
Copy.

The obvious model is a single `Book` entity. We rejected it because it hides the
rule that actually matters in a lending library - a work is lendable only while
at least one of its Copies is available - and because it makes "this title is out
of stock" impossible to express without a nullable field. Splitting the two
costs one extra entity and makes availability a question about Copies.
