package com.example.library.availability;

/**
 * What the availability service says about one copy.
 *
 * @param copyId the copy the service was asked about
 * @param available whether the copy is on the shelf
 */
public record Availability(String copyId, boolean available) {}
