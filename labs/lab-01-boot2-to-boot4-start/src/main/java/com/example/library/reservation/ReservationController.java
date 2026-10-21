package com.example.library.reservation;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The reservation API.
 *
 * <p>Written the way a Spring Boot 2 application would have written it: the
 * request body is checked by hand, the created reservation is wrapped in an
 * envelope, and a rejected request says nothing about what was wrong.
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> reserve(@RequestBody Map<String, String> request) {
        String memberId = request.get("memberId");
        String titleId = request.get("titleId");
        if (memberId == null || memberId.isBlank() || titleId == null || titleId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Map<String, Object> reservation = Map.of(
                "id", "r-001",
                "memberId", memberId,
                "titleId", titleId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("reservation", reservation));
    }
}
