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
import java.time.Duration;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import tools.jackson.databind.JsonNode;
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
 * Diagnostics for the M09 lab: the application says what it is doing while it
 * runs.
 *
 * <p>The application was left with the M08 accounting right and nothing
 * observable from outside the process: the actuator is not in the build at
 * all, no metric about Loans exists anywhere, and the one socket the process
 * owns serves only the public API. These tests state what the observability
 * upgrade adds: the liveness and readiness probes answering (which on 4.1.1
 * is the actuator starter's own default, no probe property needed), a
 * {@code library.loans.issued} counter published where the Loan is issued
 * and readable through the metrics endpoint, and a management surface riding
 * its own port while the public API's socket serves no actuator path.
 *
 * <p>They are excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/*Test.java}) so that the start module stays green
 * while the observability is still absent. Run them with:
 *
 * <pre>./mvnw -f labs -pl lab-09-observability-start verify -Pdiagnostics</pre>
 *
 * <p>Diagnostics are asserted in CI with the solutions-only profile: the same
 * tests, run only against the reference implementations by
 * {@code ./mvnw -f labs verify -Pdiagnostics-solutions}.
 *
 * <p>Not a MockMvc rig, on purpose: the lab's requirement is about sockets -
 * the management surface must answer on a port the public API does not
 * serve, which is a question only the real network answers. The application
 * boots whole on its random port, the actuator is asked for at the URLs a
 * monitor would type, and the M08 borrow drives the Loan metric through the
 * boundary it already works at.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ObservabilityDiagnosticsTest {

    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static WireMockServer availabilityService;

    private final Environment environment;
    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    @Autowired
    ObservabilityDiagnosticsTest(Environment environment, JdbcClient jdbcClient, JsonMapper jsonMapper) {
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
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:lab09-" + UUID.randomUUID());
        registry.add("spring.datasource.username", () -> "sa");
    }

    /**
     * The first requirement: the liveness and readiness probes answer.
     * Liveness answers "restart me?" and readiness "route me?", both with the
     * states the process itself holds - and on 4.1.1 both answer as soon as
     * the actuator starter is in the build, because
     * {@code management.endpoint.health.probes.enabled} defaults to true
     * ({@code matchIfMissing = true}) with no Kubernetes and no probe
     * property anywhere.
     */
    @Test
    void theProbesAnswer() {
        String managementPort = managementPort();
        assertThat(managementPort)
                .as("The application opens no management socket of its own (the property "
                        + "local.management.port, which this diagnostic reads the way a monitor "
                        + "reads it, is absent - the application boots with one socket and serves "
                        + "nothing but the public API on it): the actuator is not in the build at "
                        + "all, no spring-boot-starter-actuator in the POM. Add the starter and "
                        + "give the management server its own port with management.server.port in "
                        + "the application.yml. Nothing has to be switched on for the probes: on "
                        + "4.1.1 liveness and readiness answer with no probe property written "
                        + "(management.endpoint.health.probes.enabled defaults to true, "
                        + "matchIfMissing = true - the Boot 2 'the property, or Kubernetes' rule "
                        + "is gone), and the health endpoint the probe paths ride is in the "
                        + "starter's default exposure.")
                .isNotNull();

        String managementBase = managementBase(managementPort);
        assertThat(theHealthAt(managementBase, "/actuator/health/liveness"))
                .as("The liveness probe did not answer UP on the management socket: liveness "
                        + "answers about the process itself, and on a 4.1.1 starter-only "
                        + "classpath it is UP with no indicator of ours written anywhere. If the "
                        + "plain health endpoint answers but this path does not, the application "
                        + "is not on 4.1.1's probe default and something wrote a probe property; "
                        + "if it answers DOWN, something wired a dependency check into liveness - "
                        + "the defect this module's doc names: the platform's restart decision is "
                        + "not a database outage's trigger.")
                .isEqualTo("UP");
        assertThat(theHealthAt(managementBase, "/actuator/health/readiness"))
                .as("The readiness probe did not answer UP on the management socket: readiness is "
                        + "the routing decision - ACCEPTING_TRAFFIC answers as UP - and on 4.1.1 "
                        + "an application that publishes no availability event of its own answers "
                        + "UP for free. The state moves by event "
                        + "(AvailabilityChangeEvent.publish(context, state)), not by a boolean a "
                        + "probe reads; if something toggles a field instead, the read probe and "
                        + "every other listener begin to disagree.")
                .isEqualTo("UP");
    }

    /**
     * The second requirement: a Loan issued is a metric the registry holds -
     * published where the loan is issued, named in the glossary's words, and
     * tagged with a bounded dimension. The pieces cannot be separated: a
     * counter nobody can read is not published, and the diagnostics read it
     * the cheapest honest way, through {@code /actuator/metrics}.
     */
    @Test
    void aLoanIssuedIsAMetricTheRegistryHolds() throws IOException, InterruptedException {
        String barcode = writeACopyOnTheShelfOfARegisteredTitle();
        HttpResponse<String> created = borrow(aMemberNobodyKnowsAbout(), barcode);
        assertThat(created.statusCode())
                .as(
                        "The borrow of a Copy the availability service says is on the shelf was not a "
                                + "created loan (%s): this lab changes what the Loan feature says while it "
                                + "runs, not the accounting - the M08 borrow must keep working untouched.",
                        created.statusCode())
                .isEqualTo(201);

        String managementPort = managementPort();
        assertThat(managementPort)
                .as("The metric about loans has nowhere to read back: the application opens no "
                        + "management socket (the property local.management.port, which this "
                        + "diagnostic reads the way a monitor reads it, is absent - no "
                        + "spring-boot-starter-actuator in the POM, no management.server.port "
                        + "set). The four pieces of the requirement, in the order a review reads "
                        + "them: the counter is published in the Loan feature's use case (a "
                        + "MeterRegistry the starter brings, library.loans.issued incremented "
                        + "where the Loan row is written - the use case issues the loan, so the "
                        + "increment lives there, not in the controller and not in a stats "
                        + "gatherer afterwards); the tag is the Title the Loan was issued against "
                        + "(a dimension the catalogue bounds - a Copy identifier, a Member id, or "
                        + "anything one request can invent would make the counter its own zoo of "
                        + "time series); the metrics endpoint is exposed "
                        + "(management.endpoints.web.exposure.include must name health,metrics); "
                        + "and the management server rides its own port (management.server.port), "
                        + "so the reads stay away from the public API's socket.")
                .isNotNull();

        HttpResponse<String> meter = fetch(managementBase(managementPort), "/actuator/metrics/library.loans.issued");
        assertThat(meter.statusCode())
                .as(
                        "GET /actuator/metrics/library.loans.issued answered %s: either the counter "
                                + "is not published - a MeterRegistry in the Loan feature's use case, "
                                + "library.loans.issued incremented where the Loan row is written, tagged "
                                + "with the Title - or the metrics endpoint is not on the web exposure "
                                + "list, and the read the dashboard would make 404s exactly as a "
                                + "stranger's would: management.endpoints.web.exposure.include must name "
                                + "health,metrics.",
                        meter.statusCode())
                .isEqualTo(200);

        JsonNode meterBody = jsonMapper.readTree(meter.body());
        assertThat(meterBody.path("name").asString())
                .as(
                        "The metric the endpoint answered with is not the Loan metric (the name read "
                                + "back: %s): the name is the sentence the glossary speaks - "
                                + "library.loans.issued, a Loan was issued - not a request counter, not a "
                                + "view of http.server.requests, and not a metric whose words the code "
                                + "and the docs would have to translate.",
                        meterBody.path("name").asString())
                .isEqualTo("library.loans.issued");

        JsonNode titleTag = firstTagNamed(meterBody, "title");
        assertThat(titleTag)
                .as(
                        "The counter carries no title tag (the tags it carries: %s): the tag is the "
                                + "dimension a dashboard slices on, and the bounded one the glossary "
                                + "offers is the Title the Loan was issued against - a value the "
                                + "catalogue bounds. A per-request identifier or a Copy identifier would "
                                + "turn the counter into its own zoo of series.",
                        meterBody.path("availableTags"))
                .isNotNull();
        assertThat(titleTag.path("values").size())
                .as("The counter's title tag carries no values: the borrow above went through the "
                        + "M08 boundary and issued a Loan for a real Title, so one increment with "
                        + "one Title's value must already be in the registry.")
                .isGreaterThanOrEqualTo(1);

        JsonNode count = countMeasurement(meterBody);
        assertThat(count)
                .as(
                        "The counter has no COUNT measurement to read (the measurements it carries: "
                                + "%s): that is the format the metrics endpoint answers counters in - if "
                                + "the endpoint holds the name but speaks another shape, something "
                                + "published a non-counter meter where the lab asks for the plain, "
                                + "additive one.",
                        meterBody.path("measurements"))
                .isNotNull();
        assertThat(count.path("value").doubleValue())
                .as(
                        "The counter exists and carries its tag but has counted nothing (%s): the "
                                + "increment belongs where the loan is issued - the use case that writes "
                                + "the Loan row is the one that counts the issuance - so one borrow of "
                                + "one Copy on the shelf leaves one count behind.",
                        count)
                .isGreaterThanOrEqualTo(1.0);
    }

    /**
     * The third requirement: the management surface is not the public API's
     * socket. The starter's exposure defaults already keep the inspection
     * endpoints off the web; the stronger line the lab holds is that no
     * actuator path serves on the socket the titles, loans and reservations
     * serve on - the management server moves to its own port, and the public
     * ingress never reaches a management path at all.
     */
    @Test
    void theManagementSurfaceIsAwayFromThePublicApi() throws IOException, InterruptedException {
        HttpResponse<String> publicHealth = fetch(apiBase(), "/actuator/health");
        assertThat(publicHealth.statusCode())
                .as(
                        "The public API's socket answered /actuator/health with %s: the management "
                                + "endpoints must not be reachable from the public API. With the actuator "
                                + "in the build and no management port configured, every exposed "
                                + "endpoint - health among them - serves on the same socket the titles, "
                                + "loans and reservations serve on, and the public ingress, its rate "
                                + "limits and its certificate reach a management path. Move the actuator "
                                + "to its own socket with management.server.port: the main context "
                                + "serves no actuator path at all, and this request earns the 404 any "
                                + "unknown path earns.",
                        publicHealth.statusCode())
                .isEqualTo(404);

        String managementPort = managementPort();
        assertThat(managementPort)
                .as("The application opens no management socket: this diagnostic reads the "
                        + "local.management.port property exactly the way a monitor discovers "
                        + "where to ask, and it is absent - the actuator starter is not in the "
                        + "POM and no management.server.port is set. Add the starter and set "
                        + "management.server.port in the application.yml, to any port different "
                        + "from the API's: which of the two the deployment's ingress may reach "
                        + "is the deployment's decision, and the application's is only that the "
                        + "management surface exists and is not the public API's socket.")
                .isNotNull();

        String status = theHealthAt(managementBase(managementPort), "/actuator/health");
        assertThat(status)
                .as(
                        "The management socket answered, but its /actuator/health did not say UP "
                                + "(read back: %s): the health endpoint is the starter's default web "
                                + "exposure, so once the socket exists this read is free - no exposure "
                                + "line of ours is needed for it, and if it answers anything but UP "
                                + "something wrote a health detail the module's never-exposed list does "
                                + "not license.",
                        status)
                .isEqualTo("UP");
    }

    /**
     * The management port, read the way a monitor discovers it: the property
     * the management server publishes once it rides a socket of its own. It
     * is null on the start module's one-socket boot, and every diagnostic
     * fails on that null with the message naming the missing management
     * server and the starter it must arrive with.
     */
    private String managementPort() {
        return environment.getProperty("local.management.port");
    }

    /**
     * The public API's socket, read the way the application's own clients
     * discover it: the property the web server publishes on the random port
     * these diagnostics boot it on.
     */
    private String apiBase() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    private String managementBase(String managementPort) {
        return "http://localhost:" + managementPort;
    }

    private HttpResponse<String> fetch(String base, String path) throws IOException, InterruptedException {
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(base + path)).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * The health status of one actuator health path, read off the socket
     * whose base the call names. A request answered outside 200 is a failure
     * that names the path and the body, and a socket that never answers is a
     * failure that names the socket - not an error stack the learner has to
     * decode.
     */
    private String theHealthAt(String base, String path) {
        try {
            HttpResponse<String> response = fetch(base, path);
            assertThat(response.statusCode())
                    .as(
                            "GET %s%s answered %s (%s): the health path the probes ride must answer "
                                    + "200 - on 4.1.1 the probes and the plain health endpoint are the "
                                    + "starter's default exposure, no exposure line of ours belongs in "
                                    + "the change.",
                            base, path, response.statusCode(), response.body())
                    .isEqualTo(200);
            return jsonMapper.readTree(response.body()).path("status").asString();
        } catch (IOException | InterruptedException theSocketDidNotAnswer) {
            if (theSocketDidNotAnswer instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            fail(
                    "The socket %s did not answer a request for %s: the port the property "
                            + "local.management.port names must be the management server's own, and once "
                            + "the starter is in the build the actuator answers there with no further "
                            + "configuration.",
                    base, path, theSocketDidNotAnswer);
            return null; // not reached: fail never returns
        }
    }

    private JsonNode firstTagNamed(JsonNode meter, String key) {
        for (JsonNode tag : meter.path("availableTags")) {
            if (key.equals(tag.path("tag").asString())) {
                return tag;
            }
        }
        return null;
    }

    private JsonNode countMeasurement(JsonNode meter) {
        for (JsonNode measurement : meter.path("measurements")) {
            if ("COUNT".equals(measurement.path("statistic").asString())) {
                return measurement;
            }
        }
        return null;
    }

    private HttpResponse<String> borrow(String memberId, String copyId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase() + "/api/loans"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"memberId\":\"" + memberId + "\",\"copyId\":\"" + copyId + "\"}"))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * A member id no other test can have a loan for: the Active-loan state
     * the library holds is per member, and the diagnostics borrow under ids
     * a neighbouring run cannot have filled up.
     */
    private String aMemberNobodyKnowsAbout() {
        return "m-" + UUID.randomUUID();
    }

    /**
     * One Title with one Copy on the shelf, written straight into the
     * database - the Copy the borrow must be issued against, with the Title
     * its Loan's metric tag will name.
     */
    private String writeACopyOnTheShelfOfARegisteredTitle() {
        String isbn = "isbn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
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
}
