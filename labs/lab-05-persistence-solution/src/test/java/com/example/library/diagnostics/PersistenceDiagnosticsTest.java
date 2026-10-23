package com.example.library.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Diagnostics for the M05 lab.
 *
 * <p>The library's catalogue is still the M04 shape: a hard-coded {@code List}
 * in compiled code, so restarting the application changes nothing and the
 * database is empty all boot. These tests state what must happen instead: a
 * Flyway migration owns the Title and Copy schema, and the catalogue is read
 * from it. They are excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/&#42;Test.java}) so that the start module stays green while
 * the defects are still present. Run them with:
 *
 * <pre>./mvnw -f labs -pl lab-05-persistence-start verify -Pdiagnostics</pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
class PersistenceDiagnosticsTest {

    private static final long TITLE_ID = 95L;
    private static final long COPY_ID = 951L;
    private static final String TITLE_ISBN = "9780525429974";
    private static final String COPY_BARCODE = "9780525429974001";

    private final MockMvc mockMvc;
    private final JdbcClient jdbcClient;

    @Autowired
    PersistenceDiagnosticsTest(MockMvc mockMvc, JdbcClient jdbcClient) {
        this.mockMvc = mockMvc;
        this.jdbcClient = jdbcClient;
    }

    @DynamicPropertySource
    static void aFreshDatabaseForTheDiagnostics(DynamicPropertyRegistry registry) {
        // A database the diagnostics own, freshly created per run: the
        // application is pointed at it exactly as it would be pointed at the
        // lab's H2, and nothing a neighbouring test class wrote survives.
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:lab05-" + UUID.randomUUID());
        registry.add("spring.datasource.username", () -> "sa");
    }

    @Test
    void theSchemaIsOwnedByAMigration() {
        long applied = appliedMigrations();

        assertThat(applied)
                .as("Flyway connected but applied no migrations: the schema was not shaped by one. "
                        + "The title and copy tables must be created by a migration "
                        + "(a V001__ file under src/main/resources/db/migration), "
                        + "not by Hibernate's ddl-auto guess.")
                .isPositive();

        assertThat(tableExists("title"))
                .as("The migration did not create the title table. The migration must carry it - "
                        + "see the M05 doc for the shape.")
                .isTrue();
        assertThat(tableExists("copy"))
                .as("The migration did not create the copy table, with its foreign key to title. "
                        + "The migration must carry it - see the M05 doc for the shape.")
                .isTrue();
    }

    @Test
    void theCatalogueIsTheDatabase() throws Exception {
        writeCatalogueRows(TITLE_ID, COPY_ID, TITLE_ISBN, COPY_BARCODE);

        MockHttpServletResponse titles = responseOf("/api/titles");
        assertThat(titles.getStatus())
                .as("The catalogue is not the database: GET /api/titles must answer 200 once the "
                        + "reads go through a repository (a derived query, or a JdbcClient query "
                        + "for the catalogue plumbing) instead of the compiled List.")
                .isEqualTo(200);
        assertThat(titles.getContentAsString())
                .as("The catalogue is not the database: a title the database holds is not among "
                        + "GET /api/titles' payload, which still reads the compiled List.")
                .contains(TITLE_ISBN);

        MockHttpServletResponse copies = responseOf("/api/titles/" + TITLE_ID + "/copies");
        assertThat(copies.getStatus())
                .as("A copy the database holds does not resolve: GET /api/titles/{id}/copies must "
                        + "answer 200 once the reads go through a repository, not through the "
                        + "compiled List.")
                .isEqualTo(200);
        assertThat(copies.getContentAsString())
                .as("The copy the database holds is not among the payload of "
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
                    Nothing could be written to the database, so the catalogue cannot be read \
                    from it: the title and copy tables do not exist yet. Fix the schema first - \
                    theSchemaIsOwnedByAMigration states what the migration must create.""", noTables);
        }
    }

    /**
     * The number of migrations Flyway has applied, and a failure that names
     * what is missing when Flyway is not managing the database at all.
     */
    private long appliedMigrations() {
        try {
            return jdbcClient
                    .sql("select count(*) from \"flyway_schema_history\"")
                    .query(Long.class)
                    .single();
        } catch (BadSqlGrammarException notMigrated) {
            fail("""
                    Flyway is not managing the library's database: there is no flyway_schema_history \
                    table, so no migration has ever been applied. In Boot 4 the Flyway \
                    auto-configuration lives in its own module: add spring-boot-starter-flyway - \
                    a bare flyway-core configures nothing - and a V001__create_title_and_copy \
                    migration under src/main/resources/db/migration.""", notMigrated);
        }
        return 0;
    }

    private boolean tableExists(String table) {
        try {
            jdbcClient.sql("select count(*) from " + table).query(Long.class).single();
            return true;
        } catch (BadSqlGrammarException noTable) {
            return false;
        }
    }

    private MockHttpServletResponse responseOf(String uri) throws Exception {
        return mockMvc.perform(get(uri)).andReturn().getResponse();
    }
}
