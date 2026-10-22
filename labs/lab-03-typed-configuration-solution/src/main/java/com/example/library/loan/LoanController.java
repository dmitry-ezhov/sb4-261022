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
 * <p>The loan period and the loan limit come from the configuration, and the
 * configuration is validated before the application serves a request.
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final Clock clock;
    private final LoanProperties properties;

    public LoanController(Clock clock, LoanProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<Loan> borrow(@Valid @RequestBody LoanRequest request) {
        LocalDate borrowedOn = LocalDate.now(clock);
        // A loan period is a number of days, and a due date is a date.
        long periodInDays = properties.period().toDays();
        Loan loan =
                new Loan("l-001", request.memberId(), request.copyId(), borrowedOn, borrowedOn.plusDays(periodInDays));
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }
}
