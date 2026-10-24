package com.example.library.diagnostics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.WireMockServer;
import tools.jackson.databind.json.JsonMapper;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.example.library.LibraryApplication;

/**
 * Diagnostics for the M11 lab's third move: shutdown is a drain, and the
 * window the drain gets is a decision the application makes, not a number
 * nobody discussed.
 *
 * <p>4.1.1's default already answers {@code graceful} - the flip happened in
 * Boot 3.4, one generation before this course's stack - so the mechanism is
 * not what the Start module lacks. What it lacks is the window being
 * <em>named</em>: without {@code spring.lifecycle.timeout-per-shutdown-phase}
 * the application promises thirty seconds it never discussed, the default
 * nobody remembers accepting. These tests hold the two halves of the drain up
 * side by side: a Loan begun across the stop completes - the borrow whose
 * availability answer is still on the wire when the application is stopped,
 * landing as a Loan row inside the drain - and a request that arrives after
 * the stop is refused, because the server stopped accepting before the drain
 * finished.
 *
 * <p>Excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/&#42;Test.java}); run with:
 *
 * <pre>./mvnw -f labs -pl lab-11-resilience-and-concurrency-start verify -Pdiagnostics</pre>
 *
 * <p>The Member borrowing is {@code ada}, password {@code member-password},
 * the {@code MEMBER} principal the security lab's store holds - the
 * resilience lab changes nothing about who may ask.
 *
 * <p>The application is booted here by the test itself, not by the
 * {@code @SpringBootTest} rig, and stopped by the test itself as well: the
 * drain is the one observable that only a stopping application can show, and
 * a context the test framework holds in its cache cannot be stopped without
 * the framework reaching for it again. The boot is the application's own -
 * one {@link LibraryApplication} on a random port, over its own H2 and this
 * run's WireMock - so what stops is precisely what a Member's library is.
 */
class ShutdownDiagnosticsTest {

    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** The Member the in-flight borrow is issued in. */
    private static final String MEMBER_NAME = "ada";

    private static final String MEMBER_PASSWORD = "member-password";

    /**
     * How long the stubbed availability answer keeps the borrow on the wire:
     * comfortably past the point the stop begins, comfortably inside any
     * window the application could seriously name for its own in-flight work.
     */
    private static final long THE_ANSWER_TAKES_MILLIS = 1500;

    private static WireMockServer availabilityService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * A store the Copy rows are written with, pointed at the database the
     * application booted over: one writer, one run, no state a neighbouring
     * test class shares.
     */
    private JdbcClient jdbcClientFor(String jdbcUrl) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(jdbcUrl);
        dataSource.setUser("sa");
        return JdbcClient.create(dataSource);
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

    /**
     * The requirement, each half pleaded against the other: a borrow whose
     * availability answer is still on the wire when the stop begins lands all
     * the same - the drain held the request open until the Loan was written -
     * and a request arriving after the drain is refused, because the server
     * stopped accepting before it let the last request go. That both halves
     * work is 4.1.1's default; what the Lab asks the change to add is the
     * window being named: {@code spring.lifecycle.timeout-per-shutdown-phase},
     * to what the application's in-flight work can actually promise.
     */
    @Test
    void aLoanBegunAcrossTheStopLandsAndARequestAfterItIsRefused() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:lab11-drain-" + UUID.randomUUID();

        ConfigurableApplicationContext application = bootTheApplication(jdbcUrl);
        try {
            String apiBase = "http://localhost:" + application.getEnvironment().getProperty("local.server.port");
            String window = application.getEnvironment().getProperty("spring.lifecycle.timeout-per-shutdown-phase");

            assertThat(window)
                    .as(
                            "spring.lifecycle.timeout-per-shutdown-phase is not set on the application this "
                                    + "diagnostics boots (the read: %s): the drain happens - 4.1.1 answers graceful "
                                    + "by default, the flip Boot 3.4 made, not Boot 4 - but the window it happens "
                                    + "in is the thirty seconds nobody discussed, the default the property's "
                                    + "absence spells. The Lab's third move is the number becoming a decision: set "
                                    + "spring.lifecycle.timeout-per-shutdown-phase in application.yml to what the "
                                    + "application's in-flight work can actually promise - a borrow is one read of "
                                    + "the availability service plus one transaction, seconds, not a half-minute. "
                                    + "The drain assertions below prove the mechanism; this assertion states the "
                                    + "window it is granted in.",
                            window)
                    .isNotBlank();

            // The shelved Copy the borrow lands, written into the database the
            // application booted over: the boot's Flyway made the tables, and
            // the boot holds the in-memory database open. The URL is read back
            // off the booted environment - any settings the driver machinery
            // appended into it are part of H2's database identity, and the
            // writer must name the database by its own spelling.
            String theBootedDatabase = application.getEnvironment().getProperty("spring.datasource.url");
            String barcode = writeACopyOnTheShelfAndSlowTheAnswer(theBootedDatabase);

            CompletableFuture<Answer> borrow = borrowAsynchronously(apiBase, MEMBER_NAME, MEMBER_PASSWORD, barcode);

            // In flight: the availability answer is being waited on - the
            // borrow is one read of the availability service away from the
            // transaction and the Loan row - and the stop begins beneath it.
            waitUntilTheBorrowWaitsOnTheAvailabilityAnswer(barcode);
            assertThat(borrow.isDone())
                    .as("The in-flight borrow completed before the stop began: the drain test must hold "
                            + "the borrow across the stop, and this one was already answered before the "
                            + "application could be stopped beneath it. The stubbed availability answer "
                            + "holds the request on the wire for " + THE_ANSWER_TAKES_MILLIS
                            + "ms; begin the stop after the availability request has been made and while "
                            + "it is still being waited on, and the Loan row written after the stop began "
                            + "is what the first half of this test asserts.")
                    .isFalse();

            application.close();

            // First half: the borrow the stop began beneath. It answered - the
            // server did not kill it - and what it answered is the Loan,
            // written before the drain let the connection go.
            assertThat(borrow.join().status())
                    .as(
                            "The borrow begun across the stop did not answer 201 (it answered %s): shutdown "
                                    + "answered SIGTERM by dying mid-borrow - the shape the M11 doc's drain row "
                                    + "warns of. 4.1.1's default is already graceful, so a fail here means "
                                    + "something un-broke the drain: a server.shutdown=immediate reintroduced in "
                                    + "the change, a window named too short for the in-flight work "
                                    + "(spring.lifecycle.timeout-per-shutdown-phase - named, by the first "
                                    + "assertion of this test), or a retry or a transaction that does not "
                                    + "survive being drained.",
                            borrow.join().status())
                    .isEqualTo(201);
            assertThat(borrow.join().memberId())
                    .as(
                            "The Loan the drain carried over the stop was issued for %s, not for the Member "
                                    + "the credentials name: the borrow lands as it was issued - the "
                                    + "Authentication the chain answered - not as something else the drain "
                                    + "produced.",
                            borrow.join().memberId())
                    .isEqualTo(MEMBER_NAME);

            // Second half: a request arriving after the drain is refused. The
            // ConnectException is the refusal, as a caller sees it: nothing
            // accepted the connection, because the server stopped accepting
            // before the drain returned.
            try {
                HttpResponse<String> afterTheDrain = HTTP.send(
                        newApiRequest(apiBase, "/api/titles").GET().build(), HttpResponse.BodyHandlers.ofString());
                fail(
                        "A request after the drain was not refused (the public read of /api/titles answered %s): "
                                + "after the application stops, nothing should accept the connection - the server "
                                + "stopped accepting before it let the in-flight borrow go. If the socket answers, "
                                + "the shutdown closed the context without closing the web server's listener, or "
                                + "another process owns this test's API port.",
                        afterTheDrain.statusCode());
            } catch (ConnectException theListenerIsGone) {
                // The refusal the accept-refusal point shows a client.
            }
        } finally {
            if (application.isActive()) {
                application.close();
            }
        }
    }

    /* ------------------------------------------------------------------
     * The boot: one LibraryApplication, on a random API socket over its own
     * H2 and this run's WireMock - the application the lab asks about.
     * ------------------------------------------------------------------ */

    /**
     * The application, booted the way a deployment boots it: servlet over an
     * API socket on port zero, with the management surface on its own socket
     * but not exposed; the H2 and the availability URL are this run's. Its
     * migration makes the tables, its connections keep the in-memory
     * database open - the Copy rows the test writes land in that database,
     * and the application reads them back.
     */
    private ConfigurableApplicationContext bootTheApplication(String jdbcUrl) {
        // The overrides ride the command line - the one property source that
        // outranks the application's own yml - so the boot is pointed at this
        // run's sockets, database and stub, not at the yml's defaults.
        return new SpringApplicationBuilder(LibraryApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=0",
                        "--management.server.port=0",
                        "--library.availability.base-url=http://localhost:" + availabilityService.port(),
                        "--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.username=sa");
    }

    /* ------------------------------------------------------------------
     * The wires: the borrow and the Copy the borrow needs.
     * ------------------------------------------------------------------ */

    private CompletableFuture<Answer> borrowAsynchronously(
            String apiBase, String username, String password, String copyId) {
        HttpRequest request = newApiRequest(apiBase, "/api/loans")
                .header("Content-Type", "application/json")
                .header("Authorization", basic(username, password))
                .POST(HttpRequest.BodyPublishers.ofString("{\"copyId\":\"" + copyId + "\"}"))
                .build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(answer -> new Answer(
                        answer.statusCode(),
                        answer.body(),
                        jsonMapper.readTree(answer.body()).path("memberId").asString()))
                .exceptionally(theWireDied -> Answer.A_BORROW_THAT_NEVER_CAME_BACK);
    }

    /**
     * Waits until the availability answer is being waited on: the borrow is
     * inside the call the drain must outlive. The WireMock count is the
     * truth: the request either made it to the stub or it did not.
     */
    private void waitUntilTheBorrowWaitsOnTheAvailabilityAnswer(String barcode) throws InterruptedException {
        long waitedMillis = 0;
        long theDeadline = Duration.ofSeconds(10).toMillis();
        while (availabilityService
                        .countRequestsMatching(getRequestedFor(urlEqualTo("/api/availability/" + barcode))
                                .build())
                        .getCount()
                < 1) {
            if (waitedMillis >= theDeadline) {
                fail("The in-flight borrow never asked the availability service (the stub held no request "
                        + "for " + barcode + " within 10s): the drain test must hold the borrow across the "
                        + "stop with the availability read already on the wire, and this borrow was spent "
                        + "elsewhere - refused by the chain, retold by a MockMvc rig, or pointed at an "
                        + "availability service this run's stub is not. The wire to the availability "
                        + "service is the thing to repair first; the drain cannot be held over a call "
                        + "that is not being made.");
                return; // not reached: fail never returns
            }
            TimeUnit.MILLISECONDS.sleep(50);
            waitedMillis += 50;
        }
    }

    private HttpRequest.Builder newApiRequest(String apiBase, String path) {
        return HttpRequest.newBuilder(URI.create(apiBase + path)).timeout(Duration.ofSeconds(30));
    }

    private String basic(String username, String password) {
        return "Basic "
                + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A borrow's answer at the wire: the status it carried and the body it
     * carried, or the failure the wire produced - both readable, either way.
     *
     * @param status the borrow's status; a negative one means the round trip
     *        never completed
     * @param body the borrow's body as it was answered
     * @param memberId the Member the Loan in the body was issued to, or an
     *        empty string when no Loan was
     */
    private record Answer(int status, String body, String memberId) {

        static final Answer A_BORROW_THAT_NEVER_CAME_BACK = new Answer(-1, "", "");
    }

    /**
     * One Title with one Copy on the shelf - the Copy the borrow must be
     * issued against - and the availability service answering that it is
     * available, slowly: the answer that keeps the borrow on the wire for the
     * stop to begin beneath.
     */
    private String writeACopyOnTheShelfAndSlowTheAnswer(String jdbcUrl) {
        JdbcClient jdbcClient = jdbcClientFor(jdbcUrl);
        String barcode = "bc-" + UUID.randomUUID();
        String isbn = "isbn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
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
        availabilityService.stubFor(get(urlEqualTo("/api/availability/" + barcode))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(Math.toIntExact(THE_ANSWER_TAKES_MILLIS))
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + barcode + "\",\"available\":true}")));
        return barcode;
    }
}
