package com.example.library.reservation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The reservation answer's shape, asserted at the webmvc slice alone.
 *
 * <p>The reservation controller has no collaborators to substitute - it reads
 * the request and answers - so this slice has no {@code @MockitoBean} field,
 * and that is the point of the record: the class boots only the webmvc
 * slice's machinery, pays no full context, and its answers still say the
 * reservation feature's words. The Loan limit, the borrowing, the
 * persistence: none of it is the reservation boundary's business, and none
 * of it is booted to prove the boundary's status, media type and words.
 */
@WebMvcTest(ReservationController.class)
class ReservationControllerTests {

    private final MockMvc mockMvc;

    @Autowired
    ReservationControllerTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * A request that validates is a created Reservation, in the payload's
     * shape the M04 lab fixed.
     */
    @Test
    void aReservationRequestThatValidatesIsCreated() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"titleId\":\"t-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("m-001"))
                .andExpect(jsonPath("$.titleId").value("t-001"));
    }

    /**
     * A body that does not validate keeps the reservation's words: the loan
     * feature's refusal is titled by the loan feature, and this refusal is
     * not the loan's business.
     */
    @Test
    void aReservationRequestThatDoesNotValidateSpeaksTheReservationsWords() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"\",\"titleId\":\"t-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid reservation request"))
                .andExpect(jsonPath("$.errors", hasItem(containsString("member"))));
    }
}
