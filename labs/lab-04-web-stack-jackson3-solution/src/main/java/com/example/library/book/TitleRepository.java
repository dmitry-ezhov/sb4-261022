package com.example.library.book;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

/**
 * In-memory catalogue of the works the library holds, and of the physical items
 * it can lend. Replaced by a real database in the persistence module.
 */
@Repository
public class TitleRepository {

    private final List<Title> titles = List.of(
            new Title("t-001", "The Left Hand of Darkness", "Ursula K. Le Guin", "9780441478125"),
            new Title("t-002", "Piranesi", "Susanna Clarke", "9781635575637"));

    private final List<Copy> copies = List.of(
            new Copy("c-001", "t-001", "9780441478125001", false),
            new Copy("c-002", "t-001", "9780441478125002", true),
            new Copy("c-003", "t-002", "9781635575637001", false));

    public List<Title> findAll() {
        return titles;
    }

    public Optional<Title> findById(String id) {
        return titles.stream().filter(title -> title.id().equals(id)).findFirst();
    }

    public List<Copy> findCopiesByTitleId(String titleId) {
        return copies.stream().filter(copy -> copy.titleId().equals(titleId)).toList();
    }
}
