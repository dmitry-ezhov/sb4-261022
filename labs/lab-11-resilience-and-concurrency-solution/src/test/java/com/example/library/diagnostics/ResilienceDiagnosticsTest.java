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
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
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
 * Diagnostics for the M11 lab: the fragile edge of the library is under the
 * treatment the M11 doc taught, and the requests ride the JDK's cheap
 * threads.
 *
 * <p>The application was left exactly as the security lab left it - chain
 * guarding the lending endpoints, the Member the principal, the probes
 * answering, the metric published - and none of this Module's resilience is
 * in it. The requests borrow the 200-strong platform pool,
 * {@code spring.threads.virtual.enabled} is not set anywhere; the
 * availability client M04 introduced repeats nothing, so a wire failure
 * surfaces as the container's error page the moment it happens; and the
 * retries happen nowhere a metric counts. These tests state what the upgrade
 * adds:
 *
 * <ul>
 * <li>the threading switched on with the property alone -
 * {@code spring.threads.virtual.enabled: true} - and observed on the
 * management socket the M10 chain commands: the Operator's
 * {@code /actuator/threaddump} lists no idle platform worker, because a
 * virtual handler exists for the length of its request and exists for
 * nothing after;</li>
 * <li>a wire blip on the availability call retried through - a connection
 * reset, then the service recovered - so the borrow lands instead of dying
 * on the first fault, with {@code @EnableResilientMethods} on a
 * configuration and {@code @Retryable} on the method the borrow calls, the
 * call crossing the proxy from outside its bean;</li>
 * <li>the retries countable - a listener of the {@code MethodRetryEvent}
 * publishing a {@code library.availability.retried} counter the Operator can
 * read beside M09's {@code library.loans.issued};</li>
 * <li>exhaustion answered in domain words: when the budget is spent the
 * boundary answers a problem detail on the M08 surface rather than the
 * container's error page, and never a hang.</li>
 * </ul>
 *
 * <p>They are excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/&#42;Test.java}) so that the start module stays
 * green while the resilience is still absent. Run them with:
 *
 * <pre>./mvnw -f labs -pl lab-11-resilience-and-concurrency-start verify -Pdiagnostics</pre>
 *
 * <p>Diagnostics are asserted in CI with the solutions-only profile: the same
 * tests, run only against the reference implementations by
 * {@code ./mvnw -f labs verify -Pdiagnostics-solutions}.
 *
 * <p>The principals: the Member {@code ada}, password {@code member-password},
 * holding the {@code MEMBER} role the lending endpoints require, and the
 * Operator {@code operator}, password {@code operator-password}, holding the
 * {@code OPERATOR} role the management surface's chain requires of its
 * callers - stored by the security lab's own {@code SecurityUsersConfiguration},
 * because the resilience lab changes nothing about who may ask.
 *
 * <p>Not a MockMvc rig, on purpose: the requirement is resilience seen at the
 * wire - statuses, media types and thread lists as a caller and an operator
 * meet them, with the availability service as a real service that can be
 * slowed and can drop the wire. The application boots whole on its random
 * port, the borrow arrives the way a Member's client sends it, and the
 * observability reads arrive with the Operator's credential on the
 * management socket M09 separated and M10 guarded.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResilienceDiagnosticsTest {

    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** The Member the borrow goes out as: the credentials M10's chain accepts. */
    private static final String MEMBER_NAME = "ada";

    private static final String MEMBER_PASSWORD = "member-password";

    /** The Operator the management socket's reads command, an M10 credential. */
    private static final String OPERATOR_NAME = "operator";

    private static final String OPERATOR_PASSWORD = "operator-password";

    /**
     * The wire failure the retry must recognise: Tomcat's platform worker
     * names, as this course's own boots printed them - one idle
     * {@code http-nio-<port>-exec-N} worker per socket the guard pool serves,
     * and no {@code tomcat-handler-} name anywhere, because that prefix
     * belongs to Tomcat's {@code VirtualThreadExecutor}, whose threads are
     * virtual and are listed by the dump endpoint never.
     */
    private static final Pattern PLATFORM_WORKER =
            Pattern.compile("\"threadName\"\\s*:\\s*\"http-nio-[-\\dA-Za-z]+-exec-");

    /** The VirtualThreadExecutor's own prefix, platform spelling of it: a defect. */
    private static final Pattern PLATFORM_HANDLER = Pattern.compile("\"threadName\"\\s*:\\s*\"tomcat-handler-");

    private static WireMockServer availabilityService;

    private final Environment environment;
    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    @Autowired
    ResilienceDiagnosticsTest(Environment environment, JdbcClient jdbcClient, JsonMapper jsonMapper) {
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
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:lab11-" + UUID.randomUUID());
        registry.add("spring.datasource.username", () -> "sa");
    }

    /**
     * The first requirement: the requests hold no platform worker, because
     * the switch is the property and it is on. The Operator reads the thread
     * dump the M10 chain commands - an authenticated read on the management
     * socket - and the list carries no idle handler of either spelling: not
     * the {@code http-nio-...-exec-} guard pool Boot 2 taught, and not a
     * platform {@code tomcat-handler-} worker, because the virtual executor's
     * threads are virtual and the dump endpoint lists them never.
     */
    @Test
    void theRequestsHoldNoPlatformWorkerBecauseTheSwitchIsOn() throws IOException, InterruptedException {
        // The dump is read after the application has served real requests;
        // whichever way the boot is threaded, the surface is the one M09
        // exposed and the operator credential commands.
        servedAnApiRequest();

        HttpResponse<String> dump = fetchWithOperator(managementBase(), "/actuator/threaddump");
        assertThat(dump.statusCode())
                .as(
                        "The Operator's read of /actuator/threaddump on the management socket answered %s: "
                                + "the observation the Lab asks for is a read nobody can make yet. Two pieces "
                                + "are missing, and the M11 doc's first move is both lines of it: expose the "
                                + "thread dump by adding threaddump to management.endpoints.web.exposure."
                                + "include (the health,metrics surface M09 named stops one step short of "
                                + "it), and switch the threading on with spring.threads.virtual.enabled: "
                                + "true in application.yml - the property the Boot 3 column of the delta "
                                + "table names (introduced with the 3.2 line, default still false in "
                                + "4.1.1). No Java belongs in the move: no Executors.newVirtual..., no "
                                + "thread factory, no pool rebuilt by hand.",
                        dump.statusCode())
                .isEqualTo(200);

        assertThat(PLATFORM_WORKER.matcher(dump.body()).find())
                .as(
                        "The thread dump still lists an idle tomcat worker of the http-nio-<port>-exec- "
                                + "spelling - the guard pool this course's own platform boots print, one "
                                + "WAITING worker per socket. spring.threads.virtual.enabled is on (the "
                                + "property check below holds) but the web server is still on the platform "
                                + "executor: the Flag is one line, and what it reaches is the Tomcat "
                                + "customizer in spring-boot-tomcat, installing Tomcat's "
                                + "VirtualThreadExecutor with the tomcat-handler- prefix. If the property "
                                + "is set in a profile this diagnostics run does not boot, or beset by a "
                                + "compiled-in executor of your own, the dump is the place the truth is.",
                        PLATFORM_WORKER)
                .isFalse();

        assertThat(PLATFORM_HANDLER.matcher(dump.body()).find())
                .as(
                        "The thread dump lists a platform thread named tomcat-handler-<n>: that prefix "
                                + "is Tomcat's VirtualThreadExecutor's, and its threads are virtual, so a "
                                + "platform worker wearing the name is a hand-built executor - build no "
                                + "new HTTP thread machinery of yourself. The switch is the property "
                                + "spring.threads.virtual.enabled: true and nothing else.",
                        PLATFORM_HANDLER)
                .isFalse();

        assertThat(environment.getProperty("spring.threads.virtual.enabled"))
                .as(
                        "spring.threads.virtual.enabled is not set on the application this diagnostics "
                                + "boots (the read: %s): the platform pool still serves the requests, and the M11 "
                                + "doc's first move is the property alone - spring.threads.virtual.enabled: "
                                + "true in application.yml, in the change's main application.yml where a "
                                + "reviewer's first read finds it. The property does not exist in the "
                                + "Boot 2 era, arrived with the 3.2 line... and its default is still "
                                + "false in 4.1.1, so switching it on is the change's decision, not a "
                                + "default it inherits. The switch takes no Java: no "
                                + "Executors.newVirtualThreadPerTaskExecutor(), no thread factory.",
                        environment.getProperty("spring.threads.virtual.enabled"))
                .isEqualTo("true");
    }

    /**
     * The second requirement: a wire blip is retried through. The availability
     * service drops the connection the first time a borrow asks about a Copy
     * and answers normally the next moment - and the borrow lands as 201,
     * because the replay policy the framework itself ships carried the call
     * over the fault. With nothing retried, the first reset is the borrow's
     * death.
     */
    @Test
    void aWireBlipIsRetriedThroughAndCounted() throws IOException, InterruptedException {
        String barcode = writeACopyOnTheShelfAndBlipTheWire();

        HttpResponse<String> borrow = borrow(MEMBER_NAME, MEMBER_PASSWORD, barcode);
        assertThat(borrow.statusCode())
                .as(
                        "The borrow through a wire blip - one connection reset, the availability service "
                                + "recovered the next moment - did not answer 201 (it answered %s): the wire "
                                + "failure is retried nowhere. The replay lives on the Framework's own "
                                + "annotations, two jars the build already resolves: @EnableResilientMethods "
                                + "on a configuration class, @Retryable on the availability method the "
                                + "borrow calls, with includes naming the wire failure "
                                + "(ResourceAccessException, M04's contract for could-not-connect and "
                                + "connection-dropped) and the geometry set by deliberation - maxRetries, "
                                + "delay, jitter, multiplier, maxDelay with values a sentence can justify, "
                                + "not the annotation's defaults. The call must enter the retry from "
                                + "outside its bean - the borrow holds the client (or the checker around "
                                + "it) through the container, the way the M04 design required; "
                                + "this.availability(...) from inside the same class never crosses the "
                                + "proxy, and the annotation is dead source.",
                        borrow.statusCode())
                .isEqualTo(201);
        assertThat(memberOfTheLoan(borrow))
                .as(
                        "The Loan the retried-through borrow landed was not issued for %s: the retry "
                                + "carries the call, it does not change whose borrow it is - the Member is "
                                + "still the Authentication the chain answered, the body still names only "
                                + "the Copy's barcode.",
                        memberOfTheLoan(borrow))
                .isEqualTo(MEMBER_NAME);

        HttpResponse<String> metric =
                fetchWithOperator(managementBase(), "/actuator/metrics/library.availability.retried");
        assertThat(metric.statusCode())
                .as(
                        "The Operator's read of the library.availability.retried metric answered %s: "
                                + "a retry without an observer is a deficit nobody owns. The framework "
                                + "wires the observer into what the application already has - every "
                                + "exception a retry processing encounters publishes a MethodRetryEvent "
                                + "(it extends MethodFailureEvent, which extends ApplicationEvent), and "
                                + "the M09 listening shape is all it takes: an @EventListener method "
                                + "incrementing a counter in the MeterRegistry, at least named by the "
                                + "annotated method (event.getMethod().getName()), like "
                                + "library.loans.issued was - one listener serving every resilient "
                                + "method in the application. /actuator/metrics is on M09's exposure "
                                + "list; the read above is as real as the counters.",
                        metric.statusCode())
                .isEqualTo(200);
        assertThat(retryCount(metric))
                .as(
                        "The library.availability.retried metric's count is %s after a borrow that was "
                                + "retried through: the count a listener published did not move. The "
                                + "MethodRetryEvent listener must live where it increments on the event "
                                + "the framework publishes, the way M09's counter sits where the Loan "
                                + "row is written.",
                        retryCount(metric))
                .isGreaterThanOrEqualTo(1.0);
    }

    /**
     * The third requirement: exhaustion is answered in domain words, not in
     * the container's error page and never as a hang. Every availability
     * attempt hits a connection reset until the replay budget is spent - and
     * the borrow answers a problem detail on the M08 surface, the refusal
     * naming its domain instead of echoing the container's error page.
     *
     * <p>What the budget's spending arrives as is the change's to name:
     * 7.0.9's own retry interceptor, read off the bytecode, rethrows the
     * spent call's underlying failure - the last attempt's
     * {@code ResourceAccessException} - after publishing the retry-aborted
     * {@code MethodRetryEvent}, so the shape at the boundary is the wire
     * failure's, and spent is distinguished from unspent by what the
     * listener counted or by the template that carried the budget. The
     * problem detail is what the diagnostics ask for; how the boundary knows
     * is the change's.
     */
    @Test
    void exhaustedReplayAnswersAProblemDetailNotTheErrorPage() throws IOException, InterruptedException {
        String barcode = writeACopyOnTheShelfAndCutTheWire();

        HttpResponse<String> borrow = borrow(MEMBER_NAME, MEMBER_PASSWORD, barcode);
        assertThat(borrow.headers().firstValue("Content-Type").orElse(""))
                .as(
                        "The borrow whose availability budget was exhausted answered %s: not a problem "
                                + "detail on the M08 surface. The budget spent is a refusal the Member "
                                + "reads, and the boundary is where the reading shape comes from: an "
                                + "@ExceptionHandler on the advice translating what exhaustion arrives "
                                + "as - 7.0.9's retry interceptor rethrows the spent call's last failure, "
                                + "a ResourceAccessException (read off the bytecode), and a listener of "
                                + "the retry-aborted MethodRetryEvent or a programmatic RetryTemplate is "
                                + "what distinguishes spent from unspent - the way TitleNotFound and the "
                                + "others are translated into the domain's own words.",
                        borrow.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/problem+json");

        assertThat(borrow.statusCode())
                .as(
                        "The borrow whose availability budget was exhausted answered %s: the status "
                                + "the exhaustion earns is the boundary's own line item, and it is none of "
                                + "the ambiguity of a bare 500. The upstream failed on the wire; the "
                                + "library answers 502 - Bad Gateway, the status a service owes its "
                                + "caller when the dependency it fronts failed - with a detail in "
                                + "domain words, not a stack trace and not a hang.",
                        borrow.statusCode())
                .isEqualTo(502);

        assertThat(titleOf(borrow))
                .as(
                        "The problem detail the exhaustion answers carries no title (the body read: %s): a "
                                + "problem detail without its title is a beige number - the M08 handler "
                                + "titles every refusal in the feature's words. The exhaustion's is the "
                                + "availability's own: the protocol dropped the wire while the borrow asked "
                                + "whether the Copy is on the shelf, and the title says that in the domain's "
                                + "vocabulary.",
                        titleOf(borrow))
                .isNotBlank();
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
     * The management socket, read the way M09's diagnostics read it: the
     * property the management server publishes once it rides a port of its
     * own.
     */
    private String managementBase() {
        return "http://localhost:" + environment.getProperty("local.management.port");
    }

    /**
     * A public read on the API socket, so the dump below observes a server
     * that has served: the thread list is taken of a working library, not a
     * freshly booted one.
     */
    private void servedAnApiRequest() throws IOException, InterruptedException {
        HttpResponse<String> titles = fetch(apiBase(), "/api/titles");
        if (titles.statusCode() != 200) {
            fail(
                    "The public read of /api/titles did not answer 200 (it answered %s): the M10 chain "
                            + "made the catalogue public, and the M11 lab changes nothing about who may ask. "
                            + "The read is here so the thread dump observes a server that served.",
                    titles.statusCode());
        }
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
     * external name. The request carries no Member: M10 took the Member out
     * of the body, and the M11 lab changes nothing about whose borrow it is.
     */
    private HttpResponse<String> borrow(String username, String password, String copyId) {
        HttpRequest request = request(URI.create(apiBase() + "/api/loans"))
                .header("Content-Type", "application/json")
                .header("Authorization", basic(username, password))
                .POST(HttpRequest.BodyPublishers.ofString("{\"copyId\":\"" + copyId + "\"}"))
                .build();
        try {
            // The client's own ceiling is generous: a borrow that outlives
            // it is a hang the lab forbids, and the failure names the
            // socket the call drowned on.
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
     * The problem detail's title: the word the refusal calls itself in.
     */
    private String titleOf(HttpResponse<String> problem) {
        return jsonMapper.readTree(problem.body()).path("title").asString();
    }

    /**
     * The count the retried metric carries: the metric's COUNT measurement,
     * read the way a dashboard reads it.
     */
    private Double retryCount(HttpResponse<String> metric) {
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
     * database - the Copy a borrow must be issued against - and the
     * availability service stubbed to say just that: the copy is available.
     */
    private String writeACopyOnTheShelf(String barcode) {
        String isbn = anUnusedIsbn();
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
        return barcode;
    }

    /**
     * The blipped means: the Copy on the shelf, and the availability service
     * dropping the connection the first time this Copy is asked about, then
     * answering normally - the fault the replay must carry the borrow over.
     */
    private String writeACopyOnTheShelfAndBlipTheWire() {
        String barcode = "bc-" + UUID.randomUUID();
        writeACopyOnTheShelf(barcode);
        availabilityService.stubFor(get(urlPathMatching("/api/availability/" + barcode))
                .inScenario("a blip, then the service is back")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("recovered"));
        availabilityService.stubFor(get(urlPathMatching("/api/availability/" + barcode))
                .inScenario("a blip, then the service is back")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"copyId\":\"" + barcode + "\",\"available\":true}")));
        return barcode;
    }

    /**
     * The cut-wire means: the Copy on the shelf, and the availability service
     * dropping the connection every time - every attempt of what the caller's
     * budget is worth, until it runs out.
     */
    private String writeACopyOnTheShelfAndCutTheWire() {
        String barcode = "bc-" + UUID.randomUUID();
        writeACopyOnTheShelf(barcode);
        availabilityService.stubFor(get(urlPathMatching("/api/availability/" + barcode))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        return barcode;
    }

    private String anUnusedIsbn() {
        return "isbn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
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
