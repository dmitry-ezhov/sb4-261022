package com.example.library.book;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The catalogue half of the persistence: the reads the library does by the
 * Title's own name and key, which carry no domain rule about lending.
 *
 * <p>The interface in the middle is {@code JdbcClient}: named parameters, and
 * the row mapped straight into the {@link Title} record the API serves - what
 * {@code JdbcTemplate} needed a RowMapper for. There is no entity here: a
 * Title row has no state of its own to change, so a class with accessors
 * would be arrangement, not information.
 */
@Repository
public class TitleCatalogue {

    private final JdbcClient jdbcClient;

    TitleCatalogue(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Every Title the database holds, in the order of their keys. */
    public List<Title> findAll() {
        return jdbcClient.sql("""
                        select id, name, author, isbn
                        from title
                        order by id
                        """).query(Title.class).list();
    }

    /** One Title by its key, or empty when the library does not hold it. */
    public Optional<Title> findById(Long id) {
        return jdbcClient.sql("""
                        select id, name, author, isbn
                        from title
                        where id = :id
                        """).param("id", id).query(Title.class).optional();
    }
}
