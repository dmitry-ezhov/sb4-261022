package com.example.library.reservation;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The reservation API.
 *
 * <p>Boot 4 shape: a typed request body validated by the framework, and a
 * typed record in the response.
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @PostMapping
    public ResponseEntity<Reservation> reserve(@Valid @RequestBody ReservationRequest request) {
        Reservation reservation = new Reservation("r-001", request.memberId(), request.titleId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }
}
