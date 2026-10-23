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

import com.example.library.availability.Availability;
import com.example.library.availability.AvailabilityClient;
import com.example.library.availability.CopyNotAvailableException;

/**
 * The loan API.
 *
 * <p>Before a copy is lent, the library asks the availability service whether
 * it is on the shelf.
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final Clock clock;
    private final LoanProperties properties;
    private final AvailabilityClient availabilityClient;

    public LoanController(Clock clock, LoanProperties properties, AvailabilityClient availabilityClient) {
        this.clock = clock;
        this.properties = properties;
        this.availabilityClient = availabilityClient;
    }

    @PostMapping
    public ResponseEntity<Loan> borrow(@Valid @RequestBody LoanRequest request) {
        Availability availability = availabilityClient.availability(request.copyId());
        if (!availability.available()) {
            throw new CopyNotAvailableException(request.copyId());
        }
        LocalDate borrowedOn = LocalDate.now(clock);
        // A loan period is a number of days, and a due date is a date.
        long periodInDays = properties.period().toDays();
        Loan loan =
                new Loan("l-001", request.memberId(), request.copyId(), borrowedOn, borrowedOn.plusDays(periodInDays));
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }
}
