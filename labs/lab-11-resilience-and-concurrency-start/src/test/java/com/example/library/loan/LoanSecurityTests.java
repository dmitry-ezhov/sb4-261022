package com.example.library.loan;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.library.availability.Availability;
import com.example.library.availability.AvailabilityClient;
import com.example.library.book.Copy;
import com.example.library.book.CopyRepository;
import com.example.library.security.SecurityConfiguration;

/**
 * The borrow's authorisation, answered by the real chains a caller steps
 * into: this slice carries the library's {@link SecurityConfiguration}, so
 * the refusals here are the product's refusals, not a test rig's.
 *
 * <p>The three answers the M10 upgrade exists for, at the tier where the
 * use case is one substituted stub and nothing else of the application
 * boots: an unauthenticated borrow is refused with {@code 401} before the
 * controller runs at all; a principal without the {@code MEMBER} role is
 * refused with {@code 403} by the {@code hasRole("MEMBER")} rule on
 * {@code /api/loans/**}, still before the controller runs - a different
 * refusal, earned by a caller who did authenticate; and a Member's borrow is
 * issued in the Member the credentials name, the borrow use case handed
 * their principal rather than whatever a body might still say. What the full
 * socket, the real store and the management surface prove beyond this tier
 * is the diagnostics' subject, not this one.
 */
@WebMvcTest(LoanController.class)
@Import(SecurityConfiguration.class)
class LoanSecurityTests {

    private final MockMvc mockMvc;

    @MockitoBean
    private LoanService loans;

    @MockitoBean
    private AvailabilityClient availability;

    @MockitoBean
    private CopyRepository copies;

    @Autowired
    LoanSecurityTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * A borrow with credentials on no wire at all: the chain answers
     * {@code 401} - what is missing is authentication, and the answer a
     * Basic-authenticated client reads into it, WWW-Authenticate and the
     * status - before any of the borrow's mapping runs.
     */
    @Test
    void anUnauthenticatedBorrowIsRefusedWith401() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":\"c-001\"}"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * A principal that authenticated but holds the wrong role: the
     * {@code hasRole("MEMBER")} rule refuses with {@code 403} - a qualifying
     * identity that does not qualify for the lending endpoints - before the
     * controller runs, so no Copy is mapped and the use case is not
     * consulted once.
     */
    @WithMockUser(username = "operator", roles = "OPERATOR")
    @Test
    void aPrincipalWithoutTheMemberRoleIsRefusedWith403() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":\"c-001\"}"))
                .andExpect(status().isForbidden());
    }

    /**
     * The borrow's own name: a Member on the Copy of their choice is created,
     * and the use case is asked for the Member the credentials name - never
     * the body's word for who is borrowing, because the body carries no word
     * for that.
     */
    @WithMockUser(username = "ada", roles = "MEMBER")
    @Test
    void aMembersBorrowIsIssuedInTheMemberTheCredentialsName() throws Exception {
        when(availability.availability("c-001")).thenReturn(new Availability("c-001", true));
        when(copies.findByBarcode("c-001")).thenReturn(Optional.of(new Copy(21L, 7L, "c-001", false)));
        when(loans.borrow("ada", new Copy(21L, 7L, "c-001", false)))
                .thenReturn(new Loan(9L, "ada", "c-001", null, null));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":\"c-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("ada"));

        verify(loans).borrow("ada", new Copy(21L, 7L, "c-001", false));
    }
}
