package com.example.library.book;

/**
 * A work held by the library, described independently of the physical copies
 * that are lent to members. The payload the catalogue API serves: the row of
 * the title table, read by name.
 *
 * @param id the Title's key in the database
 * @param name the title of the work
 * @param author the author of the work
 * @param isbn the ISBN of the edition the library holds
 */
public record Title(Long id, String name, String author, String isbn) {}
