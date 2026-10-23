package com.example.library.availability;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * The availability service: what other libraries and the branch system know
 * about a copy.
 */
@HttpExchange("/api/availability")
public interface AvailabilityClient {

    /**
     * Asks whether a copy is on the shelf.
     *
     * @param copyId the copy to ask about
     * @return what the service knows about the copy
     */
    @GetExchange("/{copyId}")
    Availability availability(@PathVariable String copyId);
}
