package com.example.library.loan;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
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
 * The loan feature's wiring, asserted once - the one full-context MockMvc
 * home in the suite, and the only class that earns it.
 *
 * <p>The subject here is the seam level, which no slice can prove: the Loan
 * limit bound in M03 is read by the real {@code LoanService} and counted by
 * the real loan store; the availability client is the real HTTP Service
 * client, pointed by its real {@code library.availability.base-url}
 * configuration at a WireMock server and translating the service's statuses
 * itself; and every borrow persists through the real repository, which is
 * why the limit's refusal can be reached through the boundary alone. Nothing
 * is stubbed with a bean substitute: a wiring test's claims are about the
 * wiring, and a suite that repeated this context for boundary answers the
 * slices already prove would pay the full boot every time for nothing - the
 * resident full-context home is this class and no other. The cost of that
 * boot is measured, not assumed: for this application's predecessor, the
 * M07 doc records 2.7 s for the full context against 1.4 s for the webmvc
 * slice, same classes, same machine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfiguration.class)
class LoanWiringTests {

    private static final String COPY_ID = "c-001";

    private static WireMockServer availabilityService;

    private final MockMvc mockMvc;

    @Autowired
    LoanWiringTests(MockMvc mockMvc) {
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

    /**
     * A borrow arrives as a created Loan, dated by the bound configuration -
     * fourteen days from the clock - answered by the real service and handed
     * to the real loan store.
     */
    @Test
    void aBorrowIsAnsweredByTheRealServiceAndDatedByTheBoundPeriod() throws Exception {
        availabilityService.stubFor(get(urlPathEqualTo("/api/availability/" + COPY_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + COPY_ID + "\",\"available\":true}")));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-wiring\",\"copyId\":\"" + COPY_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("m-wiring"))
                .andExpect(jsonPath("$.copyId").value(COPY_ID))
                .andExpect(jsonPath("$.borrowedOn").value("2026-01-05"))
                .andExpect(jsonPath("$.dueOn").value("2026-01-19"));
    }

    /**
     * The Loan limit is the domain's rule, working end to end: a Member who
     * holds the limit's count of Active loans - the real store holding all
     * three, kept only because the borrows genuinely persisted - is refused a
     * further one with {@code 409}, and the refusal spells the rule - not a
     * beige number in a field error, and not a request the client could fix
     * by rewriting it. This is the assertion the slice omitted on purpose:
     * the limit is not the web tier's answer, so it is proved only here.
     */
    @Test
    void aMemberAtTheLoanLimitIsRefusedThroughTheRealStore() throws Exception {
        onTheShelf("c-101", "c-102", "c-103", "c-104");
        // A member this test owns, so no neighbouring test class can hold
        // loans for them already.
        String memberId = "m-wiring-" + UUID.randomUUID();

        borrow(memberId, "c-101").andExpect(status().isCreated());
        borrow(memberId, "c-102").andExpect(status().isCreated());
        borrow(memberId, "c-103").andExpect(status().isCreated());

        borrow(memberId, "c-104")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Loan limit reached"))
                .andExpect(jsonPath("$.detail").value("A member may hold 3 Active loans at once"));
    }

    /**
     * The real HTTP Service client reads its base-url configuration and the
     * service's answer: a copy off the shelf is refused with the feature's
     * {@code 409} problem detail, before any Loan is made.
     */
    @Test
    void aCopyOffTheShelfIsRefusedThroughTheRealClient() throws Exception {
        availabilityService.stubFor(get(urlPathEqualTo("/api/availability/c-002"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"c-002\",\"available\":false}")));

        borrow("m-wiring", "c-002")
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
        availabilityService.stubFor(get(urlPathEqualTo("/api/availability/c-404"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not known\"}")));

        borrow("m-wiring", "c-404")
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy is not known to the availability service"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
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
