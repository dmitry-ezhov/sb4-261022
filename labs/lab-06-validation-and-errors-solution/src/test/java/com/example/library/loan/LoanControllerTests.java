package com.example.library.loan;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.library.FixedClockConfiguration;

/**
 * What the loan API answers, observed at the boundary: the status, the media
 * type, the shape of the payload, the words of a refusal. Not how the loan
 * feature is arranged - a reader of these tests learns what a client of the
 * library sees.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfiguration.class)
class LoanControllerTests {

    private static final String COPY_ID = "c-001";

    private static WireMockServer availabilityService;

    private final MockMvc mockMvc;

    @Autowired
    LoanControllerTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
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

    @Test
    void aLoanIsCreatedForTheBorrowedCopy() throws Exception {
        availabilityService.stubFor(get(urlPathEqualTo("/api/availability/" + COPY_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + COPY_ID + "\",\"available\":true}")));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"copyId\":\"" + COPY_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("m-001"))
                .andExpect(jsonPath("$.copyId").value(COPY_ID))
                .andExpect(jsonPath("$.borrowedOn").value("2026-01-05"))
                .andExpect(jsonPath("$.dueOn").value("2026-01-19"));
    }

    /**
     * A body that does not validate is refused as a problem detail told in
     * the loan feature's words - never the reservation's, whose title this
     * handler once wore for both - and it names the field that failed, the
     * part a client actually needs.
     */
    @Test
    void anInvalidBorrowIsRefusedInTheLoansWords() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"   \",\"copyId\":\"c-999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid loan request"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("member"))));
    }

    /**
     * The Loan limit is the domain's rule: a Member who holds the limit's
     * count of Active loans is refused a further one with {@code 409}, and
     * the refusal spells the rule - not a beige number in a field error, and
     * not a request the client could fix by rewriting it.
     */
    @Test
    void aMemberAtTheLoanLimitIsRefusedAFurtherLoan() throws Exception {
        onTheShelf("c-101", "c-102", "c-103", "c-104");
        // A member this test owns, so no neighbouring test class can hold
        // loans for them already.
        String memberId = "m-" + UUID.randomUUID();

        borrow(memberId, "c-101").andExpect(status().isCreated());
        borrow(memberId, "c-102").andExpect(status().isCreated());
        borrow(memberId, "c-103").andExpect(status().isCreated());

        borrow(memberId, "c-104")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Loan limit reached"))
                .andExpect(jsonPath("$.detail").value("A member may hold 3 Active loans at once"));
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

    private ResultActions borrow(String memberId, String copyId) throws Exception {
        return mockMvc.perform(post("/api/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"" + memberId + "\",\"copyId\":\"" + copyId + "\"}"));
    }
}
