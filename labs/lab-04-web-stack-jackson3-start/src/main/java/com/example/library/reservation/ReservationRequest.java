package com.example.library.reservation;

import jakarta.validation.constraints.NotBlank;

/**
 * A request to reserve a title.
 *
 * @param memberId the member who wants to reserve
 * @param titleId the title they want
 */
public record ReservationRequest(
        @NotBlank String memberId, @NotBlank String titleId) {}
