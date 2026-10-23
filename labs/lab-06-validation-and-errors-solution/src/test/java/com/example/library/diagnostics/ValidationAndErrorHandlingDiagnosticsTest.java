package com.example.library.diagnostics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.library.FixedClockConfiguration;

/**
 * Diagnostics for the M06 lab.
 *
 * <p>The application was left with two defects. The handler that answers an
 * invalid body is shared with the reservation feature, so a borrow request's
 * failure says the reservation's words; and the Loan limit bound in M03 is
 * read by nobody, so any member can borrow forever. These tests state what
 * must happen instead. They are excluded from the default build (the parent
 * POM excludes {@code **&#47;diagnostics/*Test.java}) so that the start module
 * stays green while the defects are still present. Run them with:
 *
 * <pre>./mvnw -f labs -pl lab-06-validation-and-errors-solution verify -Pdiagnostics</pre>
 *
 * <p>Diagnostics are asserted in CI with the solutions-only profile: the same
 * tests, run only against the reference implementations by
 * {@code ./mvnw -f labs verify -Pdiagnostics-solutions}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfiguration.class)
class ValidationAndErrorHandlingDiagnosticsTest {

    private static WireMockServer availabilityService;

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    @Autowired
    ValidationAndErrorHandlingDiagnosticsTest(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
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
    static void availabilityServiceBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("library.availability.base-url", () -> "http://localhost:" + availabilityService.port());
    }

    /**
     * The first requirement: a borrow request that does not validate is
     * refused with a {@code 400} problem detail that names the loan's failure
     * - not the reservation's, and not the default error body.
     */
    @Test
    void anInvalidBorrowSpeaksTheLoansWords() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        // A borrow request that does not validate: its member
                        // field is blank.
                        .content("{\"memberId\":\"   \",\"copyId\":\"c-999\"}"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("A borrow request that does not validate was not refused with 400: the shape "
                        + "of the body is the boundary's to check, and a blank member field "
                        + "fails that check before any loan is made.")
                .isEqualTo(400);
        assertThat(mediaTypeOf(response).isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .as("The refusal was not served as a problem detail: a bare 400 with the default "
                        + "error body is not the requirement - that body carries a timestamp, an "
                        + "error string and no field errors, and the part a client actually "
                        + "needs is missing.")
                .isTrue();

        var problem = jsonMapper.readTree(response.getContentAsString());
        assertThat(problem.isObject())
                .as("The refusal was not a problem detail as RFC 9457 serves one.")
                .isTrue();
        assertThat(valueOf(problem, "title"))
                .as("A borrow request's failure says the reservation's words. The handler that "
                        + "answers an invalid body is shared with the reservation feature, and "
                        + "its title still is 'Invalid reservation request': a borrow request "
                        + "that does not validate must be refused with a 400 problem detail "
                        + "titled as the loan's own failure (for example 'Invalid loan "
                        + "request'), in the loan feature's words - never the reservation's "
                        + "title, and never the default error body.")
                .doesNotContainIgnoringCase("reservation")
                .containsIgnoringCase("loan");
        assertThat(fieldErrorsOf(problem))
                .as("The problem detail does not list the failed field: a client refused on the "
                        + "boundary has to know which member of the request did not validate - "
                        + "the field errors belong in the problem detail.")
                .anySatisfy(fieldError -> assertThat(fieldError).containsIgnoringCase("member"));
    }

    /**
     * The second requirement: a member at the Loan limit asking for a further
     * Loan is refused with a {@code 409} problem detail - the domain's
     * exception, raised by the loan feature, translated by the handler, and
     * never a constraint on the request record.
     */
    @Test
    void aMemberAtTheLoanLimitHasNoFurtherLoan() throws Exception {
        // Every copy the diagnostics name is on the shelf, so the availability
        // service cannot be the reason for a refusal.
        availabilityService.stubFor(get(urlPathMatching("/api/availability/.+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"c-201\",\"available\":true}")));
        // A member the diagnostics own, so neither a neighbouring run nor a
        // neighbouring test class can hold loans for them already.
        String memberId = "m-" + UUID.randomUUID();

        fillTheMemberUpToTheLimit(memberId);

        MockHttpServletResponse further = borrow(memberId, "c-204");

        assertThat(further.getStatus())
                .as("A member at the Loan limit was lent a further loan: the application.yml "
                        + "binds the limit (library.loan.limit) and nothing reads it, so the "
                        + "library has no limit at all and any member can borrow forever. The "
                        + "Loan limit is a domain rule, not a request constraint: the loan "
                        + "feature must count the member's Active loans against the bound "
                        + "limit where the borrowing happens, refuse the fourth with its own "
                        + "domain exception, and the handler must translate that exception "
                        + "into a 409 problem detail. No constraint on the request record can "
                        + "own this - the request for a fourth copy is perfectly well formed, "
                        + "so the client has nothing to fix, and no constant constraint can "
                        + "read the configured limit.")
                .isEqualTo(409);
        assertThat(mediaTypeOf(further).isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .as("The refusal for the further loan was not a problem detail: the rule's "
                        + "refusal is answered as application/problem+json, not as the default "
                        + "error body, a 500 or a success payload.")
                .isTrue();

        var problem = jsonMapper.readTree(further.getContentAsString());
        assertThat(problem.isObject())
                .as("The refusal for the further loan was not a problem detail as RFC 9457 " + "serves one.")
                .isTrue();
        assertThat(valueOf(problem, "title"))
                .as("The refusal for a member at the Loan limit does not say the rule's words. "
                        + "A member who is at the Loan limit must be answered with a 409 "
                        + "problem detail titled in the glossary's terms (for example 'Loan "
                        + "limit reached'): the refusal is the Loan limit's, named for the "
                        + "rule - not a beige number in a field error, and not a failure the "
                        + "request could fix by being rewritten.")
                .containsIgnoringCase("loan")
                .containsIgnoringCase("limit");
    }

    private void fillTheMemberUpToTheLimit(String memberId) throws Exception {
        // Three on-shelf copies, borrowed one by one, so that the further loan
        // is asked for from a member who genuinely holds the limit of Active
        // loans.
        String[] copies = {"c-201", "c-202", "c-203"};
        for (String copyId : copies) {
            MockHttpServletResponse created = borrow(memberId, copyId);
            assertThat(created.getStatus())
                    .as(
                            "The diagnostics could not reach the Loan limit: borrowing a copy that "
                                    + "is on the shelf must create a loan for it (%s), before the "
                                    + "further loan can be asked for.",
                            copyId)
                    .isEqualTo(201);
        }
    }

    private MockHttpServletResponse borrow(String memberId, String copyId) throws Exception {
        return mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"" + memberId + "\",\"copyId\":\"" + copyId + "\"}"))
                .andReturn()
                .getResponse();
    }

    private MediaType mediaTypeOf(MockHttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
    }

    private String valueOf(JsonNode problem, String field) {
        return problem.path(field).asString();
    }

    private List<String> fieldErrorsOf(JsonNode problem) {
        List<String> fieldErrors = new ArrayList<>();
        var errors = problem.path("errors");
        for (var fieldError : errors) {
            fieldErrors.add(fieldError.asString());
        }
        return fieldErrors;
    }
}
