package com.example.library.loan;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.library.FixedClockConfiguration;

/**
 * The loan feature's wiring, asserted once - the one full-context MockMvc
 * home in the suite, and the only class that earns it.
 *
 * <p>The subject here is the seam level, which no slice can prove: the Loan
 * limit bound in M03 is read by the real {@code LoanService} and counted by
 * the real loan store over the loan table; the Copy's guard and the Loan row
 * happen inside the real {@code @Transactional} boundary; the availability
 * client is the real HTTP Service client, pointed by its real
 * {@code library.availability.base-url} configuration at a WireMock server
 * and translating the service's statuses itself. Nothing is stubbed with a
 * bean substitute: a wiring test's claims are about the wiring. And the
 * caller is real wiring too: every borrow rides the real chains as a
 * Member's Basic-authenticated call - the credential the store holds, the
 * role rule that answers it, the {@code Authentication} the controller
 * passes in, and the use case's {@code @PreAuthorize} second lock all run -
 * so a borrow here is issued in that Member's own name, and every claim
 * about the Loan limit is a claim about one Member's count. A
 * database-backed borrow is only lent a Copy the database holds, so every
 * borrow writes the Copy row it races for first, the way the diagnostics
 * model it, and the Copy's on-loan flag and the loan table are asserted
 * where they live: in the database. Each test clears that Member's loan
 * rows, so its count starts at zero however its neighbours left it, and each
 * test's Title is fresh so the metric's tag is its own.
 *
 * <p>The security answers themselves - the statuses an unauthenticated
 * caller and a wrong-role principal earn - live in the loan slice's security
 * tests and the diagnostics, which do not restate here: this class's
 * business is the use case behind an authenticated Member's borrow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfiguration.class)
class LoanWiringTests {

    /** The Member whose credentials the borrows ride, per the user store. */
    private static final String MEMBER_NAME = "ada";

    private static final String MEMBER_PASSWORD = "member-password";

    private static WireMockServer availabilityService;

    private final MockMvc mockMvc;
    private final JdbcClient jdbcClient;
    private final MeterRegistry metrics;

    @Autowired
    LoanWiringTests(MockMvc mockMvc, JdbcClient jdbcClient, MeterRegistry metrics) {
        this.mockMvc = mockMvc;
        this.jdbcClient = jdbcClient;
        this.metrics = metrics;
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
     * The limit counts Active loans and reads no other neighbourhood's
     * state: the borrowing Member's rows are cleared before every test, so
     * the limit each test drives is the count its own borrows made, not a
     * sum another test's Member had already spent.
     */
    @BeforeEach
    void theBorrowingMemberStartsWithNoActiveLoans() {
        jdbcClient
                .sql("delete from loan where member_id = :memberId")
                .param("memberId", MEMBER_NAME)
                .update();
    }

    /**
     * A borrow arrives as a created Loan, dated by the bound configuration -
     * fourteen days from the clock - answered by the real service, mapped at
     * the edge from the Copy's barcode to the row, and taken inside the real
     * boundary: the Loan row lands in the authenticated Member's name, and
     * the Copy is marked, one commit.
     */
    @Test
    void aBorrowIsAnsweredByTheRealServiceDatedByTheBoundPeriodAndKeptByTheDatabase() throws Exception {
        String barcode = writeACopyOnTheShelfOfARegisteredTitle();
        onTheShelf(barcode);

        borrow(barcode)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(MEMBER_NAME))
                .andExpect(jsonPath("$.copyId").value(barcode))
                .andExpect(jsonPath("$.borrowedOn").value("2026-01-05"))
                .andExpect(jsonPath("$.dueOn").value("2026-01-19"));

        assertThat(loanRowsFor(barcode))
                .as("The borrow's boundary did not write the Loan row it answered with, so the "
                        + "API says created but the database holds nothing.")
                .containsExactly(1L);
        assertThat(copyIsMarkedOnLoan(barcode))
                .as("The borrow's boundary did not mark the Copy it lent, so the next borrower's "
                        + "guard would still match an on-the-shelf row.")
                .isTrue();
    }

    /**
     * A second borrow of one Copy is a refusal, not a second Loan - the
     * H2-visible half of the lab's guarantee: the loser is answered in the
     * M06 words, the database ends with one Loan row for the Copy, and the
     * Copy stays marked.
     */
    @Test
    void aSecondBorrowOfTheSameCopyIsARefusalAndNotASecondLoan() throws Exception {
        String barcode = writeACopyOnTheShelfOfARegisteredTitle();
        onTheShelf(barcode);

        borrow(barcode).andExpect(status().isCreated());
        borrow(barcode)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy is not on the shelf"));

        assertThat(loanRowsFor(barcode))
                .as("The refused borrow was lent a second Loan, or the winner's was lost: one "
                        + "Loan per Copy is the rule the database must end with.")
                .containsExactly(1L);
        assertThat(copyIsMarkedOnLoan(barcode))
                .as("The Copy was not marked by the winner's guard, so the refusal's database "
                        + "state is not the one the requirement describes.")
                .isTrue();
    }

    /**
     * The Loan limit is the domain's rule, working end to end and for one
     * Member: a Member who holds the limit's count of Active loans - the
     * real store holding all three Loan rows in that Member's name, counted
     * by the boundary's own query - is refused a further one with
     * {@code 409}, and the refusal spells the rule - not a beige number in a
     * field error, and not a request the client could fix by rewriting it.
     * This is the assertion the slice omitted on purpose: the limit is not
     * the web tier's answer, so it is proved only here.
     */
    @Test
    void aMemberAtTheLoanLimitIsRefusedThroughTheRealStore() throws Exception {
        String first = writeACopyOnTheShelfOfARegisteredTitle();
        String second = writeACopyOnTheShelfOfARegisteredTitle();
        String third = writeACopyOnTheShelfOfARegisteredTitle();
        String fourth = writeACopyOnTheShelfOfARegisteredTitle();
        onTheShelf(first, second, third, fourth);

        borrow(first).andExpect(status().isCreated());
        borrow(second).andExpect(status().isCreated());
        borrow(third).andExpect(status().isCreated());

        borrow(fourth)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Loan limit reached"))
                .andExpect(jsonPath("$.detail").value("A member may hold 3 Active loans at once"));

        assertThat(loanRowsFor(first, second, third))
                .as("The limit's refusal wrote a fourth Loan row it should have rolled back, or "
                        + "the three borrows never reached the database.")
                .containsExactly(1L, 1L, 1L);
    }

    /**
     * The real HTTP Service client reads its base-url configuration and the
     * service's answer: a copy off the shelf is refused with the feature's
     * {@code 409} problem detail, before any mapping or any boundary runs.
     */
    @Test
    void aCopyOffTheShelfIsRefusedThroughTheRealClient() throws Exception {
        String barcode = writeACopyOnTheShelfOfARegisteredTitle();
        availabilityService.stubFor(get(urlPathEqualTo("/api/availability/" + barcode))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + barcode + "\",\"available\":false}")));

        borrow(barcode)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy is not on the shelf"));
    }

    /**
     * The client turns a service's {@code 404} into a typed exception, and
     * the wiring answers it with the words a client can act on.
     */
    @Test
    void aCopyTheServiceDoesNotKnowIsRefusedAsUnknown() throws Exception {
        String barcode = writeACopyOnTheShelfOfARegisteredTitle();
        availabilityService.stubFor(get(urlPathEqualTo("/api/availability/" + barcode))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not known\"}")));

        borrow(barcode)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy is not known to the availability service"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    /**
     * The observability the M09 lab adds, at the registry the lab's own read
     * answers through: a borrow — the same boundary the tests above drive —
     * leaves a count behind under {@code library.loans.issued}, tagged with
     * the Title the Loan was issued against. Each test's Title is fresh, so
     * the count read back is exactly one: one borrow of one Copy on the
     * shelf left one issuance behind, not a request counter, not a view of
     * {@code http.server.requests}. The full socket half of this claim — the
     * counters appearing on the management port's
     * {@code /actuator/metrics/library.loans.issued} — is the diagnostics',
     * which MockMvc cannot reach: the management server rides its own port.
     */
    @Test
    void aBorrowLeavesOneIssuanceBehindInTheMetricsRegistry() throws Exception {
        String barcode = writeACopyOnTheShelfOfARegisteredTitle();
        onTheShelf(barcode);

        borrow(barcode).andExpect(status().isCreated());

        Long titleKey = jdbcClient
                .sql("select title_id from copy where barcode = :barcode")
                .param("barcode", barcode)
                .query(Long.class)
                .single();
        Counter issued = metrics.find("library.loans.issued")
                .tag("title", Long.toString(titleKey))
                .counter();
        assertThat(issued)
                .as("The borrow issued a Loan but left no counter behind: the increment "
                        + "belongs where the Loan row is written - the use case that issues "
                        + "the loan counts the issuance - and the counter is named in the "
                        + "glossary's words, library.loans.issued, tagged with the Title.")
                .isNotNull();
        assertThat(issued.count())
                .as("The counter carries the borrow's Title but the issuance is not in it: "
                        + "one borrow of one Copy on the shelf leaves one count behind.")
                .isEqualTo(1.0);
    }

    private void onTheShelf(String... copyIds) {
        for (String copyId : copyIds) {
            availabilityService.stubFor(get(urlPathMatching("/api/availability/" + copyId))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"copyId\":\"" + copyId + "\",\"available\":true}")));
        }
    }

    /**
     * The borrow as a Member's client types it: the Member's Basic
     * credential on the wire, a JSON body asking for the Copy - and nothing
     * naming a Member, because who is borrowing is what the wire answers.
     */
    private ResultActions borrow(String copyId) throws Exception {
        return mockMvc.perform(post("/api/loans")
                .with(httpBasic(MEMBER_NAME, MEMBER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"copyId\":\"" + copyId + "\"}"));
    }

    /**
     * One Title with one Copy on the shelf, written straight into the
     * database - the row the borrow must be kept or refused against. Every
     * barcode is owned by one test, so no neighbouring test's borrows can
     * collide with a copy this test seeds.
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
        return barcode;
    }

    /**
     * The number of Loan rows the database holds for each Copy whose barcode
     * is named: the assertion reads the store the boundary writes, not the
     * API's word for what it did.
     */
    private List<Long> loanRowsFor(String... copyBarcodes) {
        return Arrays.stream(copyBarcodes)
                .map(barcode -> jdbcClient
                        .sql("select count(*) from loan where copy_id = (select id from copy where barcode = :barcode)")
                        .param("barcode", barcode)
                        .query(Long.class)
                        .single())
                .toList();
    }

    /** Whether the Copy sits in the database marked as on loan. */
    private boolean copyIsMarkedOnLoan(String copyBarcode) {
        return jdbcClient
                .sql("select on_loan from copy where barcode = :barcode")
                .param("barcode", copyBarcode)
                .query(Boolean.class)
                .single();
    }
}
