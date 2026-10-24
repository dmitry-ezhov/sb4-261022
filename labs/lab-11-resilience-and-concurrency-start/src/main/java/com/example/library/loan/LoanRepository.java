package com.example.library.loan;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

/**
 * The Loan feature's store: the Active loans the database holds, over the
 * {@code loan} table the migration owns.
 *
 * <p>Every SQL statement in the loan feature lives here, and every call of
 * these methods happens inside the boundary the use case opens: a store
 * method is one statement whose answer the caller decides on, never
 * transaction demarcation of its own.
 *
 * <p>The reads answer JdbcClient's named parameters straight into the
 * vocabulary the table carries. The one write that must hand back the row
 * the database minted its key for goes through Spring JDBC's insert tool -
 * the reads stay where they are.
 */
@Repository
public class LoanRepository {

    private final JdbcClient jdbcClient;
    private final SimpleJdbcInsert loanInsert;

    LoanRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.loanInsert =
                new SimpleJdbcInsert(jdbcTemplate).withTableName("loan").usingGeneratedKeyColumns("id");
    }

    /**
     * How many Active loans one Member holds: the question the Loan limit is
     * asked against, read off the loan table rather than counted in compiled
     * code - the same shape the in-memory store answered since M03.
     */
    public long countByMemberId(String memberId) {
        return jdbcClient.sql("""
                        select count(*)
                        from loan
                        where member_id = :memberId
                        """).param("memberId", memberId).query(Long.class).single();
    }

    /**
     * The Copy's guard, and the whole point of the lab: one statement, whose
     * where clause carries the entire condition. It matches only the Copy
     * whose flag is still down, and the database's own atomicity is the
     * guard - two transactions racing on one row are serialised at the row
     * lock, and the second one's where clause re-reads the marked Copy and
     * finds nothing to change.
     *
     * @return {@code 1} when this caller took the Copy, {@code 0} when
     *         someone else took it in the gap - the answer the use case
     *         routes on, a refusal rather than a second Loan
     */
    public int takeOnLoan(Long copyId) {
        return jdbcClient.sql("""
                        update copy
                        set on_loan = true
                        where id = :copyId and on_loan = false
                        """).param("copyId", copyId).update();
    }

    /**
     * Writes the Loan row the database holds, inside the use case's
     * boundary, and answers the key the database minted for it: the loan's
     * identifier is the row's own, not one compiled code invents.
     */
    public Long save(String memberId, Long copyId, LocalDate borrowedOn, LocalDate dueOn) {
        return loanInsert
                .executeAndReturnKey(Map.of(
                        "member_id", memberId,
                        "copy_id", copyId,
                        "borrowed_on", borrowedOn,
                        "due_on", dueOn))
                .longValue();
    }
}
