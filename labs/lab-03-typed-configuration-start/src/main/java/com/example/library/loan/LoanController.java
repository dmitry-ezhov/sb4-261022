package com.example.library.loan;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The loan API.
 *
 * <p>Written the way a Boot 2 application would have written it: the loan
 * period is a constant in the code, because configuration is hard, and there
 * is no loan limit at all.
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private static final int LOAN_PERIOD_DAYS = 14;

    private final Clock clock;

    public LoanController(Clock clock) {
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<Loan> borrow(@Valid @RequestBody LoanRequest request) {
        LocalDate borrowedOn = LocalDate.now(clock);
        Loan loan = new Loan(
                "l-001", request.memberId(), request.copyId(), borrowedOn, borrowedOn.plusDays(LOAN_PERIOD_DAYS));
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }
}
