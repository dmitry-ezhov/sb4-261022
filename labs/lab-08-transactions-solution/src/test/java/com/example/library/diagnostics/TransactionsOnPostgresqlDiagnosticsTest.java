package com.example.library.diagnostics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.tomakehurst.wiremock.WireMockServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Diagnostics for the M08 lab: the guarantee two borrows cannot both win,
 * proved where it can be honest - against PostgreSQL.
 *
 * <p>Two Members race for the one Available copy of a Title. On H2 the rig
 * happily serialises everything, so a green there says only that the code
 * declares a guard; this class asks the second engine whether concurrent
 * borrows of one Copy end with one Loan. It is excluded from the default
 * build (the parent POM excludes {@code **&#47;diagnostics/*Test.java}) and, like
 * the M05 diagnostics, gated on a Docker daemon: a machine without one skips
 * this class, and a skip is a message about the environment, never a pass.
 * Run the diagnostics with:
 *
 * <pre>./mvnw -f labs -pl lab-08-transactions-start verify -Pdiagnostics</pre>
 *
 * <p>Diagnostics are asserted in CI with the solutions-only profile: the same
 * tests, run only against the reference implementations by
 * {@code ./mvnw -f labs verify -Pdiagnostics-solutions}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfDockerAvailable
class TransactionsOnPostgresqlDiagnosticsTest {

    // PostgreSQL 17, the version the M05 doc checked its migration against.
    @Container
    @ServiceConnection
    private static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    private static WireMockServer availabilityService;

    private final MockMvc mockMvc;
    private final JdbcClient jdbcClient;

    @Autowired
    TransactionsOnPostgresqlDiagnosticsTest(MockMvc mockMvc, JdbcClient jdbcClient) {
        this.mockMvc = mockMvc;
        this.jdbcClient = jdbcClient;
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
    static void availabilityServiceBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("library.availability.base-url", () -> "http://localhost:" + availabilityService.port());
    }

    /**
     * The requirement the lab exists to prove: two concurrent borrows, one
     * Copy, two Members - the winner reports its loan, the loser hears the
     * refusal in the M06 words, and the database ends with one Loan row for
     * that Copy, not two.
     */
    @Test
    void twoConcurrentBorrowsCannotTakeTheSameCopy() throws Exception {
        String barcode = writeACopyOnTheShelfOfARegisteredTitle();
        Callable<MockHttpServletResponse> firstRequest = () -> borrow("m-race-first", barcode);
        Callable<MockHttpServletResponse> secondRequest = () -> borrow("m-race-second", barcode);

        ExecutorService twoBorrowers = Executors.newFixedThreadPool(2);
        MockHttpServletResponse first;
        MockHttpServletResponse second;
        try {
            Future<MockHttpServletResponse> firstAnswer = twoBorrowers.submit(firstRequest);
            Future<MockHttpServletResponse> secondAnswer = twoBorrowers.submit(secondRequest);
            first = answerOf(firstAnswer);
            second = answerOf(secondAnswer);
        } finally {
            twoBorrowers.shutdownNow();
        }
        List<Integer> statuses = List.of(first.getStatus(), second.getStatus());

        assertThat(statuses.stream().filter(status -> status == 201).count())
                .as("Two borrows of one Copy both lent it: the Loan feature's decisions are made "
                        + "in compiled code where two transactions read the same free Copy. "
                        + "Borrowing must be transactional - the borrow is the boundary "
                        + "(@Transactional on the use case, the Loan limit's count, the Copy's "
                        + "guard and the Loan row inside it, the availability call outside) - "
                        + "and the Copy's guard must be one statement "
                        + "(update copy set on_loan = true where id = :copyId and on_loan = false), "
                        + "whose 0 answer refuses the loser instead of lending the Copy twice.")
                .isEqualTo(1);

        MockHttpServletResponse refusal = first.getStatus() == 409 ? first : second;
        assertThat(refusal.getStatus())
                .as(
                        "Neither of the two borrows was refused with the loser's 409: the winner and "
                                + "the loser must be told apart by the guard's answer - 1 commits the "
                                + "Loan, 0 throws the refusal.",
                        refusal.getStatus())
                .isEqualTo(409);
        assertThat(mediaTypeOf(refusal).isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .as("The losing borrow was not refused as a problem detail: the refusal of a "
                        + "Copy that is not an Available copy is answered as "
                        + "application/problem+json, not as the default error body or a 500.")
                .isTrue();
        assertThat(refusal.getContentAsString())
                .as("The losing borrow's refusal does not say the availability words: the M06 "
                        + "handler answers a copy that is not on the shelf with the 409 problem "
                        + "detail titled 'Copy is not on the shelf' - the same words a third "
                        + "borrower would hear later.")
                .contains("Copy is not on the shelf");

        assertThat(loanRowsFor(barcode))
                .as("Two borrows of one Copy left the database with the wrong number of Loan "
                        + "rows: one Loan per Copy is the rule the borrow's transaction must "
                        + "hold even under contention.")
                .isEqualTo(1);
        assertThat(copyIsMarkedOnLoan(barcode))
                .as("The Copy is still marked as on the shelf after two borrows: the winner's "
                        + "boundary must commit the marking in the same statement that took it, "
                        + "so the next borrower's guard sees the Copy taken.")
                .isTrue();
    }

    /**
     * A borrow request's answer, dispatched on this request's own thread so
     * that two borrows genuinely run at once.
     */
    private MockHttpServletResponse borrow(String memberId, String copyId) throws Exception {
        return mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"" + memberId + "\",\"copyId\":\"" + copyId + "\"}"))
                .andReturn()
                .getResponse();
    }

    /**
     * The answer of one borrowing thread, and a failure that names what a
     * never-returning borrow means: a borrow that pins a pooled connection
     * without releasing it keeps every request queueing behind it.
     */
    private MockHttpServletResponse answerOf(Future<MockHttpServletResponse> answer) {
        try {
            return answer.get(15, TimeUnit.SECONDS);
        } catch (TimeoutException neverFinished) {
            fail("""
                    A borrow never returned: a boundary that pins a pooled connection without \
                    releasing it queues every request behind it until the connection \
                    timeout. The boundary spans the statements the Loan feature needs and \
                    nothing else hangs inside it.""", neverFinished);
        } catch (Exception failed) {
            fail("A borrow did not even reach an answer: %s: %s", failed, failed.getMessage());
        }
        throw new IllegalStateException("unreachable: the fail above always throws");
    }

    private MediaType mediaTypeOf(MockHttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
    }

    /**
     * One Title with one Copy on the shelf, written straight into the
     * container's PostgreSQL - the row the two borrows race against.
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
                    The borrows did not reach the database: there is no loan table, so no Loan \
                    row can be held anywhere but in compiled code. The lab adds the loan table \
                    with its own migration (a V002__ file under db/migration, columns \
                    member_id and copy_id) and moves the loan feature's calls to a \
                    repository over it.""", noLoanTable);
        }
        return 0;
    }

    /** Whether the Copy sits in the database still marked as on loan. */
    private boolean copyIsMarkedOnLoan(String copyBarcode) {
        return jdbcClient
                .sql("select on_loan from copy where barcode = :barcode")
                .param("barcode", copyBarcode)
                .query(Boolean.class)
                .single();
    }
}
