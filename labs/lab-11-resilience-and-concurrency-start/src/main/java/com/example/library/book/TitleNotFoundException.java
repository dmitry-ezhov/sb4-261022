package com.example.library.book;

/**
 * Raised when a catalogue lookup refers to a work the library does not hold.
 */
public class TitleNotFoundException extends RuntimeException {

    public TitleNotFoundException(Long titleId) {
        super("No title with id " + titleId);
    }
}
