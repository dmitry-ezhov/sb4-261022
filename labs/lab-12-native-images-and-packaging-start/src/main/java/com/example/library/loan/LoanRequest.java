package com.example.library.loan;

import jakarta.validation.constraints.NotBlank;

import com.example.library.api.InvalidRequestTitle;

/**
 * A request to borrow one copy.
 *
 * <p>The record keeps only what a caller is allowed to choose - the Copy's
 * barcode. The Member the Loan belongs to is not a field a client supplies:
 * it is the authenticated principal, and the value stays out of the request
 * rather than being checked against it. A request record that still carried a
 * {@code memberId} would go on offering the caller an id that is true only
 * while one method remembers its annotation - so the field is gone, not
 * guarded. A body that still names a {@code memberId} is accepted: the field
 * is not in this record, so it is ignored, and the Loan lands on the caller.
 *
 * @param copyId the copy they want
 */
public record LoanRequest(@NotBlank String copyId) implements InvalidRequestTitle {

    @Override
    public String invalidRequestTitle() {
        return "Invalid loan request";
    }
}
