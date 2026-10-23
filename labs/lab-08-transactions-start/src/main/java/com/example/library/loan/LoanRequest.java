package com.example.library.loan;

import jakarta.validation.constraints.NotBlank;

import com.example.library.api.InvalidRequestTitle;

/**
 * A request to borrow one copy.
 *
 * @param memberId the member who wants to borrow
 * @param copyId the copy they want
 */
public record LoanRequest(
        @NotBlank String memberId, @NotBlank String copyId) implements InvalidRequestTitle {

    @Override
    public String invalidRequestTitle() {
        return "Invalid loan request";
    }
}
