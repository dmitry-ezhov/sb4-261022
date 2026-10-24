package com.example.library.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The repositories on the engine the library can actually ship on, inside the
 * suite rather than behind a profile.
 *
 * <p>What the suite asserts on H2 is the boundary - the web tier at the
 * slices - and the loan seams: the bound Loan limit, the real availability
 * client, at one full-context home. What is asserted only here is the engine
 * the persistence half is asked to survive: the single migration runs on
 * PostgreSQL unchanged and shapes the second engine, the catalogue's
 * {@code JdbcClient} reads map its rows into the {@link Title} record the API
 * serves, the repository's derived query runs real SQL against it, and the
 * migration's constraints are the engine's. {@code @ServiceConnection} is
 * Boot's answer to the configuration the H2 rig hard-codes: the container's
 * address reaches the datasource with no property line. The class is guarded
 * by {@code @EnabledIfDockerAvailable}, so a machine without a Docker daemon
 * skips it - a skip is a skip, never a pass.
 */
@SpringBootTest
@EnabledIfDockerAvailable
class TitleRepositoryTests {

    private static final long TITLE_ID = 97L;
    private static final String TITLE_ISBN = "9780575082709";
    private static final String DUPLICATE_ISBN = "9780575115419";

    // PostgreSQL 17, the version the M05 doc checked its migration against.
    @Container
    @ServiceConnection
    private static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    private final TitleCatalogue titles;
    private final CopyRepository copies;
    private final JdbcClient jdbcClient;

    @Autowired
    TitleRepositoryTests(TitleCatalogue titles, CopyRepository copies, JdbcClient jdbcClient) {
        this.titles = titles;
        this.copies = copies;
        this.jdbcClient = jdbcClient;
    }

    /**
     * The catalogue reads what the database holds: rows written in SQL - the
     * way a client of the database would - are the same work the repository
     * answers with, mapped straight into the API's record.
     */
    @Test
    void aTitleWrittenInSqlIsWhatTheCatalogueReads() {
        writeTitle(TITLE_ID, "The Dispossessed", TITLE_ISBN);

        assertThat(titles.findAll())
                .as("The catalogue did not read the title table on PostgreSQL: the migration "
                        + "did not shape the second engine, or the reads never ran on it - the "
                        + "one migration must reach both engines unchanged.")
                .anySatisfy(title -> {
                    assertThat(title.id()).isEqualTo(TITLE_ID);
                    assertThat(title.name()).isEqualTo("The Dispossessed");
                    assertThat(title.isbn()).isEqualTo(TITLE_ISBN);
                });
        assertThat(titles.findById(TITLE_ID))
                .as("The catalogue's lookup missed a row the engine holds, so the reads are "
                        + "not answered by the database the application is connected to.")
                .satisfies(title -> {
                    assertThat(title).isPresent();
                    assertThat(title.orElseThrow().author()).isEqualTo("Ursula K. Le Guin");
                });
    }

    /**
     * The Copy reads the application defines by their method names are real
     * SQL on this engine: a Copy's on-loan state comes back as written, and
     * the derived query is not something H2 alone would have compiled.
     */
    @Test
    void aTitlesCopiesAreReadByTheDerivedQueryOnPostgresql() {
        writeTitle(96L, "Always Coming Home", "9780575082709001");
        writeCopy(971L, 96L, "9780575082709001001", true);
        writeCopy(972L, 96L, "9780575082709001002", false);

        List<Copy> served = copies.findByTitleId(96L);

        assertThat(served)
                .as("The repository did not read the copy table on PostgreSQL: a copy the "
                        + "engine holds is not among the read's answer, so the derived query "
                        + "never ran on the second engine.")
                .hasSize(2)
                .contains(
                        new Copy(971L, 96L, "9780575082709001001", true),
                        new Copy(972L, 96L, "9780575082709001002", false));
    }

    /**
     * The migration's constraints are the second engine's law, not H2's
     * courtesy: a second row for the same edition is refused by the engine
     * the library ships on.
     */
    @Test
    void theMigrationConstraintsHoldOnTheSecondEngine() {
        writeTitle(98L, "The Fifth Head of Cerberus", DUPLICATE_ISBN);

        try {
            writeTitle(99L, "The Fifth Head of Cerberus", DUPLICATE_ISBN);
            throw new AssertionError("The engine accepted a second ISBN the migration declared unique: the "
                    + "constraints are not the second engine's, so nothing here "
                    + "asserts the schema the library ships on - the migration must "
                    + "run unchanged on PostgreSQL and its rules hold there.");
        } catch (DataIntegrityViolationException refused) {
            assertThat(refused.getMessage())
                    .as("The refusal was the engine's unique-key constraint, not something "
                            + "the application path invented.")
                    .contains("unique constraint");
        }
    }

    private void writeTitle(long id, String name, String isbn) {
        jdbcClient
                .sql("insert into title (id, name, author, isbn) values (:id, :name, :author, :isbn)")
                .param("id", id)
                .param("name", name)
                .param("author", "Ursula K. Le Guin")
                .param("isbn", isbn)
                .update();
    }

    private void writeCopy(long id, long titleId, String barcode, boolean onLoan) {
        jdbcClient
                .sql("insert into copy (id, title_id, barcode, on_loan) values (:id, :titleId, :barcode, :onLoan)")
                .param("id", id)
                .param("titleId", titleId)
                .param("barcode", barcode)
                .param("onLoan", onLoan)
                .update();
    }
}
