package com.example.library.book;

/**
 * A work held by the library, described independently of the physical copies
 * that are lent to members.
 *
 * @param id the library's identifier for the work
 * @param name the title of the work
 * @param author the author of the work
 * @param isbn the ISBN of the edition the library holds
 */
public record Title(String id, String name, String author, String isbn) {}
