package com.example.library.loan;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

import com.example.library.availability.Availability;
import com.example.library.availability.AvailabilityChecker;
import com.example.library.availability.CopyNotAvailableException;
import com.example.library.book.Copy;
import com.example.library.book.CopyRepository;
import com.example.library.security.SecurityConfiguration;

/**
 * The loan answer's shape, asserted at the webmvc slice alone.
 *
 * <p>The context this tier earns and why: the answers here - the route's
 * status, the validation's refusal, the media type, the payload's fields, the
 * problem detail's words - are all decided by the web tier: the controller,
 * the request body validation, the Jackson 3 serialization, the handler. The
 * edge the borrow maps at is the controller's too, so its answers belong
 * here: a copy the database does not hold is this tier's 404, asked of a
 * {@code CopyRepository} the slice can substitute. What the M10 upgrade
 * changed at the edge is kept at the edge: the real
 * {@link com.example.library.security.SecurityConfiguration} rides the
 * slice's MockMvc (imported, because a {@code SecurityFilterChain} bean is
 * not a web component and the slice would otherwise boot its fallback), the
 * caller is a Member, and the request body names only the Copy - the Member
 * comes from the principal. What the substitutes cannot prove (the real
 * boundary, the guard, the real replay policy and the client it wraps) is
 * not asserted here: it is the wiring home's one subject.
 */
@WebMvcTest(LoanController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(username = "ada", roles = "MEMBER")
class LoanControllerTests {

    private final MockMvc mockMvc;

    @MockitoBean
    private LoanService loans;

    @MockitoBean
    private AvailabilityChecker availability;

    @MockitoBean
    private CopyRepository copies;

    @Autowired
    LoanControllerTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * A body that validates is a created Loan, in the payload's shape - this
     * far the controller decides alone, after mapping the request's barcode
     * to the Copy the store holds. The body names the Copy and nothing else:
     * a memberId is not what a borrow may still say.
     */
    @Test
    void aBorrowRequestThatValidatesIsCreated() throws Exception {
        when(availability.availability("c-001")).thenReturn(new Availability("c-001", true));
        when(copies.findByBarcode("c-001")).thenReturn(Optional.of(new Copy(21L, 7L, "c-001", false)));
        when(loans.borrow("ada", new Copy(21L, 7L, "c-001", false)))
                .thenReturn(new Loan(9L, "ada", "c-001", LocalDate.parse("2026-01-05"), LocalDate.parse("2026-01-19")));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":\"c-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.memberId").value("ada"))
                .andExpect(jsonPath("$.copyId").value("c-001"))
                .andExpect(jsonPath("$.borrowedOn").value("2026-01-05"))
                .andExpect(jsonPath("$.dueOn").value("2026-01-19"));
    }

    /**
     * A body that still carries a memberId - the spelling the API used to
     * ask for - is not refused by that field, and the borrow does not land
     * where the body pointed: the record keeps only the Copy's barcode, so
     * the extra field is ignored, and the Loan the use case is asked for is
     * the authenticated Member's. How the borrower was himself verified: the
     * borrowing use case receives the principal whose credentials the call
     * carried.
     */
    @Test
    void aBodyThatStillNamesAMemberDoesNotNameTheBorrow() throws Exception {
        when(availability.availability("c-001")).thenReturn(new Availability("c-001", true));
        when(copies.findByBarcode("c-001")).thenReturn(Optional.of(new Copy(21L, 7L, "c-001", false)));
        when(loans.borrow("ada", new Copy(21L, 7L, "c-001", false)))
                .thenReturn(new Loan(9L, "ada", "c-001", LocalDate.parse("2026-01-05"), LocalDate.parse("2026-01-19")));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"someone-else\",\"copyId\":\"c-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("ada"));

        verify(loans).borrow("ada", new Copy(21L, 7L, "c-001", false));
    }

    /**
     * A body that does not validate is refused as a problem detail told in
     * the loan feature's words, and it names the field that failed - the
     * part a client actually needs. The body has one field left to fail: a
     * borrow with no Copy's barcode.
     */
    @Test
    void aBorrowRequestThatDoesNotValidateIsRefusedInTheLoansWords() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"   \",\"copyId\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid loan request"))
                .andExpect(jsonPath("$.detail").value("The request body is not valid."))
                .andExpect(jsonPath("$.errors", hasItem(containsString("copy"))));
    }

    /**
     * A copy the availability service says is off the shelf is refused with
     * {@code 409} and a problem detail that names the copy - the edge's
     * translation, before any mapping or any service is consulted.
     */
    @Test
    void aCopyOffTheShelfIsRefusedAsAProblemDetail() throws Exception {
        when(availability.availability("c-002")).thenReturn(new Availability("c-002", false));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":\"c-002\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy is not on the shelf"))
                .andExpect(jsonPath("$.detail").value("Copy c-002 is not on the shelf"));
    }

    /**
     * A copy the availability service does not know is refused with a
     * problem detail of its own: the service's 404 reaches the boundary as a
     * typed exception, and the handler answers in words a client can act on.
     */
    @Test
    void aCopyTheServiceDoesNotKnowIsRefusedAsUnknown() throws Exception {
        when(availability.availability("c-404"))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        HttpStatusCode.valueOf(404), "not known", null, new byte[0], null));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":\"c-404\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy is not known to the availability service"));
    }

    /**
     * A copy the availability service says is on the shelf but the library's
     * database does not hold is a 404, not a 409: the edge answers with the
     * handler's own words, and the borrow never reaches the store.
     */
    @Test
    void aBarcodeTheDatabaseDoesNotHoldIsRefusedAsNotFound() throws Exception {
        when(availability.availability("c-nowhere")).thenReturn(new Availability("c-nowhere", true));
        when(copies.findByBarcode("c-nowhere")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":\"c-nowhere\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy not found"))
                .andExpect(jsonPath("$.detail").value("No copy with barcode c-nowhere"));
    }

    /**
     * A borrow whose use case is refused - the loser of the race, or simply
     * a repeat request - hears the refusal in the availability feature's
     * words: the boundary's exception is one the handler already translates,
     * whatever told it to throw.
     */
    @Test
    void aBorrowTheUseCaseRefusesIsAnsweredInTheAvailabilitysWords() throws Exception {
        when(availability.availability("c-003")).thenReturn(new Availability("c-003", true));
        when(copies.findByBarcode("c-003")).thenReturn(Optional.of(new Copy(33L, 7L, "c-003", false)));
        when(loans.borrow("ada", new Copy(33L, 7L, "c-003", false))).thenThrow(new CopyNotAvailableException("c-003"));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":\"c-003\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Copy is not on the shelf"))
                .andExpect(jsonPath("$.detail").value("Copy c-003 is not on the shelf"));
    }

    /**
     * The Loan limit refusal keeps its own words too: the boundary throws
     * the domain's exception and the handler answers {@code 409}, whatever
     * store backs the count - and the count itself is one Member's, keyed by
     * the principal the call was authenticated as.
     */
    @Test
    void aMemberAtTheLoanLimitIsRefusedAsAProblemDetail() throws Exception {
        when(availability.availability("c-004")).thenReturn(new Availability("c-004", true));
        when(copies.findByBarcode("c-004")).thenReturn(Optional.of(new Copy(44L, 7L, "c-004", false)));
        when(loans.borrow("ada", new Copy(44L, 7L, "c-004", false))).thenThrow(new LoanLimitExceededException(3));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":\"c-004\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Loan limit reached"))
                .andExpect(jsonPath("$.detail").value("A member may hold 3 Active loans at once"));
    }
}
