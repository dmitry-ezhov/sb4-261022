package com.example.library.api;

/**
 * A request payload that names the feature it belongs to, in the glossary's
 * terms.
 *
 * <p>The boundary's handler answers a failed body in that feature's words: a
 * borrow request's refusal says the loan feature's words, a reservation's
 * says the reservation's. A handler shared by every request must not borrow a
 * neighbour's words - the title a client reads is part of the API's
 * vocabulary, and {@code "Invalid reservation request"} on a borrow request
 * is the course's own example of the answer gone wrong.
 */
public interface InvalidRequestTitle {

    /**
     * The {@code title} of the problem detail served when this request's body
     * does not validate - for example {@code "Invalid loan request"}.
     */
    String invalidRequestTitle();
}
