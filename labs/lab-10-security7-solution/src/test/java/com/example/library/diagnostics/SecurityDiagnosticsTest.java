package com.example.library.diagnostics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Diagnostics for the M10 lab: the library's API is protected like an API
 * that knows its caller.
 *
 * <p>The application was left with the observability lab's answers working and
 * nobody authenticated: the build carries no security starter, so no
 * {@code SecurityFilterChain} guards anything - any caller can borrow, the
 * borrow trusts the {@code memberId} the request body names, and the Loan
 * limit is counted against that body value. These tests state what the
 * security upgrade adds: an unauthenticated borrow refused with {@code 401}
 * (the title catalogue and the health probe staying public, every other path
 * closed), a principal
 * without the Member role refused with {@code 403} before the use case runs,
 * the Loan row issued for the authenticated Member rather than for whoever
 * the body named, and a Loan limit counted against the caller's loans - not
 * against a body field and not against a global total. The management surface keeps
 * its own socket and now its own rule: the health probe answers publicly, the
 * rest of the management surface answers to authentication.
 *
 * <p>They are excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/&#42;Test.java}) so that the start module stays green
 * while the security is still absent. Run them with:
 *
 * <pre>./mvnw -f labs -pl lab-10-security7-start verify -Pdiagnostics</pre>
 *
 * <p>Diagnostics are asserted in CI with the solutions-only profile: the same
 * tests, run only against the reference implementations by
 * {@code ./mvnw -f labs verify -Pdiagnostics-solutions}.
 *
 * <p>The principals these tests authenticate as, so the reference
 * implementation can be written against the same words: three Members -
 * {@code ada}, {@code bruno} and {@code cecil}, each with the password
 * {@code member-password} and the {@code MEMBER} role - and an {@code OPERATOR}
 * principal named {@code operator} with the password {@code operator-password},
 * the role the management surface's own callers hold. The store that holds
 * them is the solution module's to write, its
 * passwords encoded by a {@code PasswordEncoder} so every stored value names
 * its algorithm; a Member's authenticated name is the {@code member_id} the
 * Loan row carries.
 *
 * <p>Not a MockMvc rig, on purpose: the requirement is authorisation seen at
 * the wire - statuses and problem details as a caller meets them, with
 * credentials as a real client attaches them, and one of the reads on the
 * management socket, which the borrowed MockMvc never reaches. The
 * application boots whole on its random port and the requests arrive the way
 * a Member's basic-auth client would send them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityDiagnosticsTest {

    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** The Member the authorisation diagnostics borrow as. */
    private static final String MEMBER_NAME = "ada";

    private static final String MEMBER_PASSWORD = "member-password";
    /** The Member sitting at the Loan limit for the fourth requirement. */
    private static final String LIMITED_MEMBER_NAME = "cecil";
    /** The Member under the limit while the limited one is at it. */
    private static final String SECOND_MEMBER_NAME = "bruno";
    /** The principal holding the management surface's role, for the 403. */
    private static final String OPERATOR_NAME = "operator";

    private static final String OPERATOR_PASSWORD = "operator-password";

    private static WireMockServer availabilityService;

    private final Environment environment;
    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    @Autowired
    SecurityDiagnosticsTest(Environment environment, JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.environment = environment;
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
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
    static void availabilityServiceBaseUrlAndAFreshDatabase(DynamicPropertyRegistry registry) {
        registry.add("library.availability.base-url", () -> "http://localhost:" + availabilityService.port());
        // A database the diagnostics own, freshly created per run: the
        // application is pointed at it exactly as it would be pointed at the
        // lab's H2, and nothing a neighbouring test class wrote survives.
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:lab10-" + UUID.randomUUID());
        registry.add("spring.datasource.username", () -> "sa");
    }

    /**
     * The first requirement: the lending endpoints require authentication.
     * An unauthenticated borrow is refused with {@code 401} - authentication
     * is what it lacks - before anything of the borrow runs at all, while the
     * title catalogue stays readable by anyone.
     */
    @Test
    void anUnauthenticatedBorrowIsRefusedWith401() throws IOException, InterruptedException {
        assertThat(fetch(apiBase(), "/api/titles").statusCode())
                .as("The title catalogue did not answer 200 to an unauthenticated read: the catalogue is "
                        + "one of the two surfaces this lab keeps public - a read before a login needs no "
                        + "authentication. When the chain answers this with 401, its permitAll rule for "
                        + "the GET on /api/titles/** is missing, or is not first in the rule order - the "
                        + "two public rules precede the Member rule and the anyRequest() rule that closes "
                        + "the chain.")
                .isEqualTo(200);

        String copyOnTheShelf = writeACopyOnTheShelf();
        HttpResponse<String> answered = borrowUnauthenticated(aMemberNobodyKnowsAbout(), copyOnTheShelf);
        assertThat(answered.statusCode())
                .as(
                        "An unauthenticated borrow of a Copy on the shelf was not refused with 401 (the "
                                + "borrow answered %s): the lending API answers any caller because the build "
                                + "carries no security starter, so there is no chain to refuse anything. The "
                                + "upgrade, in the doc's order: add spring-boot-starter-security (and "
                                + "spring-boot-starter-security-test in test scope), declare the chain as a @Bean "
                                + "SecurityFilterChain taking HttpSecurity - no WebSecurityConfigurerAdapter, no "
                                + "authorizeRequests(), no and() - and let it refuse this request: a caller with "
                                + "no credentials is unauthenticated, and an unauthenticated borrow answers 401.",
                        answered.statusCode())
                .isEqualTo(401);

        // Everything else is backed by the anyRequest() rule: the
        // reservation surface is the application's other open endpoint
        // today, and one unauthenticated call states what must happen to
        // every path the rules do not name.
        HttpResponse<String> reserve =
                post("/api/reservations", "{\"memberId\":\"" + aMemberNobodyKnowsAbout() + "\",\"titleId\":\"t-1\"}");
        assertThat(reserve.statusCode())
                .as(
                        "An unauthenticated call on the reservation surface was not refused with 401 (it "
                                + "answered %s): the chain keeps more than the lending endpoints behind "
                                + "authentication - the last rule is anyRequest().authenticated(), so every path "
                                + "the rules do not name is closed, including this one. If only /api/loans is "
                                + "guarded, a rule like permitAll() on some other api/ path, or the chain never "
                                + "reaching its last rule, is what let this through.",
                        reserve.statusCode())
                .isEqualTo(401);
    }

    /**
     * The second requirement: the URL rule answers <em>may this principal call
     * the lending API at all</em>, and it answers with the role. An
     * authenticated principal without the {@code MEMBER} role is refused with
     * {@code 403} before the use case runs - a different refusal from the
     * first test's {@code 401}, earned by a caller who did authenticate.
     */
    @Test
    void aPrincipalWithoutTheMemberRoleIsRefusedWith403() throws IOException, InterruptedException {
        String copyOnTheShelf = writeACopyOnTheShelf();
        HttpResponse<String> answered =
                borrow(OPERATOR_NAME, OPERATOR_PASSWORD, aMemberNobodyKnowsAbout(), copyOnTheShelf);
        assertThat(answered.statusCode())
                .as(
                        "The borrow from a principal without the Member role was not refused with 403 (the "
                                + "borrow answered %s). Two refusals are allowed and neither happened: the build "
                                + "carries no security starter, so no chain guards the path and the borrow went "
                                + "through as if the credentials had been valid; and if a chain exists but this "
                                + "still answered 201, the rule is hasRole(\"MEMBER\") on /api/loans/** and it is "
                                + "not matching. The distinction the diagnostics insist on: this caller "
                                + "authenticated - operator/operator-password, an OPERATOR principal whose role "
                                + "is the management surface's - so the refusal is not the 401 of unknowing, it "
                                + "is the 403 of a role that does not qualify, answered before the use case "
                                + "runs. If the answer were 401 instead, the credential would be unreadable: the "
                                + "store must define the operator principal these diagnostics authenticate as, "
                                + "next to the Members.",
                        answered.statusCode())
                .isEqualTo(403);
    }

    /**
     * The third requirement: the Loan belongs to the authenticated Member,
     * not to the memberId the body carried. Two calls prove the whole shape:
     * a body that still names somebody else is ignored, and the Loan lands on
     * the caller; a body that names no Member at all works as well as the one
     * that did - the request record keeps only what the caller is allowed to
     * choose, the Copy's barcode.
     */
    @Test
    void aLoanBelongsToTheAuthenticatedMemberNotTheBody() throws IOException, InterruptedException {
        String barcodeTheBodyNames = writeACopyOnTheShelf();
        HttpResponse<String> aBodyNamingSomeoneElse =
                borrow(MEMBER_NAME, MEMBER_PASSWORD, aMemberNobodyKnowsAbout(), barcodeTheBodyNames);
        assertThat(aBodyNamingSomeoneElse.statusCode())
                .as(
                        "The borrow as an authenticated Member whose body also named somebody else's "
                                + "memberId did not answer 201 (it answered %s): a request that still carries the "
                                + "old field may keep working - the field is ignored, not refused, so its 201 is "
                                + "not the defect. The call whose Loan row is read below is the assertion's "
                                + "object.",
                        aBodyNamingSomeoneElse.statusCode())
                .isEqualTo(201);
        assertThat(ownerOfTheLoanFor(barcodeTheBodyNames))
                .as(
                        "The Loan row for the Copy the body's barcode named was written for %s, the memberId "
                                + "the request body carried: the borrow trusts the body, so any caller able to "
                                + "reach /api/loans can borrow as any Member. The controller stops reading a "
                                + "memberId from the request and passes authentication.getName() to the use case "
                                + "instead - the Member is the Authentication the container injects. The "
                                + "request record keeps only what the caller is allowed to choose: drop memberId "
                                + "from LoanRequest, because an id the API still offers the caller is an id true "
                                + "only while one method remembers its annotation.",
                        ownerOfTheLoanFor(barcodeTheBodyNames))
                .isEqualTo(MEMBER_NAME);

        String barcodeTheBodyDoesNotName = writeACopyOnTheShelf();
        HttpResponse<String> aBodyWithoutAMember =
                borrow(MEMBER_NAME, MEMBER_PASSWORD, null, barcodeTheBodyDoesNotName);
        assertThat(aBodyWithoutAMember.statusCode())
                .as(
                        "The borrow whose body names no Member at all did not answer 201 (it answered %s): "
                                + "the request record still requires a memberId the caller must not choose - so "
                                + "the borrow is refused by the very validation that should have nothing to "
                                + "validate. Drop memberId from LoanRequest along with its @NotBlank: the "
                                + "Authentication the controller injects is the only source the Member's "
                                + "identity has, and the body keeps the Copy's barcode - the one thing a borrow "
                                + "must still say.",
                        aBodyWithoutAMember.statusCode())
                .isEqualTo(201);
        assertThat(ownerOfTheLoanFor(barcodeTheBodyDoesNotName))
                .as(
                        "The Loan row the record-without-a-memberId borrow wrote was written for %s, not for "
                                + "the authenticated Member: the row belongs to the principal whose credentials "
                                + "the call carried.",
                        ownerOfTheLoanFor(barcodeTheBodyDoesNotName))
                .isEqualTo(MEMBER_NAME);
    }

    /**
     * The fourth requirement: the Loan limit counts the authenticated
     * Member's Active loans. One Member sits at the limit and earns the
     * domain's own refusal on the fourth borrow, and a second Member with no
     * Active loans borrows straight through while the first is at the limit -
     * the count the limit reads is neither the request's field nor a global
     * total, but the caller's.
     */
    @Test
    void theLoanLimitCountsTheAuthenticatedMembersLoans() throws IOException, InterruptedException {
        topUpToTheLoanLimit(LIMITED_MEMBER_NAME);

        String barcodeAtTheLimit = writeACopyOnTheShelf();
        HttpResponse<String> theMembersFourthBorrow =
                borrow(LIMITED_MEMBER_NAME, MEMBER_PASSWORD, SECOND_MEMBER_NAME, barcodeAtTheLimit);
        assertThat(theMembersFourthBorrow.statusCode())
                .as(
                        "The borrow as an authenticated Member at the Loan limit was not refused with 409 "
                                + "(it answered %s): this call carried %s's credentials, the body named %s, and "
                                + "the Member under the limit the body named borrowed on behalf of one who "
                                + "cannot. The count the limit reads is the use case's argument, and the argument "
                                + "is the principal: once the controller passes authentication.getName() in, the "
                                + "count is the caller's Active loans and the fourth borrow hears the limit's own "
                                + "refusal. Tighten the shape with the second lock the lab asks for: "
                                + "@EnableMethodSecurity on a configuration class - the new annotation, whose "
                                + "prePostEnabled defaults to true, not the deprecated one whose default runs "
                                + "nothing - and @PreAuthorize(\"#memberId == authentication.name\") on the borrow "
                                + "use case, where the expression compares the argument with the principal.",
                        theMembersFourthBorrow.statusCode(), LIMITED_MEMBER_NAME, SECOND_MEMBER_NAME)
                .isEqualTo(409);
        assertThat(titleOf(theMembersFourthBorrow))
                .as(
                        "The fourth borrow's problem detail is not titled by the Loan limit (the title read "
                                + "back: %s): the refusal a Member at the limit earns is the domain's own - the "
                                + "limit's exception translated to the problem detail titled 'Loan limit "
                                + "reached', not the availability service's, not a validation error.",
                        titleOf(theMembersFourthBorrow))
                .isEqualTo("Loan limit reached");

        String barcodeUnderTheLimit = writeACopyOnTheShelf();
        HttpResponse<String> whileTheFirstIsAtTheLimit =
                borrow(SECOND_MEMBER_NAME, MEMBER_PASSWORD, LIMITED_MEMBER_NAME, barcodeUnderTheLimit);
        assertThat(whileTheFirstIsAtTheLimit.statusCode())
                .as(
                        "A second Member's first borrow was refused (%s) while another Member sat at the "
                                + "Loan limit: the count the limit read is either the one the body names or a "
                                + "count of every loan in the table - both are wrong, and both stop the Member "
                                + "under the limit from borrowing. The limit is per Member: the count is keyed by "
                                + "the principal's name, the store's query answers one member's Active loans, and "
                                + "a Member under the limit borrows however many loans anybody else holds.",
                        whileTheFirstIsAtTheLimit.statusCode())
                .isEqualTo(201);
        assertThat(ownerOfTheLoanFor(barcodeUnderTheLimit))
                .as(
                        "The Loan row the second Member's borrow wrote was written for %s: the row belongs to "
                                + "the authenticated Member, whose credentials the call carried.",
                        ownerOfTheLoanFor(barcodeUnderTheLimit))
                .isEqualTo(SECOND_MEMBER_NAME);
    }

    /**
     * The fifth requirement: every management read answers to authentication.
     * M09 separated the management surface onto a socket of its own and
     * exposed one step past health - the metrics endpoint - and the lab keeps
     * the separation and adds the guard: the health probe stays public, the
     * rest of the management surface closes.
     */
    @Test
    void theManagementSurfaceAnswersOnlyToAuthentication() throws IOException, InterruptedException {
        assertThat(fetch(managementBase(), "/actuator/health").statusCode())
                .as("The health probe no longer answers 200 unauthenticated on the management socket: "
                        + "the M09 probe stays public in this lab too - it is the second of the two "
                        + "surfaces whose permitAll rules stand ahead of the Member rule and the "
                        + "anyRequest() rule that closes the chain.")
                .isEqualTo(200);

        HttpResponse<String> metrics = fetch(managementBase(), "/actuator/metrics");
        assertThat(metrics.statusCode())
                .as(
                        "An unauthenticated read of the management surface's metrics endpoint answered %s: "
                                + "the M09 socket is separated but guarded by nothing, and the security upgrade "
                                + "closes it. The management surface keeps its own chain, ordered ahead of the "
                                + "api's (@Order(1) ahead of @Order(2), selected with securityMatcher and the "
                                + "matcher built from PathPatternRequestMatcher.withDefaults() - the one matcher "
                                + "class 7.1.1 still has), permits /actuator/health/** so the probe above keeps "
                                + "answering, and leaves every other exposed endpoint behind authentication: "
                                + "metrics is on M09's exposure list, so the read above is a real read of real "
                                + "data until a chain refuses it. A stranger's 200 is the leak this test exists "
                                + "to name.",
                        metrics.statusCode())
                .isNotEqualTo(200);
    }

    /* ------------------------------------------------------------------
     * The wires: sockets, credentials, and the rows the borrow leaves.
     * ------------------------------------------------------------------ */

    /**
     * The public API's socket, read the way the application's own clients
     * discover it: the property the web server publishes on the random port
     * these diagnostics boot it on.
     */
    private String apiBase() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    /**
     * The management socket, read the way M09's diagnostics read it: the
     * property the management server publishes once it rides a port of its
     * own.
     */
    private String managementBase() {
        return "http://localhost:" + environment.getProperty("local.management.port");
    }

    private HttpResponse<String> fetch(String base, String path) {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder(URI.create(base + path)).GET().build();
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException theSocketDidNotAnswer) {
            return failOnUnansweredSocket(base, path, theSocketDidNotAnswer);
        }
    }

    /**
     * A POST on the public API with no credential attached: the probe of the
     * chain's last rule, asked of a path that no permitAll rule names.
     */
    private HttpResponse<String> post(String path, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase() + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException theSocketDidNotAnswer) {
            return failOnUnansweredSocket(apiBase(), path, theSocketDidNotAnswer);
        }
    }

    /**
     * A borrow with no credential at all - what an anonymous caller sends
     * before the upgrade, and what the chain must refuse.
     */
    private HttpResponse<String> borrowUnauthenticated(String memberId, String copyId) {
        return borrow(null, null, memberId, copyId);
    }

    /**
     * A borrow typed the way a Member's client types it: the credentials on
     * the wire, a JSON body asking for the Copy. The body may name a Member
     * or name none - both spellings are sent, because what the body may still
     * carry is part of what this lab decides: the Loan it produces belongs to
     * the credentials, never to the body.
     */
    private HttpResponse<String> borrow(String username, String password, String memberId, String copyId) {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create(apiBase() + "/api/loans")).header("Content-Type", "application/json");
        if (username != null) {
            request.header("Authorization", basic(username, password));
        }
        try {
            return HTTP.send(
                    request.POST(HttpRequest.BodyPublishers.ofString(borrowBody(memberId, copyId)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException theSocketDidNotAnswer) {
            return failOnUnansweredSocket(apiBase(), "/api/loans", theSocketDidNotAnswer);
        }
    }

    private String basic(String username, String password) {
        return "Basic "
                + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private String borrowBody(String memberId, String copyId) {
        if (memberId == null) {
            return "{\"copyId\":\"" + copyId + "\"}";
        }
        return "{\"memberId\":\"" + memberId + "\",\"copyId\":\"" + copyId + "\"}";
    }

    /**
     * The {@code member_id} of the Loan row the database holds for one Copy's
     * barcode - read straight off the table, because the question is whose
     * Loan this is, and the row is the only place the answer lives once the
     * borrow has finished.
     */
    private String ownerOfTheLoanFor(String barcode) {
        return jdbcClient
                .sql("""
                        select l.member_id
                        from loan l
                        join copy c on c.id = l.copy_id
                        where c.barcode = :barcode
                        """)
                .param("barcode", barcode)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    private int activeLoansOf(String memberId) {
        return Math.toIntExact(jdbcClient
                .sql("select count(*) from loan where member_id = :memberId")
                .param("memberId", memberId)
                .query(Long.class)
                .single());
    }

    /**
     * The problem detail's title: the word the refusal calls itself in.
     */
    private String titleOf(HttpResponse<String> problem) {
        return jsonMapper.readTree(problem.body()).path("title").asString();
    }

    /**
     * Brings a Member up to the Loan limit with Active loans written the way
     * time writes them: one row per Copy in the loan table, directly - the
     * how is irrelevant to the requirement, the <em>how many</em> is all of
     * it. The count starts from what the table already holds, so the test
     * leaves every other test's Member exactly where that test left it.
     */
    private void topUpToTheLoanLimit(String memberId) {
        int limit = environment.getProperty("library.loan.limit", Integer.class, 3);
        for (int held = activeLoansOf(memberId); held < limit; held++) {
            writeAnActiveLoanFor(memberId);
        }
    }

    /**
     * One Title, one Copy out of circulation and one Loan row: borrowed
     * today, due a loan period later - the Active loan the limit counts.
     * The row needs a Copy because the table's foreign key does, and the
     * Copy needs its Title; the identifiers are the database's to mint.
     */
    private void writeAnActiveLoanFor(String memberId) {
        String isbn = anUnusedIsbn();
        String barcode = "bc-" + UUID.randomUUID();
        jdbcClient
                .sql("insert into title (name, author, isbn) values (:name, :author, :isbn)")
                .param("name", "The Left Hand of Darkness")
                .param("author", "Ursula K. Le Guin")
                .param("isbn", isbn)
                .update();
        Long titleId = jdbcClient
                .sql("select id from title where isbn = :isbn")
                .param("isbn", isbn)
                .query(Long.class)
                .single();
        jdbcClient
                .sql("insert into copy (title_id, barcode, on_loan) values (:titleId, :barcode, true)")
                .param("titleId", titleId)
                .param("barcode", barcode)
                .update();
        Long copyId = jdbcClient
                .sql("select id from copy where barcode = :barcode")
                .param("barcode", barcode)
                .query(Long.class)
                .single();
        LocalDate borrowedOn = LocalDate.now();
        jdbcClient
                .sql("insert into loan (member_id, copy_id, borrowed_on, due_on) values "
                        + "(:memberId, :copyId, :borrowedOn, :dueOn)")
                .param("memberId", memberId)
                .param("copyId", copyId)
                .param("borrowedOn", borrowedOn)
                .param("dueOn", borrowedOn.plusDays(14))
                .update();
    }

    private String anUnusedIsbn() {
        return "isbn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * One Title with one Copy on the shelf, written straight into the
     * database - the Copy a borrow must be issued against - and the
     * availability service stubbed to say just that: the copy is available.
     */
    private String writeACopyOnTheShelf() {
        String isbn = anUnusedIsbn();
        String barcode = "bc-" + UUID.randomUUID();
        jdbcClient
                .sql("insert into title (name, author, isbn) values (:name, :author, :isbn)")
                .param("name", "The Dispossessed")
                .param("author", "Ursula K. Le Guin")
                .param("isbn", isbn)
                .update();
        Long titleId = jdbcClient
                .sql("select id from title where isbn = :isbn")
                .param("isbn", isbn)
                .query(Long.class)
                .single();
        jdbcClient
                .sql("insert into copy (title_id, barcode, on_loan) values (:titleId, :barcode, :onLoan)")
                .param("titleId", titleId)
                .param("barcode", barcode)
                // An Available copy: not currently on loan.
                .param("onLoan", Boolean.FALSE)
                .update();
        availabilityService.stubFor(get(urlPathMatching("/api/availability/" + barcode))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + barcode + "\",\"available\":true}")));
        return barcode;
    }

    /**
     * A member id no other test can hold a Loan for: the Active-loan state
     * the library keeps is per member, so the diagnostics borrow under ids a
     * neighbouring run cannot have filled up.
     */
    private String aMemberNobodyKnowsAbout() {
        return "m-" + UUID.randomUUID();
    }

    /**
     * A socket that never answers is not a refusal the diagnostics judge:
     * the failure names the socket, not a status code - and never returns.
     */
    private <T> T failOnUnansweredSocket(String base, String path, Exception cause) {
        if (cause instanceof InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        fail(
                "The socket %s did not answer a request for %s: the application the diagnostics boot owns "
                        + "both sockets, and reading them is the first thing to repair. The cause is the "
                        + "unreadable answer's.",
                base, path, cause);
        return null; // not reached: fail never returns
    }
}
