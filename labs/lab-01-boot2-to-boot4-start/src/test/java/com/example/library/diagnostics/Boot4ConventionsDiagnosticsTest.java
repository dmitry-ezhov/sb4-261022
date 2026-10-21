package com.example.library.diagnostics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Diagnostics for the M01 lab.
 *
 * <p>These tests describe what the Boot 4 catalogue API must look like. They are
 * excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/*Test.java}) so that the start module stays green while
 * the defects are still present. Run them with:
 *
 * <pre>mvn -f labs verify -Pdiagnostics</pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
class Boot4ConventionsDiagnosticsTest {

    private final MockMvc mockMvc;

    @Autowired
    Boot4ConventionsDiagnosticsTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void missingTitleIsReportedAsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/titles/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Title not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void titlesAreReturnedAsAJsonArray() throws Exception {
        mockMvc.perform(get("/api/titles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("The Left Hand of Darkness"));
    }

    @Test
    void copiesAreReturnedAsAJsonArray() throws Exception {
        mockMvc.perform(get("/api/titles/t-001/copies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barcode").value("9780441478125001"));
    }
}
