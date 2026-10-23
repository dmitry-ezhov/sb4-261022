package com.example.library.book;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The Copy reads of the lending side: one Title's items, described as the
 * {@link Copy} records the API serves rather than as the entities they are
 * stored as. The entity/payload split is made here, at the boundary.
 *
 * <p>The queries are derived from the method names and checked when the
 * application starts - a misspelled property fails boot, not a request.
 */
public interface CopyRepository extends JpaRepository<CopyEntity, Long> {

    /** The Copies that are items of one Title. */
    List<Copy> findByTitleId(Long titleId);
}
