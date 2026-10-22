package com.example.library.loan;

import jakarta.validation.constraints.NotBlank;

/**
 * A request to borrow one copy.
 *
 * @param memberId the member who wants to borrow
 * @param copyId the copy they want
 */
public record LoanRequest(
        @NotBlank String memberId, @NotBlank String copyId) {}
