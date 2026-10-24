package com.example.library.loan;

/**
 * A Member at the Loan limit asked for a further Loan.
 *
 * <p>The domain's own words, sayable by the API: the request is perfectly
 * well formed - it names a real Member and a real Copy - and the library
 * refuses it anyway, because of the Active loans the Member already holds.
 * That is why the refusal answers {@code 409}, not {@code 400}: there is
 * nothing in the request to fix.
 */
public class LoanLimitExceededException extends RuntimeException {

    public LoanLimitExceededException(int limit) {
        // The wording is the glossary's: the rule is the Loan limit, a limit
        // is a number of Active loans, and "Active loan" is what the library
        // holds - never "checkout", never a beige "max items exceeded".
        super("A member may hold " + limit + " Active loans at once");
    }
}
