package com.example.library.loan;

import java.time.LocalDate;

/**
 * An active borrowing of one copy by one member.
 *
 * <p>The payload a borrow answers with. Its words are the library's, not the
 * loan table's: the copy is named by its barcode, the member by their
 * identifier, the loan by the key the database minted for the row.
 *
 * @param id the loan row's key in the database
 * @param memberId the member holding the loan
 * @param copyId the barcode of the copy that is lent out
 * @param borrowedOn the day the loan started
 * @param dueOn the day the copy must be back
 */
public record Loan(Long id, String memberId, String copyId, LocalDate borrowedOn, LocalDate dueOn) {}
