package com.example.library.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Diagnostics for the M05 lab: one configuration, two engines.
 *
 * <p>The application the lab is building must run unchanged on PostgreSQL:
 * the migration is written in standard SQL once, and both engines take it.
 * These tests ask Testcontainers for a PostgreSQL and boot the application
 * against it through {@code @ServiceConnection} - no second configuration.
 * They are excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/&#42;Test.java}) so that the start module stays green
 * while the defects are still present. Run them with:
 *
 * <pre>./mvnw -f labs -pl lab-05-persistence-start verify -Pdiagnostics</pre>
 *
 * <p>A Docker daemon is needed to run a PostgreSQL. Machines without one are
 * not blocked from this lab: {@code @EnabledIfDockerAvailable} skips this
 * class there, and {@link PersistenceDiagnosticsTest} covers everything else
 * on H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfDockerAvailable
class PersistenceOnPostgresqlDiagnosticsTest {

    private static final long TITLE_ID = 95L;
    private static final long COPY_ID = 951L;
    private static final String TITLE_ISBN = "9780525429974";
    private static final String COPY_BARCODE = "9780525429974001";

    // PostgreSQL 17, the version the M05 doc checked its migration against.
    @Container
    @ServiceConnection
    private static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    private final MockMvc mockMvc;
    private final JdbcClient jdbcClient;

    @Autowired
    PersistenceOnPostgresqlDiagnosticsTest(MockMvc mockMvc, JdbcClient jdbcClient) {
        this.mockMvc = mockMvc;
        this.jdbcClient = jdbcClient;
    }

    @Test
    void theCatalogueIsTheDatabaseOnPostgresql() throws Exception {
        long applied = appliedMigrations();

        assertThat(applied)
                .as("Flyway applied no migration against PostgreSQL: the schema was shaped on H2 "
                        + "alone, so the single migration is not being taken by the second engine. "
                        + "Write it in standard SQL once (identity, boolean, varchar - no H2-only "
                        + "limits, no MODE=PostgreSQL) and both engines run it unchanged.")
                .isPositive();

        writeCatalogueRows(TITLE_ID, COPY_ID, TITLE_ISBN, COPY_BARCODE);

        MockHttpServletResponse titles = responseOf("/api/titles");
        assertThat(titles.getStatus())
                .as("The application does not serve the catalogue from PostgreSQL: "
                        + "GET /api/titles must answer 200 with a title the database on "
                        + "the container holds. One configuration - one module, two engines.")
                .isEqualTo(200);
        assertThat(titles.getContentAsString())
                .as("The application does not serve the catalogue from PostgreSQL: a title the "
                        + "database on the container holds is not among GET /api/titles' payload.")
                .contains(TITLE_ISBN);

        MockHttpServletResponse copies = responseOf("/api/titles/" + TITLE_ID + "/copies");
        assertThat(copies.getStatus())
                .as("A copy the PostgreSQL database holds does not resolve on "
                        + "GET /api/titles/{id}/copies: the reads must come from the database "
                        + "the application is connected to, not from the compiled List.")
                .isEqualTo(200);
        assertThat(copies.getContentAsString())
                .as("A copy the PostgreSQL database holds is not among the payload of "
                        + "GET /api/titles/{id}/copies, which still reads the compiled List.")
                .contains(COPY_BARCODE);
    }

    private void writeCatalogueRows(long titleId, long copyId, String titleIsbn, String copyBarcode) {
        try {
            jdbcClient
                    .sql("insert into title (id, name, author, isbn) values (:id, :name, :author, :isbn)")
                    .param("id", titleId)
                    .param("name", "The Dispossessed")
                    .param("author", "Ursula K. Le Guin")
                    .param("isbn", titleIsbn)
                    .update();
            jdbcClient
                    .sql("insert into copy (id, title_id, barcode, on_loan) values (:id, :titleId, :barcode, :onLoan)")
                    .param("id", copyId)
                    .param("titleId", titleId)
                    .param("barcode", copyBarcode)
                    // An Available copy: not currently on loan.
                    .param("onLoan", Boolean.FALSE)
                    .update();
        } catch (BadSqlGrammarException noTables) {
            fail("""
                    Nothing could be written to PostgreSQL, so the catalogue cannot be read \
                    from it: the title and copy tables do not exist there yet. The migration \
                    the H2 run applied must reach this engine unchanged - standard SQL only, \
                    no H2-only syntax.""", noTables);
        }
    }

    /**
     * The number of migrations Flyway has applied on the container's
     * PostgreSQL, and a failure that names what is missing when the migration
     * has not been taken by the second engine.
     */
    private long appliedMigrations() {
        try {
            return jdbcClient
                    .sql("select count(*) from \"flyway_schema_history\"")
                    .query(Long.class)
                    .single();
        } catch (BadSqlGrammarException notMigrated) {
            fail("""
                    Flyway is not managing PostgreSQL: there is no flyway_schema_history table \
                    there, so no migration has been applied to the container's database - the \
                    application booted against a fresh one all its own. The M05 lab asks for \
                    one configuration and one migration that both engines take: check that the \
                    H2 configuration's Flyway starter and migration are what this run picks \
                    up too.""", notMigrated);
        }
        return 0;
    }

    private MockHttpServletResponse responseOf(String uri) throws Exception {
        return mockMvc.perform(get(uri)).andReturn().getResponse();
    }
}
