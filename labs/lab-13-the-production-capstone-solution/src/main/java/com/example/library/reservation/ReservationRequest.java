package com.example.library.reservation;

import jakarta.validation.constraints.NotBlank;

import com.example.library.api.InvalidRequestTitle;

/**
 * A request to reserve a title.
 *
 * @param memberId the member who wants to reserve
 * @param titleId the title they want
 */
public record ReservationRequest(
        @NotBlank String memberId, @NotBlank String titleId) implements InvalidRequestTitle {

    @Override
    public String invalidRequestTitle() {
        return "Invalid reservation request";
    }
}
