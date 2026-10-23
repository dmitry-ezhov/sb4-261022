package com.example.library.diagnostics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.library.FixedClockConfiguration;

/**
 * Diagnostics for the M08 lab: the Loan feature's accounting reaches the
 * database.
 *
 * <p>The application was left with the Loan store in compiled code: a
 * {@code CopyOnWriteArrayList} the {@code LoanRepository} comment has been
 * promising to replace since M05, no loan table anywhere in the schema, and
 * a borrow that cannot even talk to a transaction. These tests state what
 * must happen instead: a borrow is a Loan row the database holds, and a
 * repeated borrow of one Copy is refused, not a second loan. They are
 * excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/*Test.java}) so that the start module stays green
 * while the defects are still present. Run them with:
 *
 * <pre>./mvnw -f labs -pl lab-08-transactions-start verify -Pdiagnostics</pre>
 *
 * <p>Diagnostics are asserted in CI with the solutions-only profile: the same
 * tests, run only against the reference implementations by
 * {@code ./mvnw -f labs verify -Pdiagnostics-solutions}.
 *
 * <p>The race two borrowing threads can still win even with everything above
 * done is stated in {@link TransactionsOnPostgresqlDiagnosticsTest}, which
 * needs PostgreSQL to be honest about it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfiguration.class)
class TransactionsDiagnosticsTest {

    private static WireMockServer availabilityService;

    private final MockMvc mockMvc;
    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    @Autowired
    TransactionsDiagnosticsTest(MockMvc mockMvc, JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    @BeforeAll
    static void startTheAvailabilityService() {
        availabilityService = new WireMockServer(options().dynamicPort());
        availabilityService.start();
    }

    @AfterAll
    static void stopTheAvailabilityService() {
        availabilityService.stop();
    }

    @DynamicPropertySource
    static void availabilityServiceBaseUrlAndAFreshDatabase(DynamicPropertyRegistry registry) {
        registry.add("library.availability.base-url", () -> "http://localhost:" + availabilityService.port());
        // A database the diagnostics own, freshly created per run: the
        // application is pointed at it exactly as it would be pointed at the
        // lab's H2, and nothing a neighbouring test class wrote survives.
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:lab08-" + UUID.randomUUID());
        registry.add("spring.datasource.username", () -> "sa");
    }

    /**
     * The first requirement: a borrow is a Loan row the database holds. The
     * Copy the diagnostics write into the database is the Copy the request
     * names by its barcode, and when the borrow succeeds the library can
     * point at the Loan row it kept - in the loan table the lab adds its own
     * migration for, with the vocabulary in the column names.
     */
    @Test
    void aBorrowIsARowTheDatabaseHolds() throws Exception {
        String barcode = writeACopyOnTheShelfOfARegisteredTitle();

        MockHttpServletResponse created = borrow(aMemberNobodyKnowsAbout(), barcode);

        assertThat(created.getStatus())
                .as(
                        "A borrow of a Copy the database holds and the availability service says is "
                                + "on the shelf was not a created loan (%s): the Loan feature's happy "
                                + "path must keep working while its store moves - nothing in this lab "
                                + "asks to rebuild the boundary of the web stack.",
                        created.getStatus())
                .isEqualTo(201);

        assertThat(loanRowsFor(barcode))
                .as("A borrow left no Loan row the database holds: the Loan feature's store is "
                        + "still the compiled List the LoanRepository comment has been promising "
                        + "to replace since M05, so a restart loses every loan and nothing "
                        + "would reach a transaction. Move the store to the database: a loan "
                        + "table named by a migration this lab adds (a V002__ file under "
                        + "src/main/resources/db/migration, the way M05's migration owns title "
                        + "and copy), a repository whose columns carry the vocabulary "
                        + "(member_id, copy_id), and a borrow that writes the row.")
                .isEqualTo(1);
    }

    /**
     * The second requirement: a second borrow of one Copy is a refusal, not
     * a second loan. The Copy the first borrow took is no longer an
     * Available copy, so the second request hears the refusal in the M06
     * words - and the database ends with one Loan row and one marked Copy,
     * whatever the availability service said while the two requests ran.
     */
    @Test
    void aSecondBorrowOfTheSameCopyIsARefusalNotASecondLoan() throws Exception {
        String barcode = writeACopyOnTheShelfOfARegisteredTitle();
        String member = aMemberNobodyKnowsAbout();

        MockHttpServletResponse first = borrow(member, barcode);
        assertThat(first.getStatus())
                .as(
                        "The first borrow of a Copy on the shelf was not a created loan (%s): the "
                                + "refusal is the second borrow's answer, and the first must still be "
                                + "the success case.",
                        first.getStatus())
                .isEqualTo(201);

        MockHttpServletResponse second = borrow(aMemberNobodyKnowsAbout(), barcode);

        assertThat(second.getStatus())
                .as(
                        "A second borrow of one Copy was lent a second loan (%s): the Copy has no "
                                + "guard its calls check, so repeating the request lends the same Copy "
                                + "again. Make the guard one statement "
                                + "(update copy set on_loan = true where id = :copyId and on_loan = false): "
                                + "it matches only the row whose flag is still false, so a second borrow "
                                + "of a taken Copy finds nothing to change - and the 0 answer is the "
                                + "refusal the M06 handler translates into a 409 problem detail.",
                        second.getStatus())
                .isEqualTo(409);
        assertThat(contentTypeOf(second).isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .as(
                        "The second borrow was not refused as a problem detail: a bare %s with the "
                                + "default error body is not the requirement - the refusal is answered "
                                + "as application/problem+json.",
                        second.getStatus())
                .isTrue();
        assertThat(titleOf(second))
                .as("The second borrow's refusal does not say the availability words. The M06 "
                        + "handler answers a copy that is not an Available copy with the 409 "
                        + "problem detail titled 'Copy is not on the shelf' - the refusal the "
                        + "loser of the race gets, reached after the availability service had "
                        + "already said yes.")
                .isEqualTo("Copy is not on the shelf");

        assertThat(loanRowsFor(barcode))
                .as("The refused borrow left the database with the wrong number of Loan rows for "
                        + "the Copy: one Loan per Copy is the rule, so the second request "
                        + "writes nothing at all - not a second loan, not a record to clean up "
                        + "afterward.")
                .isEqualTo(1);
        assertThat(copyIsMarkedOnLoan(barcode))
                .as("The Copy was never marked as taken: the guard is not the availability "
                        + "service's word or the Loan row's existence, it is the one update "
                        + "that flips the Copy's flag and refuses to match a taken row.")
                .isTrue();
    }

    private MockHttpServletResponse borrow(String memberId, String copyId) throws Exception {
        return mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"" + memberId + "\",\"copyId\":\"" + copyId + "\"}"))
                .andReturn()
                .getResponse();
    }

    /**
     * A member id no other test can have a loan for: the Active-loan state
     * the library holds is per member, and the diagnostics borrow under ids
     * a neighbouring run cannot have filled up.
     */
    private String aMemberNobodyKnowsAbout() {
        return "m-" + UUID.randomUUID();
    }

    /**
     * One Title with one Copy on the shelf, written straight into the
     * database - the row the borrow must be kept or refused against.
     */
    private String writeACopyOnTheShelfOfARegisteredTitle() {
        String isbn = "isbn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String barcode = "bc-" + UUID.randomUUID();
        jdbcClient
                .sql("insert into title (name, author, isbn) values (:name, :author, :isbn)")
                .param("name", "The Dispossessed")
                .param("author", "Ursula K. Le Guin")
                .param("isbn", isbn)
                .update();
        Long titleId = jdbcClient
                .sql("select id from title where isbn = :isbn")
                .param("isbn", isbn)
                .query(Long.class)
                .single();
        jdbcClient
                .sql("insert into copy (title_id, barcode, on_loan) values (:titleId, :barcode, :onLoan)")
                .param("titleId", titleId)
                .param("barcode", barcode)
                // An Available copy: not currently on loan.
                .param("onLoan", Boolean.FALSE)
                .update();
        availabilityService.stubFor(get(urlPathMatching("/api/availability/" + barcode))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + barcode + "\",\"available\":true}")));
        return barcode;
    }

    /**
     * The number of Loan rows the database holds for the Copy whose barcode
     * is named, and a failure that names what is missing when the loan table
     * is not there at all.
     */
    private long loanRowsFor(String copyBarcode) {
        try {
            return jdbcClient
                    .sql("""
                        select count(*) from loan
                        where copy_id = (select id from copy where barcode = :barcode)
                        """)
                    .param("barcode", copyBarcode)
                    .query(Long.class)
                    .single();
        } catch (BadSqlGrammarException noLoanTable) {
            fail("""
                    The borrow did not reach the database: there is no loan table, so no Loan \
                    row can be held anywhere but in compiled code. The lab adds the loan table \
                    with its own migration (a V002__ file under db/migration, columns \
                    member_id and copy_id) and moves the loan feature's calls to a \
                    repository over it.""", noLoanTable);
        }
        return 0;
    }

    /**
     * Whether the Copy sits in the database still marked as on loan, after
     * the borrow took it.
     */
    private boolean copyIsMarkedOnLoan(String copyBarcode) {
        return jdbcClient
                .sql("select on_loan from copy where barcode = :barcode")
                .param("barcode", copyBarcode)
                .query(Boolean.class)
                .single();
    }

    private MediaType contentTypeOf(MockHttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
    }

    private String titleOf(MockHttpServletResponse response) throws Exception {
        return problemBody(response).path("title").asString();
    }

    private JsonNode problemBody(MockHttpServletResponse response) throws Exception {
        return jsonMapper.readTree(response.getContentAsString());
    }
}
