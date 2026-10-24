package com.example.library.loan;

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
import com.example.library.book.Copy;
import com.example.library.book.CopyNotFoundException;
import com.example.library.book.CopyRepository;

/**
 * The loan API.
 *
 * <p>The controller owns the edge of the borrow: it validates the request's
 * shape, asks the availability service whether the copy is on the shelf, and
 * maps the request's copy id - the barcode, every copy's external name - to
 * the Copy the database holds. Mapping at the edge means the use case is
 * handed the row the store will work on, and no barcode crosses into the
 * boundary.
 *
 * <p>The library's lending rules are the LoanService's - the controller does
 * not count a Member's Active loans, does not take the Copy and does not
 * mint a Loan.
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loans;
    private final AvailabilityClient availabilityClient;
    private final CopyRepository copies;

    LoanController(LoanService loans, AvailabilityClient availabilityClient, CopyRepository copies) {
        this.loans = loans;
        this.availabilityClient = availabilityClient;
        this.copies = copies;
    }

    @PostMapping
    public ResponseEntity<Loan> borrow(@Valid @RequestBody LoanRequest request) {
        // The service's answer precedes everything else: a copy the
        // availability service says is off the shelf is refused before the
        // library's own side of the borrow runs at all.
        Availability availability = availabilityClient.availability(request.copyId());
        if (!availability.available()) {
            throw new CopyNotAvailableException(request.copyId());
        }
        Copy copy =
                copies.findByBarcode(request.copyId()).orElseThrow(() -> new CopyNotFoundException(request.copyId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(loans.borrow(request.memberId(), copy));
    }
}
