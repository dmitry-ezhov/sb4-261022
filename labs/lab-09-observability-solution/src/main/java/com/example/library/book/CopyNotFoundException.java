package com.example.library.book;

/**
 * Raised when a lending lookup refers to a Copy the library does not hold.
 *
 * <p>The availability service's word is about the system the Copy lives in
 * outside the library; the loan table's foreign key is about the rows the
 * library itself holds. A barcode the copy table has no row for is a
 * {@code 404}, not a {@code 409}: there is nothing taken about it, and
 * nothing about the request to fix - the name belongs to no shelf.
 */
public class CopyNotFoundException extends RuntimeException {

    public CopyNotFoundException(String barcode) {
        super("No copy with barcode " + barcode);
    }
}
