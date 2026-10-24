package com.example.library.loan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The loan store's statements on the engine the library can actually ship
 * on, inside the suite rather than behind a profile.
 *
 * <p>What the suite asserts on H2 is the boundary answers - the loan seams
 * at the one full-context home. What is asserted only here is the store's
 * SQL on the second engine: the loan table the lab's own migration adds is
 * shaped there unchanged, the store's insert writes a row the database keeps
 * and minted a key for, the limit's count reads that row back, and the Copy
 * guard is one statement whose 1/0 answer is the second engine's own. Each
 * call runs on its own connection, the way a call outside a boundary runs.
 * {@code @ServiceConnection} is Boot's answer to the configuration the H2
 * rig hard-codes, and the class is guarded by
 * {@code @EnabledIfDockerAvailable}, so a machine without a Docker daemon
 * skips it - a skip is a skip, never a pass. The concurrent half of the
 * guard's story is the diagnostics', not this class's: two threads against
 * PostgreSQL are the Lab's own proving rig, run under its profile.
 */
@SpringBootTest
@EnabledIfDockerAvailable
class LoanRepositoryTests {

    // PostgreSQL 17, the version the M05 doc checked its migration against.
    @Container
    @ServiceConnection
    private static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    private final LoanRepository loans;
    private final JdbcClient jdbcClient;

    @Autowired
    LoanRepositoryTests(LoanRepository loans, JdbcClient jdbcClient) {
        this.loans = loans;
        this.jdbcClient = jdbcClient;
    }

    /**
     * The store's insert writes what the database keeps: the row answers the
     * key the engine minted, and the limit's own question - the count - reads
     * it back from the loan table the migration added.
     */
    @Test
    void aLoanTheStoreSavesIsARowTheCountReadsBack() {
        String barcode = writeACopyOnTheShelf();
        Long copyId = keyOf(barcode);
        String memberId = "m-repo-saves-" + UUID.randomUUID();

        Long loanRow = loans.save(memberId, copyId, LocalDate.parse("2026-01-05"), LocalDate.parse("2026-01-19"));

        assertThat(loanRow)
                .as("The store's insert came back with no key, so a Loan the database keeps "
                        + "has no identifier the application could read it by.")
                .isPositive();
        assertThat(loans.countByMemberId(memberId))
                .as("A Loan the store saved is not what the loan table holds: the insert and "
                        + "the limit's count do not speak about the same table, so the boundary's "
                        + "refusal and its writes answer different stores.")
                .isEqualTo(1);
        assertThat(loanRowOf(loanRow))
                .as("The Loan row the store wrote does not carry what the use case gave it: the "
                        + "columns must hold the member, the copy and the loan period's dates as "
                        + "written.")
                .containsExactly(memberId, String.valueOf(copyId), "2026-01-05", "2026-01-19");
    }

    /**
     * The Copy's guard is one statement on the second engine too: the first
     * take of a Copy on the shelf is {@code 1}, the repeat is {@code 0}, and
     * another Copy on another row is still a {@code 1} - the where clause is
     * the whole condition, per row, not a global switch.
     */
    @Test
    void theCopysGuardIsOneStatementWhoseAnswerDecides() {
        String barcode = writeACopyOnTheShelf();
        Long copyId = keyOf(barcode);
        String otherBarcode = writeACopyOnTheShelf();
        Long otherCopyId = keyOf(otherBarcode);

        assertThat(loans.takeOnLoan(copyId))
                .as("The guard did not match the Copy on the shelf, so borrowing a Copy the "
                        + "database holds was refused before the database ever answered.")
                .isEqualTo(1);
        assertThat(loans.takeOnLoan(copyId))
                .as("The guard matched a Copy already on loan, so repeating the request lent it "
                        + "again: the where clause does not carry the condition - the statement "
                        + "is not the guard the lab asks for.")
                .isEqualTo(0);
        assertThat(loans.takeOnLoan(otherCopyId))
                .as("The guard answers per copy, not per table: the second take refused a "
                        + "different Copy the database still holds on the shelf.")
                .isEqualTo(1);
    }

    private String writeACopyOnTheShelf() {
        String isbn = "isbn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String barcode = "bc-" + UUID.randomUUID();
        jdbcClient
                .sql("""
                        insert into title (name, author, isbn)
                        values (:name, :author, :isbn)
                        """)
                .param("name", "The Dispossessed")
                .param("author", "Ursula K. Le Guin")
                .param("isbn", isbn)
                .update();
        Long titleId = keyOfTitle(isbn);
        jdbcClient
                .sql("""
                        insert into copy (title_id, barcode, on_loan)
                        values (:titleId, :barcode, :onLoan)
                        """)
                .param("titleId", titleId)
                .param("barcode", barcode)
                // An Available copy: not currently on loan.
                .param("onLoan", Boolean.FALSE)
                .update();
        return barcode;
    }

    private Long keyOfTitle(String isbn) {
        return jdbcClient
                .sql("select id from title where isbn = :isbn")
                .param("isbn", isbn)
                .query(Long.class)
                .single();
    }

    private Long keyOf(String barcode) {
        return jdbcClient
                .sql("select id from copy where barcode = :barcode")
                .param("barcode", barcode)
                .query(Long.class)
                .single();
    }

    private List<String> loanRowOf(long loanId) {
        return jdbcClient
                .sql("""
                        select member_id, copy_id::text, borrowed_on::text, due_on::text
                        from loan
                        where id = :id
                        """)
                .param("id", loanId)
                .query((resultSet, rowNum) -> List.of(
                        resultSet.getString("member_id"),
                        resultSet.getString("copy_id"),
                        resultSet.getString("borrowed_on"),
                        resultSet.getString("due_on")))
                .single();
    }
}
