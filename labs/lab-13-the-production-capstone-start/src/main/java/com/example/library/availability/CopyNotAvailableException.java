package com.example.library.availability;

/**
 * Raised when the availability service says a copy is not on the shelf.
 */
public class CopyNotAvailableException extends RuntimeException {

    public CopyNotAvailableException(String copyId) {
        super("Copy " + copyId + " is not on the shelf");
    }
}
