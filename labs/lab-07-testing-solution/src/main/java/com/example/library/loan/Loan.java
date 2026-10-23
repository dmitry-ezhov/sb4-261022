package com.example.library.loan;

import java.time.LocalDate;

/**
 * An active borrowing of one copy by one member.
 *
 * @param id the library's identifier for the loan
 * @param memberId the member holding the loan
 * @param copyId the copy that is lent out
 * @param borrowedOn the day the loan started
 * @param dueOn the day the copy must be back
 */
public record Loan(String id, String memberId, String copyId, LocalDate borrowedOn, LocalDate dueOn) {}
