package com.example.library.loan;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.library.FixedClockConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfiguration.class)
class LoanControllerTests {

    private final MockMvc mockMvc;

    @Autowired
    LoanControllerTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void aLoanIsCreatedForTheBorrowedCopy() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"copyId\":\"c-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("m-001"))
                .andExpect(jsonPath("$.copyId").value("c-001"))
                .andExpect(jsonPath("$.borrowedOn").value("2026-01-05"))
                .andExpect(jsonPath("$.dueOn").value("2026-01-19"));
    }
}
