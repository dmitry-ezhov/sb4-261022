package com.example.library.diagnostics;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.library.FixedClockConfiguration;
import com.example.library.LibraryApplication;

/**
 * Diagnostics for the M03 lab.
 *
 * <p>These tests describe what the library's lending rules must look like.
 * They are excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/*Test.java}) so that the start module stays green
 * while the defects are still present. Run them with:
 *
 * <pre>mvn -f labs verify -Pdiagnostics</pre>
 */
@SpringBootTest(properties = "library.loan.period=30D")
@AutoConfigureMockMvc
@Import(FixedClockConfiguration.class)
class LoanConfigurationDiagnosticsTest {

    private final MockMvc mockMvc;

    @Autowired
    LoanConfigurationDiagnosticsTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void theLoanPeriodComesFromTheConfiguration() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"m-001\",\"copyId\":\"c-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.borrowedOn").value("2026-01-05"))
                .andExpect(jsonPath("$.dueOn").value("2026-02-04"));
    }

    @Test
    void anInvalidLoanLimitStopsTheApplication() {
        assertThatThrownBy(() -> new SpringApplicationBuilder(LibraryApplication.class)
                        .web(WebApplicationType.NONE)
                        .run("--library.loan.limit=0"))
                .hasStackTraceContaining("library.loan.limit");
    }
}
