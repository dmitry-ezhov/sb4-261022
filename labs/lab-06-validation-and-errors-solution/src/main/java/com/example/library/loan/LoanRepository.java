package com.example.library.loan;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

/**
 * The Loan feature's store: the Active loans the library holds.
 *
 * <p>In memory for now; the transaction lab moves the store to the database.
 * The rule keeps its shape either way, because the LoanService only asks this
 * store questions it can answer: how many Active loans one Member holds.
 */
@Repository
public class LoanRepository {

    private final List<Loan> activeLoans = new CopyOnWriteArrayList<>();
    private final AtomicLong loansLent = new AtomicLong();

    /**
     * How many Active loans one Member holds: the question the Loan limit is
     * asked against, and the only question a rule needs of a store that is
     * still in memory.
     */
    public long countByMemberId(String memberId) {
        return activeLoans.stream()
                .filter(loan -> loan.memberId().equals(memberId))
                .count();
    }

    /**
     * Lends the Copy: a loan is Active the moment it is saved, and the
     * library's identifier for it is minted here, in the store.
     */
    public Loan save(String memberId, String copyId, LocalDate borrowedOn, LocalDate dueOn) {
        // The library's identifier is minted in the store, so every loan - in
        // memory now, in the database later - carries one and only one id.
        Loan loan = new Loan("l-" + "%03d".formatted(loansLent.incrementAndGet()), memberId, copyId, borrowedOn, dueOn);
        activeLoans.add(loan);
        return loan;
    }
}
