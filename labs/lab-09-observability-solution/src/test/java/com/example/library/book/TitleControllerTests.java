package com.example.library.book;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The catalogue answer's shape, asserted at the webmvc slice alone.
 *
 * <p>What a client of the library sees here - the list's shape, an item's
 * fields, the problem detail for a title the library does not hold - is the
 * web tier's answer to what the repositories return. Which table the reads
 * came from is not a thing the answer says, so the slice names its one
 * controller and substitutes both repositories the constructor asks for with
 * {@code @MockitoBean} fields: no database is booted for the boundary's
 * answers, and the real reads are proven where they belong - against the
 * engine the library ships on, by the container test.
 */
@WebMvcTest(TitleController.class)
class TitleControllerTests {

    private final MockMvc mockMvc;

    @MockitoBean
    private TitleCatalogue titles;

    @MockitoBean
    private CopyRepository copies;

    @Autowired
    TitleControllerTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * The titles are served in the payload shape M05 fixed, typed by
     * Jackson 3 - the list the repository returns is what the answer carries.
     */
    @Test
    void theCatalogueServesTheTitlesItHas() throws Exception {
        when(titles.findAll())
                .thenReturn(List.of(new Title(91L, "The Left Hand of Darkness", "Ursula K. Le Guin", "9780441478125")));

        mockMvc.perform(get("/api/titles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(91))
                .andExpect(jsonPath("$[0].isbn").value("9780441478125"));
    }

    /**
     * A Title's Copies are served with the barcode and the on-loan answer of
     * each item, exactly as the read returned them.
     */
    @Test
    void aTitlesCopiesAreServedAsTheReadReturnsThem() throws Exception {
        when(copies.findByTitleId(91L))
                .thenReturn(List.of(
                        new Copy(911L, 91L, "9780441478125001", true), new Copy(912L, 91L, "9780441478125002", false)));

        mockMvc.perform(get("/api/titles/91/copies"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].barcode").value("9780441478125001"))
                .andExpect(jsonPath("$[0].onLoan").value(true))
                .andExpect(jsonPath("$[1].onLoan").value(false));
    }

    /**
     * A Title the catalogue does not hold is a problem detail, the {@code 404}
     * the boundary's handler translates.
     */
    @Test
    void aTitleTheCatalogueDoesNotHoldIsAProblemDetail() throws Exception {
        when(titles.findById(424242L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/titles/424242"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Title not found"))
                .andExpect(jsonPath("$.detail").value("No title with id 424242"));
    }
}
