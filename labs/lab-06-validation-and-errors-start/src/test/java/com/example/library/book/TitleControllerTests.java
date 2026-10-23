package com.example.library.book;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The catalogue the persistence lab built: the endpoints answer with what the
 * database holds. Each class runs against a database of its own - a fresh one
 * per run, so what one test class wrote does not survive the next - and the
 * rows are written as any client of the database would: with the migration's
 * own words, through SQL, not through the application's web or service code.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TitleControllerTests {

    private static final long TITLE_ID = 91L;
    private static final long TITLE_ID_COPY = 93L;
    private static final long OTHER_TITLE_ID = 92L;
    private static final long COPY_ID = 911L;

    private final MockMvc mockMvc;
    private final JdbcClient jdbcClient;

    @Autowired
    TitleControllerTests(MockMvc mockMvc, JdbcClient jdbcClient) {
        this.mockMvc = mockMvc;
        this.jdbcClient = jdbcClient;
    }

    @DynamicPropertySource
    static void aFreshDatabaseForTheTests(DynamicPropertyRegistry registry) {
        // A database the tests own, the way the diagnostics hand one over: the
        // application is pointed at it as it would be pointed at its H2.
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:library-tests-" + UUID.randomUUID());
        registry.add("spring.datasource.username", () -> "sa");
    }

    @Test
    void theCatalogueServesWhatTheDatabaseHolds() throws Exception {
        writeTitle(TITLE_ID, "9780441478125");
        writeTitle(OTHER_TITLE_ID, "9780525429974");

        mockMvc.perform(get("/api/titles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value((int) TITLE_ID))
                .andExpect(jsonPath("$[0].isbn").value("9780441478125"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void aTitleOnTheShelfAndOneLentOutAreBothServedAsCopies() throws Exception {
        writeTitle(TITLE_ID_COPY, "9781635575637");
        writeCopy(COPY_ID, TITLE_ID_COPY, "9781635575637001", true);
        writeCopy(912L, TITLE_ID_COPY, "9781635575637002", false);

        mockMvc.perform(get("/api/titles/" + TITLE_ID_COPY + "/copies"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].barcode").value("9781635575637001"))
                .andExpect(jsonPath("$[0].onLoan").value(true))
                .andExpect(jsonPath("$[1].onLoan").value(false));
    }

    @Test
    void aTitleTheDatabaseDoesNotHoldIsAProblemDetail() throws Exception {
        mockMvc.perform(get("/api/titles/424242"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").isNotEmpty());
    }

    private void writeTitle(long id, String isbn) {
        jdbcClient
                .sql("insert into title (id, name, author, isbn) values (:id, :name, :author, :isbn)")
                .param("id", id)
                .param("name", "The Left Hand of Darkness")
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
