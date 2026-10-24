package com.example.library.diagnostics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
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
 * Diagnostics for the M13 lab's capstone move: the outage told to the
 * platform as state.
 *
 * <p>The application stands exactly as the twelve Modules assembled it - the
 * borrows transactional behind the second lock, the requests on virtual
 * threads, the availability bounded and retried, the refusal map complete,
 * the two counters published, the two probes answering - and it answers its
 * probe questions with the states Boot handed it. When the availability
 * service stops answering, the refusals pile up as {@code 502} problem
 * details, the {@code library.availability.retried} counter climbs - and
 * readiness keeps answering the state it never decided, {@code UP}, through
 * every second of the outage. These tests state what the capstone adds on
 * top of everything that already works:
 *
 * <ul>
 * <li>the outage published the way the M09 doc taught and no module built -
 * {@code AvailabilityChangeEvent.publish(context,
 * ReadinessState.REFUSING_TRAFFIC)} when the availability service stops
 * answering, so the readiness probe the platform routes on answers
 * {@code OUT_OF_SERVICE};</li>
 * <li>the recovery published the same way - a later availability read that
 * lands publishes {@code ReadinessState.ACCEPTING_TRAFFIC}, and readiness
 * moves back;</li>
 * <li>nothing else changed by the outage or the publish: the catalogue's
 * reads stay public, the borrows keep answering their {@code 502}s and their
 * {@code 201}s, the counters keep counting - the refusal question was
 * already answered by the boundary, readiness is the routing one;</li>
 * <li>and liveness never moves: the two probes answer different questions,
 * and a restart is the platform's repair for a broken <em>process</em>,
 * never for a broken <em>dependency</em>.</li>
 * </ul>
 *
 * <p>Excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/&#42;Test.java}) so that the start module stays
 * green while the readiness state is still absent. Run them with:
 *
 * <pre>./mvnw -f labs -pl lab-13-the-production-capstone-start verify -Pdiagnostics</pre>
 *
 * <p>Diagnostics are asserted in CI with the solutions-only profile: the same
 * tests, run only against the reference implementations by
 * {@code ./mvnw -f labs verify -Pdiagnostics-solutions}.
 *
 * <p>The principals are the security lab's: the Member {@code ada}, password
 * {@code member-password}, holding the {@code MEMBER} role the lending
 * endpoints require, and the Operator {@code operator}, password
 * {@code operator-password}, whose credential the counters' reads command.
 * The readiness probe itself is public - the M10 chain permits
 * {@code /actuator/health/**} - because it is the read a monitor must be
 * able to make without a credential.
 *
 * <p>Not a MockMvc rig, on purpose: the outage is an event a whole
 * application lives through, and its evidence is what a caller and a monitor
 * can read - statuses, media types, probe states and counters over the
 * application's two sockets, with the availability service as a real service
 * that can drop the wire and pick it up again.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReadinessDiagnosticsTest {

    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** The Member whose borrows walk the outage, the credentials M10's chain accepts. */
    private static final String MEMBER_NAME = "ada";

    private static final String MEMBER_PASSWORD = "member-password";

    /** The Operator the management counters' reads command, an M10 credential. */
    private static final String OPERATOR_NAME = "operator";

    private static final String OPERATOR_PASSWORD = "operator-password";

    /**
     * How long the readiness probe gets to move after the moment that moves
     * it: the publish is in-process and instant for a probe that reads the
     * published state, and the patience is for the outage's story to arrive
     * in order - borrow refused, then state published - not for a slow probe
     * of its own.
     */
    private static final Duration READINESS_PATIENCE = Duration.ofSeconds(15);

    private static WireMockServer availabilityService;

    private final Environment environment;
    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    @Autowired
    ReadinessDiagnosticsTest(Environment environment, JdbcClient jdbcClient, JsonMapper jsonMapper) {
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
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:lab13-readiness-" + UUID.randomUUID());
        registry.add("spring.datasource.username", () -> "sa");
    }

    /**
     * The requirement, one story told in order: the availability service
     * answers, the readiness probe reads the state Boot handed it - then the
     * service goes silent, the borrow answers its {@code 502}, the retry
     * counter climbs, and the readiness probe says nothing the whole time,
     * because the outage exists only on the operator's side of the sockets.
     * The publish is the Lab's move; the recovery - one availability read
     * answered again, the recovery borrow landing, readiness moving back -
     * publishes the same way.
     */
    @Test
    void theOutageIsPublishedAsStateAndTheRecoveryMovesItBack() throws IOException, InterruptedException {
        HttpResponse<String> baseline = fetch(managementBase(), "/actuator/health/readiness");
        assertThat(baseline.statusCode())
                .as(
                        "The unauthenticated read of /actuator/health/readiness answered %s: the probe is"
                                + " the platform's routing question and the M10 chain keeps"
                                + " /actuator/health/** public - a monitor that cannot read it without a"
                                + " credential cannot route on it, and the outage's story below has"
                                + " nowhere to land. This baseline is the state the outage is measured"
                                + " against, not a requirement the Lab introduces - the M09 boot read"
                                + " 200 off both probes.",
                        baseline.statusCode())
                .isEqualTo(200);
        assertThat(readinessStatusOf(baseline))
                .as(
                        "The readiness probe answered %s before the outage: UP is what an application that"
                                + " has decided nothing reports, and the outage below is measured against"
                                + " exactly this. If a red lands here, the application the diagnostics"
                                + " booted is not the assembled one - something published before the"
                                + " outage began.",
                        readinessStatusOf(baseline))
                .isEqualTo("UP");

        String cutBarcode = writeACopyOnTheShelfAndCutTheWire();
        HttpResponse<String> borrow = borrow(MEMBER_NAME, MEMBER_PASSWORD, cutBarcode);
        assertThat(borrow.statusCode())
                .as(
                        "The borrow whose availability budget was spent answered %s, not a 502: the wire"
                                + " was cut for every attempt and a borrow under a fully dead dependency"
                                + " is the one case the M11 boundary answers in the domain's words -"
                                + " Bad Gateway, the status a service owes its caller when the dependency"
                                + " it fronts failed. This half is the twelve Modules' work and keeps"
                                + " working under the capstone: the outage changes what the process tells"
                                + " the platform, never what the boundary tells the Member.",
                        borrow.statusCode())
                .isEqualTo(502);
        assertThat(borrow.headers().firstValue("Content-Type").orElse(""))
                .as(
                        "The borrow under the cut wire answered %s: a 502 without the problem media type"
                                + " is the container's error page wearing the number. The M08 surface"
                                + " answers RFC 9457, application/problem+json, titled in the"
                                + " availability feature's words.",
                        borrow.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/problem+json");

        HttpResponse<String> retried =
                fetchWithOperator(managementBase(), "/actuator/metrics/library.availability.retried");
        assertThat(retried.statusCode())
                .as(
                        "The Operator's read of the library.availability.retried metric during the outage"
                                + " answered %s: the outage's signature is already observable from the"
                                + " process's own counters - every ask the retry carried publishes the"
                                + " MethodRetryEvent the M11 listener counts. An outage that registers"
                                + " itself nowhere an operator is watching doubles the work of the"
                                + " incident, and the readiness publish the Lab adds is not a substitute"
                                + " for the operator's counter but a second reading of the same story.",
                        retried.statusCode())
                .isEqualTo(200);
        assertThat(countOf(retried))
                .as(
                        "The Operator's outage read of library.availability.retried counted %s, no ask was"
                                + " carried: the wire was cut for every attempt, so every attempt after the"
                                + " first published a MethodRetryEvent, and the borrow could not have left"
                                + " the budget unspent. If the counter never moved, the borrow never reached"
                                + " the wire the diagnostics cut - the borrow that answered the 502 above"
                                + " is the one to trace.",
                        countOf(retried))
                .isGreaterThanOrEqualTo(1.0);

        awaitReadiness(
                "OUT_OF_SERVICE",
                "the readiness probe never left the state Boot handed it through the whole outage: the"
                        + " application answered every ask - the 502 the Member read, the retried counter"
                        + " the Operator read - but told the platform nothing, and routing stayed open"
                        + " into an outage the application was the first to know about. The Lab's move"
                        + " is the publish the M09 doc taught and no module built: when the availability"
                        + " service stops answering, publish the will as state -"
                        + " AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC) -"
                        + " the moment whose arrival is the spent budget's ResourceAccessException the"
                        + " boundary just answered 502 with. The probe then reads the published state and"
                        + " answers OUT_OF_SERVICE, the unroute-me half of the two probe questions. The"
                        + " state is published, not toggled: let the readiness indicator report the"
                        + " published application availability - do not teach it to re-ask the dependency"
                        + " itself, or the routing answer and the refusal answer can tell different"
                        + " stories, exactly the drift the outage just proved.");

        HttpResponse<String> liveness = fetch(managementBase(), "/actuator/health/liveness");
        assertThat(liveness.statusCode())
                .as(
                        "The unauthenticated read of /actuator/health/liveness answered %s during the"
                                + " outage: the liveness probe is on the same public surface the readiness"
                                + " probe is, and the publish must move readiness only - a restart is the"
                                + " platform's repair for a broken process, never for a broken dependency,"
                                + " so the refusing application stays alive.",
                        liveness.statusCode())
                .isEqualTo(200);
        assertThat(readinessStatusOf(liveness))
                .as(
                        "Liveness answered %s during the outage: the publish that refuses traffic is a"
                                + " routing decision and must not reach the restart question. The two"
                                + " probes answer two questions - liveness about the process, readiness"
                                + " about its routing - and only the routing one moved.",
                        readinessStatusOf(liveness))
                .isEqualTo("UP");

        HttpResponse<String> catalogue = fetch(apiBase(), "/api/titles");
        assertThat(catalogue.statusCode())
                .as(
                        "The public read of /api/titles during the outage answered %s: nothing outside"
                                + " the routing answer changed - the catalogue the M10 chain made public"
                                + " stays public, because readiness is the routing question and the"
                                + " refusal question was already answered by the boundary. An outage that"
                                + " closes what it did not need to close is a publish done by force.",
                        catalogue.statusCode())
                .isEqualTo(200);

        String healedBarcode = writeACopyOnTheShelfAndAnswerAvailable();
        HttpResponse<String> recovery = borrow(MEMBER_NAME, MEMBER_PASSWORD, healedBarcode);
        assertThat(recovery.statusCode())
                .as(
                        "The recovery borrow - the availability service answering normally again - did"
                                + " not land as 201: the outage refused the borrows whose availability read"
                                + " could not be had, and a read this one had. A readiness state that turns"
                                + " the borrows themselves down has misunderstood the routing question: the"
                                + " application inside the process is working; only the dependency in front"
                                + " of it was not.",
                        recovery.statusCode())
                .isEqualTo(201);
        assertThat(memberOfTheLoan(recovery))
                .as(
                        "The recovery borrow landed for %s, not for the Member the credentials name: the"
                                + " recovery issues a Loan exactly like any other - in the name of the"
                                + " Authentication the chain answered.",
                        memberOfTheLoan(recovery))
                .isEqualTo(MEMBER_NAME);

        awaitReadiness(
                "UP",
                "the availability service answered its next read - the recovery borrow landed - but the"
                        + " readiness probe never moved back: the state must be re-published the same way it"
                        + " was published, AvailabilityChangeEvent.publish(context,"
                        + " ReadinessState.ACCEPTING_TRAFFIC) when a read lands again, the recovery half of"
                        + " the outage's story. A probe that moves on the outage but not on the recovery"
                        + " routes nothing forever after, and the deployment that never hears UP again has"
                        + " to restart the application to be told it is well.");

        HttpResponse<String> issued = fetchWithOperator(managementBase(), "/actuator/metrics/library.loans.issued");
        assertThat(issued.statusCode())
                .as(
                        "The Operator's read of the library.loans.issued metric answered %s: the recovery"
                                + " borrow landed, and the M09 counter - whose increment sits where the Loan"
                                + " row is written - must answer for it. The counters keep counting"
                                + " throughout the outage; the readiness publish changed none of them.",
                        issued.statusCode())
                .isEqualTo(200);
        assertThat(countOf(issued))
                .as(
                        "The Operator's read of library.loans.issued counted %s after the recovery borrow"
                                + " landed: the issuance the Loan row recorded is the one the counter answers"
                                + " for. If the count never moved, the recovery borrow never reached its"
                                + " boundary - the Loan is the trace.",
                        countOf(issued))
                .isGreaterThanOrEqualTo(1.0);
    }

    /* ------------------------------------------------------------------
     * The wires: sockets, credentials, the copy rows and the stubs.
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
     * The management socket, read the way the M09 diagnostics read it: the
     * property the management server publishes once it rides a port of its
     * own.
     */
    private String managementBase() {
        return "http://localhost:" + environment.getProperty("local.management.port");
    }

    private HttpResponse<String> fetch(String base, String path) throws IOException, InterruptedException {
        return HTTP.send(request(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * A read of the management surface the M10 chain commands: the Operator's
     * credential on the wire, basic as the chain speaks it.
     */
    private HttpResponse<String> fetchWithOperator(String base, String path) {
        HttpRequest request = request(URI.create(base + path))
                .header("Authorization", basic(OPERATOR_NAME, OPERATOR_PASSWORD))
                .GET()
                .build();
        try {
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException theSocketDidNotAnswer) {
            return failOnUnansweredSocket(base, path, theSocketDidNotAnswer);
        }
    }

    /**
     * The borrow typed the way a Member's client types it: the credentials
     * on the wire and a body naming only the Copy - the copyId, every Copy's
     * external name. The request carries no Member: the borrow is in the
     * principal's name however the body is written.
     */
    private HttpResponse<String> borrow(String username, String password, String copyId) {
        HttpRequest request = request(URI.create(apiBase() + "/api/loans"))
                .header("Content-Type", "application/json")
                .header("Authorization", basic(username, password))
                .POST(HttpRequest.BodyPublishers.ofString("{\"copyId\":\"" + copyId + "\"}"))
                .build();
        try {
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException theSocketDidNotAnswer) {
            return failOnUnansweredSocket(apiBase(), "/api/loans", theSocketDidNotAnswer);
        }
    }

    /** The {@code memberId} field of the Loan a borrow's answer carries. */
    private String memberOfTheLoan(HttpResponse<String> body) {
        return jsonMapper.readTree(body.body()).path("memberId").asString();
    }

    /**
     * The readiness answer's status, the word the probe reports: UP,
     * OUT_OF_SERVICE, DOWN - the states the platform's router reads.
     */
    private String readinessStatusOf(HttpResponse<String> probe) {
        return jsonMapper.readTree(probe.body()).path("status").asString();
    }

    /**
     * One probe's state, awaited: the polling reads the probe plainly
     * unauthenticated and stops the moment it names the wanted state. Where
     * the whole patience is spent without the state appearing, the failure is
     * the guidance argument's - what is missing and what move it asks for.
     */
    private void awaitReadiness(String wantedStatus, String whatIsMissing) throws InterruptedException {
        long deadline = System.nanoTime() + READINESS_PATIENCE.toNanos();
        String lastStatus = null;
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<String> probe = fetch(managementBase(), "/actuator/health/readiness");
                if (probe.statusCode() == 200 || probe.statusCode() == 503) {
                    lastStatus = readinessStatusOf(probe);
                    if (wantedStatus.equals(lastStatus)) {
                        return;
                    }
                } else {
                    lastStatus = "the read answering " + probe.statusCode();
                }
            } catch (IOException theProbeWasUnreadableOnThisPass) {
                // The socket before the boot, or a read the poll simply
                // retries: a dropped probe read is not evidence, the whole
                // patience without one state change is.
                lastStatus = "a read that never landed";
            }
            Thread.sleep(250);
        }
        fail(
                "The readiness probe kept answering %s for the full %d-second patience: %s",
                lastStatus, READINESS_PATIENCE.toSeconds(), whatIsMissing);
    }

    /**
     * The count the metric read carries: the metric's COUNT measurement, read
     * the way a dashboard reads it.
     */
    private Double countOf(HttpResponse<String> metric) {
        var measurements = jsonMapper.readTree(metric.body()).path("measurements");
        for (int tried = 0; tried < measurements.size(); tried++) {
            var measurement = measurements.get(tried);
            if (measurement.path("statistic").asString().equals("COUNT")) {
                return measurement.path("value").asDouble();
            }
        }
        return null;
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30));
    }

    private String basic(String username, String password) {
        return "Basic "
                + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * One Title with one Copy on the shelf, written straight into the
     * database - the Copy a borrow must be issued against.
     */
    private void writeACopyOnTheShelf(String barcode) {
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
     * The cut-wire means: the Copy on the shelf, and the availability
     * service dropping the connection every time - every attempt of what the
     * caller's budget is worth, until it runs out.
     */
    private String writeACopyOnTheShelfAndCutTheWire() {
        String barcode = "bc-" + UUID.randomUUID();
        writeACopyOnTheShelf(barcode);
        availabilityService.stubFor(get(urlEqualTo("/api/availability/" + barcode))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        return barcode;
    }

    /**
     * The healed means: the Copy on the shelf, and the availability service
     * answering that it is available - the read the recovery publish needs.
     */
    private String writeACopyOnTheShelfAndAnswerAvailable() {
        String barcode = "bc-" + UUID.randomUUID();
        writeACopyOnTheShelf(barcode);
        availabilityService.stubFor(get(urlEqualTo("/api/availability/" + barcode))
                .willReturn(aResponse()
                        .withStatus(200)
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
                "The socket %s did not answer a request for %s: the application the diagnostics boot owns "
                        + "both sockets, and reading them is the first thing to repair. The cause is the "
                        + "unreadable answer's.",
                base, path, cause);
        return null; // not reached: fail never returns
    }
}
