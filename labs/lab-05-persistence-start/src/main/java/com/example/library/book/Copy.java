package com.example.library.book;

/**
 * A physical item of a {@link Title} that can be lent to a member.
 *
 * @param id the library's identifier for the item
 * @param titleId the identifier of the work this item is a copy of
 * @param barcode the barcode printed on the item
 * @param onLoan whether the item is currently lent out
 */
public record Copy(String id, String titleId, String barcode, boolean onLoan) {}
