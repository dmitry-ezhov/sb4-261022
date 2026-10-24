package com.example.library.reservation;

/**
 * A member's hold on a title they cannot borrow yet.
 *
 * @param id the library's identifier for the reservation
 * @param memberId the member holding the reservation
 * @param titleId the title being held
 */
public record Reservation(String id, String memberId, String titleId) {}
