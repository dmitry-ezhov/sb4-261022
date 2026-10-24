package com.example.library.diagnostics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
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
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
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
 * Diagnostics for the M13 lab's second move: the bounds whose numbers are
 * decisions arriving from the environment at boot, not living only in the
 * jar's defaults.
 *
 * <p>The assembled application names three bounds its deployment can hold it
 * to: the drain window the graceful shutdown runs in, the replay geometry
 * the availability read is repeated under, and the client timeouts every
 * built client inherits. On the start module the drain window and the
 * timeouts are already properties the environment sets - the M11 and M04
 * moves - but the replay geometry is compiled into the
 * {@code @Retryable}'s own numbers, so an override the deployment names
 * reaches the environment and changes nothing: the request that a budget of
 * one attempt must let die is still carried through by the annotation's.
 * This diagnostic applies the overrides a deployment would apply to a real
 * boot - one boot, everything overridden at once:
 *
 * <ul>
 * <li>{@code library.availability.max-retries=0}, a replay budget of one
 * attempt: a wire blip - one connection reset, the service standing again
 * the next moment - is now an answer, not a replay, and the borrow answers
 * its {@code 502} with nothing carried; the retry counter the operator
 * reads answers for the one spent attempt and no more, where the compiled
 * geometry of three attempts publishes what it spends;</li>
 * <li>{@code spring.http.clients.connect-timeout=500ms} and
 * {@code spring.http.clients.read-timeout=5s}, the timeouts the
 * deployment names for every client the builder builds: the connect bound
 * the diagnostics read back carried off the booted environment - no
 * connect on a localhost wire is slow enough to feel a half second - and
 * the read bound on its own wire, an availability answer that takes its
 * time past the jar's own two-second default but inside the named five,
 * so the borrow it belongs to lands where the jar's own default would
 * have timed the read dead and answered its {@code 502};</li>
 * <li>{@code spring.lifecycle.timeout-per-shutdown-phase=3s}, the window the
 * drain runs in named by the deployment: a borrow begun across the stop
 * still lands inside it.</li>
 * </ul>
 *
 * <p>Excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/&#42;Test.java}); run with:
 *
 * <pre>./mvnw -f labs -pl lab-13-the-production-capstone-start verify -Pdiagnostics</pre>
 *
 * <p>Not a MockMvc rig, on purpose: the overrides are the ones a deployment
 * spells on its command line - the one property source that outranks the
 * jar's own yml - so the process that keeps them is the whole application,
 * booted by the test itself on its own H2 over this run's WireMock, and
 * stopped by the test itself, the way a deployment stops it.
 */
class BoundsDiagnosticsTest {

    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** The Member whose borrows test the bounds, the credentials M10's chain accepts. */
    private static final String MEMBER_NAME = "ada";

    private static final String MEMBER_PASSWORD = "member-password";

    /**
     * The replay budget the deployment names here: zero retries, one attempt
     * in total. Under it a single wire failure is a spent budget - the
     * borrow answers its 502 and no retry counting is left to see.
     */
    private static final String THE_NAMED_BUDGET = "--library.availability.max-retries=0";

    /**
     * The connect timeout the deployment names here: half a second. No
     * connect on the localhost wire the borrows run over is slow enough to
     * feel it, so its work in these diagnostics is being carried - read
     * back off the booted environment, the same read that vouches for the
     * other command-line values.
     */
    private static final String THE_NAMED_CONNECT_TIMEOUT = "--spring.http.clients.connect-timeout=500ms";

    /**
     * The read timeout the deployment names here: five seconds, longer
     * than the jar's own two - the bound with teeth on the wire. An
     * availability answer that spends the time between them on the wire
     * dies under the jar's own default and lands under the named bound:
     * the borrow waiting on it is the read that says which of the two
     * clocks the built client keeps.
     */
    private static final String THE_NAMED_READ_TIMEOUT = "--spring.http.clients.read-timeout=5s";

    /**
     * The drain window the deployment names here: three seconds, briefly
     * enough that a stopped process is not held for the half-minute
     * defaults nobody discussed, honestly enough that one in-flight borrow -
     * a bounded availability read plus one transaction - lands inside it.
     */
    private static final String THE_NAMED_WINDOW = "--spring.lifecycle.timeout-per-shutdown-phase=3s";

    /**
     * How long the stubbed availability answer keeps the recovery borrow on
     * the wire: comfortably past the point the stop begins, comfortably
     * inside any window the application could seriously name for its own
     * in-flight work.
     */
    private static final long THE_ANSWER_TAKES_MILLIS = 1500;

    /**
     * How long the lingering availability answer keeps the read on the
     * wire before it lands: past the two seconds the jar's own
     * read-timeout default would have let it live, inside the five seconds
     * the deployment's named bound keeps it under.
     */
    private static final long THE_SLOW_LANDING_TAKES_MILLIS = 2500;

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
     * The requirement, halves pleaded against each other on one boot: the
     * overrides a deployment spells on its command line reach the booted
     * process, the replay geometry the deployment named is the one the
     * replays keep - one attempt answers the blip with a 502 and no retry
     * count - and the window the deployment named is the one the drain
     * keeps, the borrow begun across the stop landing inside it.
     */
    @Test
    void theBoundsTheDeploymentSuppliesAreTheOnesTheProcessKeeps() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:lab13-bounds-" + UUID.randomUUID();

        ConfigurableApplicationContext application = bootTheApplication(jdbcUrl);
        try {
            assertThat(application.getEnvironment().getProperty("library.availability.max-retries"))
                    .as(
                            "The booted process does not carry the replay budget the diagnostics named "
                                    + "(%s) on its own command line: the process the diagnostics booted is "
                                    + "the one the bounds were applied to, and if the override did not "
                                    + "reach the environment, the boot is not the one this test judges. "
                                    + "The wire reads below are where the bound is actually kept.",
                            THE_NAMED_BUDGET)
                    .isEqualTo("0");
            assertThat(application.getEnvironment().getProperty("spring.http.clients.connect-timeout"))
                    .as(
                            "The booted process does not carry the connect timeout the diagnostics named "
                                    + "(%s) on its own command line: the process the diagnostics booted is "
                                    + "the one the bounds were applied to, and the named bound is the one "
                                    + "the environment carries. No connect on the localhost wire below is "
                                    + "slow enough to feel a half second, so here the deployment's say is "
                                    + "the value itself, read back.",
                            THE_NAMED_CONNECT_TIMEOUT)
                    .isEqualTo("500ms");
            assertThat(application.getEnvironment().getProperty("spring.http.clients.read-timeout"))
                    .as(
                            "The booted process does not carry the read timeout the diagnostics named "
                                    + "(%s) on its own command line: the process the diagnostics booted is "
                                    + "the one the bounds were applied to, and the lingering read below is "
                                    + "then timed not on the bound the deployment named but on whichever "
                                    + "clock the environment failed to override.",
                            THE_NAMED_READ_TIMEOUT)
                    .isEqualTo("5s");
            assertThat(application.getEnvironment().getProperty("spring.lifecycle.timeout-per-shutdown-phase"))
                    .as(
                            "The booted process does not carry the drain window the diagnostics named "
                                    + "(%s) on its own command line: the process the diagnostics booted is "
                                    + "the one the bounds were applied to, and if the override did not "
                                    + "reach the environment, the stop below is not the stopped process "
                                    + "the bounds were applied to.",
                            THE_NAMED_WINDOW)
                    .isEqualTo("3s");

            String apiBase = "http://localhost:" + application.getEnvironment().getProperty("local.server.port");
            String managementBase =
                    "http://localhost:" + application.getEnvironment().getProperty("local.management.port");

            // First half: the geometry. One reset, then the service stands
            // again - the exact shape the default geometry carries a borrow
            // through, and exactly what a budget of one attempt must let die.
            String blippedBarcode = writeACopyOnTheShelfAndBlipTheWire(jdbcUrl);
            HttpResponse<String> borrow = borrowOnce(apiBase, MEMBER_NAME, MEMBER_PASSWORD, blippedBarcode);
            assertThat(borrow.statusCode())
                    .as(
                            "With the deployment's budget of one attempt (%s) on the command line, the "
                                    + "blipped borrow still answered %s: one connection reset drowned, the "
                                    + "second attempt made it through, and a renegotiation the deployment "
                                    + "never named carried the call. The replay geometry is compiled "
                                    + "into the @Retryable's own numbers, so the environment reaches it "
                                    + "and answers nothing. The bound whose numbers are decisions must "
                                    + "arrive from the environment at boot: bind the geometry as "
                                    + "configuration properties beside library.availability.base-url - "
                                    + "library.availability.max-retries, with the delay, jitter, "
                                    + "multiplier and max-delay siblings beside it - and feed the "
                                    + "annotation the environment's answer through its String "
                                    + "variants, maxRetriesString and kin, which carry ${...} "
                                    + "placeholders: @Retryable(maxRetriesString = "
                                    + "\"${library.availability.max-retries:2}\") keeps the jar's own "
                                    + "geometry when the deployment names none and takes the "
                                    + "deployment's when it does. A budget of one attempt then lands "
                                    + "the blipped borrow on its 502 exactly as this read asks.",
                            THE_NAMED_BUDGET, borrow.statusCode())
                    .isEqualTo(502);

            HttpResponse<String> retried =
                    fetchWithOperator(managementBase, "/actuator/metrics/library.availability.retried");
            assertThat(countOf(retried))
                    .as(
                            "The Operator's read of the library.availability.retried metric carries a count of "
                                    + "%s after the blipped borrow died on its one attempt: the count the "
                                    + "deployment's budget of one attempt owes is two - the MethodRetryEvent "
                                    + "the framework publishes for the execution that failed, plus the "
                                    + "budget-exhausted mark the retry processing publishes when it aborts "
                                    + "the spent call. Anything above it is another attempt the environment's "
                                    + "budget never allowed - the compiled-in geometry of three attempts "
                                    + "spending itself, the read the same borrow earns without the "
                                    + "deployment's say.",
                            countOf(retried))
                    .isLessThanOrEqualTo(2.0);
            assertThat(borrow.headers().firstValue("Content-Type").orElse(""))
                    .as(
                            "The blipped borrow under the deployment's budget answered %s: the spend is the "
                                    + "same refusal the default exhaustion answers, on the same M08 surface "
                                    + "- 502, problem detail, titled in the availability feature's words. "
                                    + "The geometry changing is the deployment's business; the surface it "
                                    + "refuses on is the application's.",
                            borrow.headers().firstValue("Content-Type").orElse(""))
                    .startsWith("application/problem+json");

            // Second half: the client timeouts. One read the deployment's
            // named bound keeps: the answer takes its time on the wire -
            // past the jar's own two-second default, inside the named five -
            // and the borrow waiting on it lands as a Loan, where the jar's
            // own default would have timed the read dead and answered its
            // 502 on a budget of one attempt.
            String lingeringBarcode = writeACopyOnTheShelfAndLetTheAnswerTakeItsTime(jdbcUrl);
            HttpResponse<String> slowLanding = borrowOnce(apiBase, MEMBER_NAME, MEMBER_PASSWORD, lingeringBarcode);
            assertThat(slowLanding.statusCode())
                    .as(
                            "The borrow whose availability answer spent %d ms on the wire - past the two "
                                    + "seconds the jar's own read-timeout default keeps an answer under, "
                                    + "inside the five seconds the deployment named (%s) - answered %s "
                                    + "instead of landing as a Loan: the built client timed the read on a "
                                    + "clock the deployment did not name. The timeouts are the "
                                    + "environment's bounds like the budget and the window - spelled on "
                                    + "the command line, the property source that outranks the jar's own "
                                    + "yml - carried by the HttpClientSettings the client "
                                    + "auto-configuration publishes, and inherited by every client built "
                                    + "from the injected builder, of which the availability client is "
                                    + "one. Under the jar's own two seconds this read dies and the "
                                    + "budget of one attempt (%s) answers it with this same refuser; a "
                                    + "201 here is only an application whose built client keeps the "
                                    + "deployment's five seconds - the read that would not land was "
                                    + "never cut.",
                            THE_SLOW_LANDING_TAKES_MILLIS,
                            THE_NAMED_READ_TIMEOUT,
                            slowLanding.statusCode(),
                            THE_NAMED_BUDGET)
                    .isEqualTo(201);
            assertThat(jsonMapper.readTree(slowLanding.body()).path("memberId").asString())
                    .as(
                            "The Loan the lingering read's borrow landed was issued for %s, not for the "
                                    + "Member the credentials name: the borrow lands as it was issued - "
                                    + "the Authentication the chain answered - however long the "
                                    + "availability read it is grounded in took to get there.",
                            jsonMapper
                                    .readTree(slowLanding.body())
                                    .path("memberId")
                                    .asString())
                    .isEqualTo(MEMBER_NAME);

            // Third half: the window. The availability answer is on the wire
            // when the stop begins, and the borrow lands inside the window the
            // environment named - the deployment's own 3s, not the jar's
            // default, and not the drain dying early either.
            String inFlightBarcode = writeACopyOnTheShelfAndSlowTheAnswer(jdbcUrl);
            CompletableFuture<Answer> inFlight =
                    borrowAsynchronously(apiBase, MEMBER_NAME, MEMBER_PASSWORD, inFlightBarcode);
            waitUntilTheBorrowWaitsOnTheAvailabilityAnswer(inFlightBarcode);
            assertThat(inFlight.isDone())
                    .as("The in-flight borrow completed before the stop began: the drain half of this "
                            + "test must hold the borrow across the stop, and this one was already "
                            + "answered before the application could be stopped beneath it. The "
                            + "stubbed availability answer holds the request on the wire for "
                            + THE_ANSWER_TAKES_MILLIS
                            + "ms; begin the stop after the availability request has been made "
                            + "and while it is still being waited on.")
                    .isFalse();

            application.close();

            assertThat(inFlight.join().status())
                    .as(
                            "The borrow begun across the stop did not land as 201 inside the window the "
                                    + "deployment named (%s): the drain is the boot's default - the flip "
                                    + "one generation before this stack - and the window it runs in is the "
                                    + "line the M11 lab made a decision. If the borrow died mid-flight, the "
                                    + "process stopped without keeping it, or the environment's window was "
                                    + "too brief for one bounded availability read plus one transaction; "
                                    + "if the test timed out waiting, the drain outlived the named window "
                                    + "and the old default stood in again.",
                            THE_NAMED_WINDOW)
                    .isEqualTo(201);
            assertThat(inFlight.join().memberId())
                    .as(
                            "The Loan the drain carried over the stop was issued for %s, not for the "
                                    + "Member the credentials name: the borrow lands as it was issued - the "
                                    + "Authentication the chain answered - not as something else the drain "
                                    + "produced.",
                            inFlight.join().memberId())
                    .isEqualTo(MEMBER_NAME);
        } finally {
            if (application.isActive()) {
                application.close();
            }
        }
    }

    /* ------------------------------------------------------------------
     * The boot: one LibraryApplication, on a random API socket over its own
     * H2 and this run's WireMock, the deployment's bounds spelled on its
     * command line.
     * ------------------------------------------------------------------ */

    /**
     * The application, booted the way a deployment boots it: servlet over an
     * API socket on port zero, the management surface on its own socket, the
     * H2 and the availability URL of this run - and the four bounds the
     * deployment names riding the command line, the one property source that
     * outranks the jar's own yml, exactly the overrides without a rebuild a
     * deployment makes. Its migration makes the tables, its connections keep
     * the in-memory database open - the Copy rows the test writes land in
     * that database, and the application reads them back.
     */
    private ConfigurableApplicationContext bootTheApplication(String jdbcUrl) {
        return new SpringApplicationBuilder(LibraryApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--server.port=0",
                        "--management.server.port=0",
                        "--library.availability.base-url=http://localhost:" + availabilityService.port(),
                        "--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.username=sa",
                        THE_NAMED_BUDGET,
                        THE_NAMED_CONNECT_TIMEOUT,
                        THE_NAMED_READ_TIMEOUT,
                        THE_NAMED_WINDOW);
    }

    /* ------------------------------------------------------------------
     * The wires: the borrows, the Copy rows and the stubs.
     * ------------------------------------------------------------------ */

    /**
     * One borrow, sent and waited for: the credentials on the wire and a
     * body naming only the Copy, the request carrying no Member - the borrow
     * is in the principal's name however the body is written.
     */
    private HttpResponse<String> borrowOnce(String apiBase, String username, String password, String copyId) {
        HttpRequest request = request(URI.create(apiBase + "/api/loans"))
                .header("Content-Type", "application/json")
                .header("Authorization", basic(username, password))
                .POST(HttpRequest.BodyPublishers.ofString("{\"copyId\":\"" + copyId + "\"}"))
                .build();
        try {
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException theSocketDidNotAnswer) {
            return failOnUnansweredSocket(apiBase, "/api/loans", theSocketDidNotAnswer);
        }
    }

    private CompletableFuture<Answer> borrowAsynchronously(
            String apiBase, String username, String password, String copyId) {
        HttpRequest request = request(URI.create(apiBase + "/api/loans"))
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
     * A read of the management surface the M10 chain commands: the
     * Operator's credential on the wire, basic as the chain speaks it. Where
     * the metric is absent - a counter no increment ever registered - the
     * endpoint answers 404 and {@link #countOf(HttpResponse)} reads it as
     * the zero it is.
     */
    private HttpResponse<String> fetchWithOperator(String base, String path) {
        HttpRequest request = request(URI.create(base + path))
                .header("Authorization", basic("operator", "operator-password"))
                .GET()
                .build();
        try {
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException theSocketDidNotAnswer) {
            return failOnUnansweredSocket(base, path, theSocketDidNotAnswer);
        }
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
                fail(
                        "The in-flight borrow never asked the availability service (the stub held no "
                                + "request for %s within 10s): the drain half must hold the borrow across "
                                + "the stop with the availability read already on the wire, and this "
                                + "borrow was spent elsewhere - refused by the chain, retold by a MockMvc "
                                + "rig, or pointed at an availability service this run's stub is not. "
                                + "The wire to the availability service is the thing to repair first; the "
                                + "drain cannot be held over a call that is not being made.",
                        barcode);
                return; // not reached: fail never returns
            }
            TimeUnit.MILLISECONDS.sleep(50);
            waitedMillis += 50;
        }
    }

    /**
     * The count the metric read carries: the metric's COUNT measurement,
     * read the way a dashboard reads it. An absent counter - a metric no
     * increment ever registered, answered with whatever status the endpoint
     * answers missing metrics with - reads as the zero it is.
     */
    private Double countOf(HttpResponse<String> metric) {
        if (metric.statusCode() != 200) {
            return 0.0;
        }
        var measurements = jsonMapper.readTree(metric.body()).path("measurements");
        for (int tried = 0; tried < measurements.size(); tried++) {
            var measurement = measurements.get(tried);
            if (measurement.path("statistic").asString().equals("COUNT")) {
                return measurement.path("value").asDouble();
            }
        }
        return 0.0;
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30));
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
     * issued against - written into the database the application booted over.
     */
    private void writeACopyOnTheShelf(String jdbcUrl, String barcode) {
        JdbcClient jdbcClient = jdbcClientFor(jdbcUrl);
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
    }

    /**
     * The blipped means: the Copy on the shelf, and the availability service
     * dropping the connection the first time this Copy is asked about, then
     * answering normally - the fault a default geometry carries a borrow
     * through and a budget of one attempt must let die.
     */
    private String writeACopyOnTheShelfAndBlipTheWire(String jdbcUrl) {
        String barcode = "bc-" + UUID.randomUUID();
        writeACopyOnTheShelf(jdbcUrl, barcode);
        availabilityService.stubFor(get(urlEqualTo("/api/availability/" + barcode))
                .inScenario("a blip the named budget must not carry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("recovered"));
        availabilityService.stubFor(get(urlEqualTo("/api/availability/" + barcode))
                .inScenario("a blip the named budget must not carry")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + barcode + "\",\"available\":true}")));
        return barcode;
    }

    /**
     * The slow means: the Copy on the shelf, and the availability service
     * answering that it is available, slowly - the answer that keeps the
     * borrow on the wire for the stop to begin beneath.
     */
    private String writeACopyOnTheShelfAndSlowTheAnswer(String jdbcUrl) {
        String barcode = "bc-" + UUID.randomUUID();
        writeACopyOnTheShelf(jdbcUrl, barcode);
        availabilityService.stubFor(get(urlEqualTo("/api/availability/" + barcode))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(Math.toIntExact(THE_ANSWER_TAKES_MILLIS))
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + barcode + "\",\"available\":true}")));
        return barcode;
    }

    /**
     * The lingering means: the Copy on the shelf, and the availability
     * service answering that it is available, but taking its time on the
     * wire - past the jar's own read-timeout default, inside the
     * deployment's named bound. The read that lands anyway is the proof
     * the built client keeps the clock the deployment named.
     */
    private String writeACopyOnTheShelfAndLetTheAnswerTakeItsTime(String jdbcUrl) {
        String barcode = "bc-" + UUID.randomUUID();
        writeACopyOnTheShelf(jdbcUrl, barcode);
        availabilityService.stubFor(get(urlEqualTo("/api/availability/" + barcode))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(Math.toIntExact(THE_SLOW_LANDING_TAKES_MILLIS))
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + barcode + "\",\"available\":true}")));
        return barcode;
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
                "The socket %s did not answer a request for %s: the application the diagnostics booted "
                        + "owns both sockets, and reading them is the first thing to repair. The cause is "
                        + "the unreadable answer's.",
                base, path, cause);
        return null; // not reached: fail never returns
    }
}
