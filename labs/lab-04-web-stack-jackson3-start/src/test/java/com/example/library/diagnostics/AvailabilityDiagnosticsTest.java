package com.example.library.diagnostics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

/**
 * Diagnostics for the M04 lab.
 *
 * <p>The library asks an availability service whether a copy is on the shelf
 * before it lends it. These tests describe what must happen when the service
 * says no, and when the service does not know the copy at all. They are
 * excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/*Test.java}) so that the start module stays green
 * while the defects are still present. Run them with:
 *
 * <pre>mvn -f labs verify -Pdiagnostics</pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockConfiguration.class)
class AvailabilityDiagnosticsTest {

    private static final String COPY_ID = "c-001";

    private static WireMockServer availabilityService;

    private final MockMvc mockMvc;

    @Autowired
    AvailabilityDiagnosticsTest(MockMvc mockMvc) {
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
    void aCopyThatIsNotOnTheShelfIsRefused() throws Exception {
        availabilityService.stubFor(get(urlPathEqualTo("/api/availability/" + COPY_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + COPY_ID + "\",\"available\":false}")));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"copyId\":\"" + COPY_ID + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").isNotEmpty());
    }

    @Test
    void aCopyTheServiceDoesNotKnowIsReportedAsAProblemDetail() throws Exception {
        availabilityService.stubFor(get(urlPathEqualTo("/api/availability/" + COPY_ID))
                .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"copyId\":\"" + COPY_ID + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").isNotEmpty());
    }
}
