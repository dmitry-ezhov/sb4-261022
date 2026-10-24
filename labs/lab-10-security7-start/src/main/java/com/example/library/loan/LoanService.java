package com.example.library.loan;

import java.time.Clock;
import java.time.LocalDate;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.availability.CopyNotAvailableException;
import com.example.library.book.Copy;

/**
 * The Loan feature's rules, where borrowing happens.
 *
 * <p>The Loan limit is a rule of the domain, not a shape of the request: the
 * Member's other Active loans are state the database holds, and the limit is
 * a value configuration binds - so the count is taken here, against
 * {@code properties.limit()}, and never expressed as a constant constraint
 * on {@link LoanRequest}, which could read neither.
 *
 * <p>This use case also says what it did while it ran: every issuance is
 * counted in the metrics registry, under
 * {@code library.loans.issued}, the increment sitting here - where the Loan
 * row is written - rather than in the controller and not in a stats
 * gatherer afterwards.
 */
@Service
public class LoanService {

    private final LoanRepository loans;
    private final LoanProperties properties;
    private final Clock clock;
    private final MeterRegistry registry;

    LoanService(LoanRepository loans, LoanProperties properties, Clock clock, MeterRegistry registry) {
        this.loans = loans;
        this.properties = properties;
        this.clock = clock;
        this.registry = registry;
    }

    /**
     * Lends one Copy to one Member, unless the Member is at the Loan limit,
     * and the borrow is the boundary the lab is about: the Loan limit's
     * count, the Copy's guard and the Loan row all happen inside the one
     * transaction this annotation opens for the use case - one commit, and
     * a domain refusal rolls the whole thing back rather than leaving half a
     * borrow behind. Nothing bigger sits under the boundary: a remote call
     * would turn a transaction into a round trip and pin a pooled connection
     * against another service's response time, so the M04 availability call
     * stays where it has always been, in the controller, before a
     * transaction exists.
     *
     * <p>The Copy arrives mapped at the edge: the controller resolved the
     * request's barcode to the Copy the database holds, so this use case
     * works on the row key and never maps an external identifier inside the
     * boundary.
     *
     * @param memberId the member to lend to, whose Active loans the limit
     *        counts
     * @param copy the Copy as the database holds it: the row the guard will
     *        take, with its key and its barcode
     * @return the Loan the database now holds, in the words the API answers
     *         in - the member's identifier, the Copy's barcode, the row's
     *         key
     * @throws LoanLimitExceededException if the Member already holds Active
     *         loans up to the limit configuration sets
     * @throws CopyNotAvailableException if the Copy's guard matched no row -
     *         someone took the Copy between the availability service's
     *         answer and this boundary, and the loser of the race is refused
     */
    @Transactional
    public Loan borrow(String memberId, Copy copy) {
        if (loans.countByMemberId(memberId) >= properties.limit()) {
            throw new LoanLimitExceededException(properties.limit());
        }
        if (loans.takeOnLoan(copy.id()) == 0) {
            throw new CopyNotAvailableException(copy.barcode());
        }
        LocalDate borrowedOn = LocalDate.now(clock);
        // A loan period is a number of days, and a due date is a date.
        long periodInDays = properties.period().toDays();
        LocalDate dueOn = borrowedOn.plusDays(periodInDays);
        Long loanId = loans.save(memberId, copy.id(), borrowedOn, dueOn);
        // The Loan was issued; the counter is here for exactly that fact and
        // no other. Its one tag is the Title the Loan was issued against -
        // a copyId or a memberId would be a time series per Copy and per
        // Member, and the metrics registry would drown in them; the Title is
        // a value the catalogue bounds. A counter is not transactional
        // state: it counts the issuances this use case made, and nothing
        // later in the process can roll it back.
        registry.counter("library.loans.issued", "title", Long.toString(copy.titleId()))
                .increment();
        return new Loan(loanId, memberId, copy.barcode(), borrowedOn, dueOn);
    }
}
