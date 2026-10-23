package com.example.library.loan;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

import com.example.library.availability.Availability;
import com.example.library.availability.AvailabilityClient;

/**
 * The loan answer's shape, asserted at the webmvc slice alone.
 *
 * <p>The context this tier earns and why: the answers here - the route's
 * status, the validation's refusal, the media type, the payload's fields, the
 * problem detail's words - are all decided by the web tier: the controller,
 * the body validation, the Jackson 3 serialization, the handler. None of them
 * needs a {@code LoanRepository} or a real clock, so paying the full
 * application's boot (the Flyway migration, the availability client's wiring
 * and the store it points at) for one assertion about a status code would
 * prove nothing the slice does not. {@code @WebMvcTest} boots the controller
 * it names and the web machinery around it, and {@code @MockitoBean} puts one
 * mock in each collaborator's place - the framework's replacement for the
 * removed {@code @MockBean}. What the substitutes cannot prove (the real
 * service, the bound limit, the real HTTP client) is not asserted here: it is
 * the wiring home's one subject.
 */
@WebMvcTest(LoanController.class)
class LoanControllerTests {

    private final MockMvc mockMvc;

    @MockitoBean
    private LoanService loans;

    @MockitoBean
    private AvailabilityClient availability;

    @Autowired
    LoanControllerTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * A body that validates is a created Loan, in the payload's shape - this
     * far the controller decides alone.
     */
    @Test
    void aBorrowRequestThatValidatesIsCreated() throws Exception {
        when(availability.availability("c-001")).thenReturn(new Availability("c-001", true));
        when(loans.borrow("m-001", "c-001"))
                .thenReturn(new Loan(
                        "l-001", "m-001", "c-001", LocalDate.parse("2026-01-05"), LocalDate.parse("2026-01-19")));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"copyId\":\"c-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("m-001"))
                .andExpect(jsonPath("$.copyId").value("c-001"))
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
    void aBorrowRequestThatDoesNotValidateIsRefusedInTheLoansWords() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"   \",\"copyId\":\"c-999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid loan request"))
                .andExpect(jsonPath("$.detail").value("The request body is not valid."))
                .andExpect(jsonPath("$.errors", hasItem(containsString("member"))));
    }

    /**
     * A copy the availability service says is off the shelf is refused with
     * {@code 409} and a problem detail that names the copy - the boundary's
     * translation of the availability answer, before any service is consulted.
     */
    @Test
    void aCopyOffTheShelfIsRefusedAsAProblemDetail() throws Exception {
        when(availability.availability("c-002")).thenReturn(new Availability("c-002", false));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"copyId\":\"c-002\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy is not on the shelf"))
                .andExpect(jsonPath("$.detail").value("Copy c-002 is not on the shelf"));
    }

    /**
     * A copy the availability service does not know is refused with a problem
     * detail of its own: the service's 404 reaches the boundary as a typed
     * exception, and the handler answers in words a client can act on.
     */
    @Test
    void aCopyTheServiceDoesNotKnowIsRefusedAsUnknown() throws Exception {
        when(availability.availability("c-404"))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatusCode.valueOf(404), "not known", null, new byte[0], null));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"copyId\":\"c-404\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy is not known to the availability service"));
    }
}
