package com.example.library.loan;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

/**
 * The Loan feature's rules, where borrowing happens.
 *
 * <p>The Loan limit is a rule of the domain, not a shape of the request: the
 * Member's other Active loans are state this service holds, and the limit is
 * a value configuration binds - so the count is taken here, against
 * {@code properties.limit()}, and never expressed as a constant constraint
 * on {@link LoanRequest}, which could read neither.
 */
@Service
public class LoanService {

    private final LoanRepository loans;
    private final LoanProperties properties;
    private final Clock clock;

    LoanService(LoanRepository loans, LoanProperties properties, Clock clock) {
        this.loans = loans;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Lends one Copy to one Member, unless the Member is at the Loan limit.
     * The controller passes the request through the availability gate; this
     * is where the library's lending rules are enforced - a refusal is
     * thrown, in the domain's own words, for the handler to translate.
     *
     * @throws LoanLimitExceededException if the Member already holds Active
     *         loans up to the limit configuration sets
     */
    public Loan borrow(String memberId, String copyId) {
        if (loans.countByMemberId(memberId) >= properties.limit()) {
            throw new LoanLimitExceededException(properties.limit());
        }
        LocalDate borrowedOn = LocalDate.now(clock);
        // A loan period is a number of days, and a due date is a date.
        long periodInDays = properties.period().toDays();
        return loans.save(memberId, copyId, borrowedOn, borrowedOn.plusDays(periodInDays));
    }
}
