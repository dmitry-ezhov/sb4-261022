package com.example.library.reservation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * What the reservation API answers, observed at the boundary. An invalid
 * reservation keeps the reservation's words: the loan feature's refusal is
 * titled by the loan feature, and this refusal is not the loan's business.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerTests {

    private final MockMvc mockMvc;

    @Autowired
    ReservationControllerTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void validReservationIsAccepted() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"titleId\":\"t-001\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidReservationIsRejected() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"\",\"titleId\":\"t-001\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anInvalidReservationSpeaksTheReservationsWords() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"\",\"titleId\":\"t-001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid reservation request"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }
}
