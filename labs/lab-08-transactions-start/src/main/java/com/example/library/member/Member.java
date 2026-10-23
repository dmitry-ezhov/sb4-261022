package com.example.library.member;

/**
 * A person who borrows from the library.
 *
 * @param id the library's identifier for the member
 * @param name the member's name
 */
public record Member(String id, String name) {}
