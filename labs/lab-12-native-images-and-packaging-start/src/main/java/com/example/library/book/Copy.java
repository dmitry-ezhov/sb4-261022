package com.example.library.book;

/**
 * A physical item of a {@link Title} that can be lent to a member. The
 * payload the catalogue API serves.
 *
 * @param id the Copy's key in the database
 * @param titleId the key of the Title this item is a copy of
 * @param barcode the barcode printed on the item
 * @param onLoan whether the item is currently lent out
 */
public record Copy(Long id, Long titleId, String barcode, boolean onLoan) {}
