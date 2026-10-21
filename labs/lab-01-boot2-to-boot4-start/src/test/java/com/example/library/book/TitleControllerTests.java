package com.example.library.book;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TitleControllerTests {

    private final MockMvc mockMvc;

    @Autowired
    TitleControllerTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void titlesEndpointIsServed() throws Exception {
        mockMvc.perform(get("/api/titles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void singleTitleEndpointIsServed() throws Exception {
        mockMvc.perform(get("/api/titles/t-001")).andExpect(status().isOk());
    }

    @Test
    void copiesEndpointIsServed() throws Exception {
        mockMvc.perform(get("/api/titles/t-001/copies")).andExpect(status().isOk());
    }
}
