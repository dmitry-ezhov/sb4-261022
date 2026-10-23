package com.example.library.loan;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.example.library.FixedClockConfiguration;

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
}
