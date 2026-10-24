package com.example.library.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Diagnostics for the M12 lab: the application's deployment forms, run in
 * their packaged shape rather than the editor's exploded one.
 *
 * <p>The application arrives exactly as the resilience lab left it - the
 * requests on virtual threads, the availability call bounded and retried, the
 * drain window named - packaged into the executable jar the M02 build taught,
 * and nothing beyond it: no build ever walked the application's container
 * ahead of time, so no {@code process-aot} goal is bound, no generated bean
 * definitions or hint JSON are in the archive, and the AOT switch the 4.1.1
 * jar answers to has nothing to play back. These tests state what the
 * packaging upgrade adds, and each one <em>runs</em> the form it judges:
 *
 * <ul>
 * <li>the packaged jar booted plainly and again under
 * {@code spring.aot.enabled=true} - one archive, both runs answering
 * {@code GET /api/titles}; the switched run comes up on the initializer the
 * build generated, and Boot 4.1.1 reports its absence through
 * {@code AotInitializerNotFoundFailureAnalyzer} in its own words ("Did you
 * build the application with enabled AOT processing?") - the switch itself is
 * a jar fact the course's delta table read off exactly that class;</li>
 * <li>the extracted form - {@code java -Djarmode=tools -jar ... extract}, the
 * 3.3 rename of Boot 2's {@code layertools} - booted plainly and under the
 * switch, the packaging page's recommended run ("running the application from
 * an exploded structure is faster and recommended in production");</li>
 * <li>the AOT cache trained against the extracted form with the packaging
 * page's own commands ({@code -XX:AOTCacheOutput=app.aot
 * -Dspring.context.exit=onRefresh}) and served from the combined form the
 * page names in one sentence: "AOT cache and Spring's AOT can be combined to
 * further improve startup time".</li>
 * </ul>
 *
 * <p>The native image the Module introduces is the fourth form and none of
 * these diagnostics build it: a native build belongs to whoever owns a
 * GraalVM Native Image Kit in version 25 ({@code mvn -Pnative
 * native:compile-no-fork}), and the diagnostics depend on none of that - they
 * run the jar forms on the JDK the build itself already runs.
 *
 * <p>They are failsafe's ({@code *IT}, unfbundled from the default run and
 * run after the package phase, because only a jar packaged by <em>this</em>
 * build is the artifact these tests judge; the parent POM excludes
 * {@code **&#47;diagnostics/*IT.java} from every ordinary build). Run with:
 *
 * <pre>./mvnw -f labs -pl lab-12-native-images-and-packaging-start verify -Pdiagnostics</pre>
 *
 * <p>Diagnostics are asserted in CI with the solutions-only profile: the same
 * tests, run only against the reference implementations by
 * {@code ./mvnw -f labs verify -Pdiagnostics-solutions}.
 *
 * <p>The application is driven as a whole process, not as this test JVM's
 * context: each form is booted on a port the test picks, the catalogue's
 * public read is polled until it answers or the boot dies, and the process is
 * always stopped again. Not a {@code @SpringBootTest} rig on purpose, for the
 * reason this Module is the packaging one: the form is the subject, and an
 * application that boots in the editor but not from its packaged entrance
 * shows red here, as does one whose AOT switch never came on.
 */
class DeploymentFormsDiagnosticsIT {

    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** How long a packaged boot gets to answer the catalogue's public read. */
    private static final Duration BOOT_PATIENCE = Duration.ofSeconds(90);

    /** How long the jarmode {@code tools} extraction gets to finish. */
    private static final Duration EXTRACT_PATIENCE = Duration.ofSeconds(120);

    /**
     * How long the cache's training run gets to write the file and end itself:
     * a JDK 25 AOT cache is assembled by a child process the training run
     * launches, and the runs are a minute-scale business.
     */
    private static final Duration TRAINING_PATIENCE = Duration.ofSeconds(240);

    /** How much of a dead boot's output a failure message quotes back. */
    private static final int LOG_TAIL_CHARS = 3000;

    private static Path packagedJar;

    private static Path extractedForm;

    private static Path extractedJar;

    /**
     * The two forms every running test boots: the jar the build packaged and
     * the extracted form the {@code tools} jarmode lays out. A test that
     * cannot get even this far is a build problem no diagnostic helps.
     */
    @BeforeAll
    static void stageThePackagedForms() throws IOException, InterruptedException {
        packagedJar = thePackagedJar();
        extractedForm = extractWithTheToolsJarmode(packagedJar);
        extractedJar = extractedForm.resolve(packagedJar.getFileName().toString());
    }

    /**
     * The first requirement: the packaged jar the build just produced boots
     * from its own entrance and answers the catalogue - plainly, the way the
     * M02 build left it, and again under the AOT switch the same packaging
     * page names, {@code java -Dspring.aot.enabled=true -jar}. Under the
     * switch the boot plays back the generated initializer, which the log of
     * an AOT-processed boot names: "Starting AOT-processed". Where the jar has
     * no initializer to play back, Boot reports exactly that in its own
     * analyzer's words, and the message names the one-move bind.
     */
    @Test
    void thePackagedJarServesTheApiBothPlainlyAndUnderItsAotSwitch() throws IOException {
        int port = anUnusedPort();
        try (Boot plain = aBootFor(packagedJar, List.of(), port)) {
            assertItAnswersTheApi(
                    plain,
                    "the packaged jar (java -jar, no switch)",
                    "The packaged entrance must boot by itself, on the jar's own application.yml "
                            + "with only the server port named - the M02 test moved from inspecting "
                            + "this jar to running it, so an application that boots in the editor but "
                            + "not from its packaged entrance is the red this rig exists to turn up. "
                            + "If this half ever goes red, the suspect is what the editor's exploded "
                            + "run hid: the classpath, the working directory, a jar the packaged form "
                            + "does not carry.");
        }
        try (Boot optimised = aBootFor(packagedJar, List.of("-Dspring.aot.enabled=true"), port)) {
            assertItAnswersTheApi(
                    optimised,
                    "the packaged jar run under its AOT switch (-Dspring.aot.enabled=true)",
                    "the jar carries no initializer the AOT switch can play back: the build never "
                            + "walked the application's container ahead of time, so no process-aot goal "
                            + "is bound in this module's packaging and nothing of target/spring-aot/main "
                            + "ever landed inside the archive. The move is one <execution> on the"
                            + " spring-boot-maven-plugin in this module's pom.xml - <id>process-aot</id>,"
                            + " <goals><goal>process-aot</goal></goals> - the goal the starter-parent's"
                            + " native profile binds for a native build, whose default phase"
                            + " (prepare-package) puts its output - the generated bean definitions, the"
                            + " bytecode and the hint JSON under META-INF/native-image - before the"
                            + " repackage. Package again, and the run the packaging page teaches is one"
                            + " flag: java -Dspring.aot.enabled=true -jar. Boot names a missing"
                            + " initializer in its own failure analyzer's words when the switch finds"
                            + " none (\"Did you build the application with enabled AOT processing?\"),"
                            + " so the boot log below tells which way the red came.");
            optimisedAssertsTheAotPlaybackNamed(optimised, "packaged-jar");
        }
    }

    /**
     * The second requirement: the extracted form - the jarmode the 3.3 line
     * renamed, {@code tools}, reached by the packaging page's own extract
     * command - boots by itself, and the same extracted boot answers under the
     * AOT switch. The extracted jar is the same repackage the jar produced,
     * decomposed into {@code lib/}, so the initializer rides the extraction
     * only where the build produced it in the first place; the run itself is
     * the packaging page's recommended one.
     */
    @Test
    void theExtractedFormServesTheApiBothPlainlyAndUnderItsAotSwitch() throws IOException {
        int port = anUnusedPort();
        try (Boot plain = aBootFor(extractedJar, List.of(), port)) {
            assertItAnswersTheApi(
                    plain,
                    "the extracted form (java -jar my-app/<name>.jar after the tools extract)",
                    "the extracted jar, decomposed into its lib/ directory, is the packaging page's "
                            + "recommended run: \"running the application from an exploded structure is "
                            + "faster and recommended in production\". The extraction itself already "
                            + "succeeded in the staging above, so a red here is the boot half of the"
                            + " extracted shape, and the later AOT halves of this lab will carry the"
                            + " same root.");
        }
        try (Boot optimised = aBootFor(extractedJar, List.of("-Dspring.aot.enabled=true"), port)) {
            assertItAnswersTheApi(
                    optimised,
                    "the extracted form run under its AOT switch (-Dspring.aot.enabled=true)",
                    "the extracted jar is the same repackage the jar form produced, so its initializer"
                            + " is missing for the same reason as the jar's: the build bounded no"
                            + " process-aot goal anywhere and nothing of target/spring-aot/main carries"
                            + " in the archive. Bind the process-aot goal on the spring-boot-maven-plugin"
                            + " as an <execution> in this module's pom.xml, package again, re-run the"
                            + " extract the staging above already ran (the command the packaging page"
                            + " teaches: java -Djarmode=tools -jar my-app.jar extract), and the"
                            + " extracted form boots on its initializer exactly as the jar form does.");
            optimisedAssertsTheAotPlaybackNamed(optimised, "extracted-form");
        }
    }

    /**
     * The third requirement: the AOT cache, trained against the extracted
     * form with the packaging page's own two commands, served from the
     * combined form the page names - the cache on the run, Spring's AOT on the
     * boot. The training run ends itself the moment the context has refreshed
     * ({@code spring.context.exit=onRefresh}), the cache it writes is this
     * build's, and the cached boot under the switch is the deployment matrix's
     * cheapest startup of all.
     */
    @Test
    void theAotCacheTrainedOnTheExtractedFormServesTheApiUnderItsAotSwitch() throws IOException, InterruptedException {
        Path cacheFile = theCacheTheTrainingRunWrote();

        int port = anUnusedPort();
        try (Boot cached =
                aBootFor(extractedJar, List.of("-XX:AOTCache=" + cacheFile, "-Dspring.aot.enabled=true"), port)) {
            assertItAnswersTheApi(
                    cached,
                    "the AOT cache read on the extracted form under the AOT switch "
                            + "(-XX:AOTCache=..., -Dspring.aot.enabled=true)",
                    "the cache was trained - the app.aot file the training run wrote exists - but the"
                            + " cached run under the AOT switch never came up. The packaging page's"
                            + " combination sentence is the shape to hold: \"AOT cache and Spring's AOT"
                            + " can be combined to further improve startup time\" - both halves at once:"
                            + " a jar built with its AOT generation (bind the process-aot goal on the"
                            + " spring-boot-maven-plugin as an <execution> in this module's pom.xml,"
                            + " package again), and the cache read on the run (-XX:AOTCache=app.aot next"
                            + " to -Dspring.aot.enabled=true). One mismatch cancels the whole pair: a"
                            + " cache is usable \"as long as the application is not updated and the"
                            + " same Java version is used\".");
            optimisedAssertsTheAotPlaybackNamed(cached, "aot-cache");
        }
    }

    /**
     * The training run: the packaging page's own command against the
     * extracted form, writing this build's cache file. The context ends itself
     * the moment it has refreshed, and the JDK assembles the cache in a child
     * process the run launches on its way out.
     */
    private static Path theCacheTheTrainingRunWrote() throws IOException, InterruptedException {
        Path cacheLocation = aScratchDirectoryUnderTarget("training");

        List<String> command = new ArrayList<>();
        command.add(theJdkBinary("java"));
        command.add("-XX:AOTCacheOutput=" + cacheLocation.resolve("app.aot"));
        command.add("-Dspring.context.exit=onRefresh");
        command.add("-jar");
        command.add(extractedJar.toAbsolutePath().toString());

        Process training = new ProcessBuilder(command)
                .directory(extractedForm.toFile())
                .redirectErrorStream(true)
                .redirectOutput(cacheLocation.resolve("training.log").toFile())
                .start();
        boolean finished = training.waitFor(TRAINING_PATIENCE.toSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            training.destroyForcibly();
            fail(
                    "The AOT cache's training run never ended (the context was asked to exit itself at"
                            + " refresh, and %d seconds were its ceiling): the training command is the"
                            + " packaging page's own - java -XX:AOTCacheOutput=app.aot"
                            + " -Dspring.context.exit=onRefresh -jar my-app.jar, run against the extracted"
                            + " form, because \"You have to use the cache file with the extracted form of"
                            + " the application, otherwise it has no effect\". The training log is %s.",
                    TRAINING_PATIENCE.toSeconds(),
                    cacheLocation.resolve("training.log").toAbsolutePath());
        }
        if (training.exitValue() != 0) {
            fail(
                    "The AOT cache's training run ended with exit code %s: the training command is the"
                            + " packaging page's own - java -XX:AOTCacheOutput=app.aot"
                            + " -Dspring.context.exit=onRefresh -jar my-app.jar against the extracted form"
                            + " - and a run that ends itself at refresh exits 0. The training log is %s%s",
                    training.exitValue(),
                    cacheLocation.resolve("training.log").toAbsolutePath(),
                    theLogSoFar(cacheLocation.resolve("training.log")));
        }

        Path cacheFile = cacheLocation.resolve("app.aot");
        assertThat(Files.exists(cacheFile))
                .as(
                        "The training run ended cleanly (exit 0) but wrote no %s: the cache file is the"
                                + " artifact the product of the packaging page's own pair of lines, the"
                                + " training run (java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh"
                                + " -jar my-app.jar against the extracted form) and the run flag"
                                + " (-XX:AOTCache=app.aot). The training log is %s%s",
                        cacheFile.toAbsolutePath(),
                        cacheLocation.resolve("training.log").toAbsolutePath(),
                        theLogSoFar(cacheLocation.resolve("training.log")))
                .isTrue();
        return cacheFile;
    }

    /* ------------------------------------------------------------------
     * The wiring: the packaged jar, the shapes it is laid out into, and
     * the boots, polls and stops the diagnostics themselves run.
     * ------------------------------------------------------------------ */

    /**
     * The jar this build packaged - reading it the way the M02 test reads it,
     * out of {@code target/}, because the artifact the diagnostics boot is
     * the artifact the build just wrote.
     */
    private static Path thePackagedJar() throws IOException {
        try (var entries = Files.list(Path.of("target"))) {
            return entries.filter(Files::isRegularFile)
                    .filter(entry -> entry.getFileName().toString().endsWith(".jar"))
                    .findFirst()
                    .orElseThrow(() ->
                            new IOException("The packaged jar the diagnostics boot is not in target/: the form is the"
                                    + " subject, so the module's own verify build has to produce it first"
                                    + " (./mvnw -f labs -pl lab-12-native-images-and-packaging-start verify)."));
        }
    }

    /**
     * The extracted form: the jarmode the 3.3 line renamed ({@code tools},
     * not Boot 2's {@code layertools}), driven by the packaging page's own
     * command, and its layout asserted - the extracted jar the runs below boot
     * must exist by the name the {@code extract} verb lays it out as.
     */
    private static Path extractWithTheToolsJarmode(Path jar) throws IOException, InterruptedException {
        Path destination = aScratchDirectoryUnderTarget("extraction");

        // The extract verb refuses a destination that already holds anything,
        // so the extract's own log lives beside the destination, never in it.
        Path extractLog = aScratchDirectoryUnderTarget("extraction-log").resolve("extract.log");

        List<String> command = new ArrayList<>();
        command.add(theJdkBinary("java"));
        command.add("-Djarmode=tools");
        command.add("-jar");
        command.add(jar.toAbsolutePath().toString());
        command.add("extract");
        command.add("--destination");
        command.add(destination.toAbsolutePath().toString());

        Process extraction = new ProcessBuilder(command)
                .directory(destination.toFile())
                .redirectErrorStream(true)
                .redirectOutput(extractLog.toFile())
                .start();
        if (!extraction.waitFor(EXTRACT_PATIENCE.toSeconds(), TimeUnit.SECONDS)) {
            extraction.destroyForcibly();
            fail(
                    "The tools jarmode's extract never ended within %d seconds: the extraction is the"
                            + " packaging page's own command (java -Djarmode=tools -jar my-app.jar extract),"
                            + " a rename the 3.3 line carried from Boot 2's layertools spelling. The"
                            + " extract log is %s.",
                    EXTRACT_PATIENCE.toSeconds(), extractLog.toAbsolutePath());
        }
        if (extraction.exitValue() != 0) {
            fail(
                    "The tools jarmode's extract ended with exit code %s (the extraction command is"
                            + " java -Djarmode=tools -jar my-app.jar extract; the extract log is %s):%s",
                    extraction.exitValue(), extractLog.toAbsolutePath(), theLogSoFar(extractLog));
        }

        Path extracted = destination.resolve(jar.getFileName().toString());
        if (!Files.exists(extracted)) {
            fail(
                    "The extract verb left no %s beside its lib/ in %s: the extracted layout is the"
                            + " one the runs below boot, and the extract log - %s - spells whatever"
                            + " name the verb chose instead, which the diagnostics could not guess as"
                            + " their jar.%s",
                    jar.getFileName(),
                    destination.toAbsolutePath(),
                    extractLog.toAbsolutePath(),
                    theLogSoFar(extractLog));
        }
        return destination;
    }

    /**
     * One started packaged entrance - a {@code java} command of this build's
     * own JDK, the jar or extracted jar as {@code -jar}, only the server port
     * named among the program arguments, its log a file the diagnostics
     * survive on. The boot is stopped on close.
     */
    private static Boot aBootFor(Path jar, List<String> jvmArguments, int port) throws IOException {
        Path bootLocation = aScratchDirectoryUnderTarget("boot");

        List<String> command = new ArrayList<>();
        command.add(theJdkBinary("java"));
        command.addAll(jvmArguments);
        command.add("-jar");
        command.add(jar.toAbsolutePath().toString());
        command.add("--server.port=" + port);

        Process started = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(bootLocation.resolve("boot.log").toFile())
                .start();
        return new Boot(started, port, bootLocation.resolve("boot.log"));
    }

    /**
     * One started packaged entrance: stopped on close, because a diagnosis
     * never leaves a boot behind.
     */
    private static final class Boot implements AutoCloseable {

        private final Process process;
        private final int port;
        private final Path log;

        private Boot(Process process, int port, Path log) {
            this.process = process;
            this.port = port;
            this.log = log;
        }

        private int port() {
            return port;
        }

        private boolean alive() {
            return process.isAlive();
        }

        private int exitCodeIfTheBootDied() {
            return process.isAlive() ? -1 : process.exitValue();
        }

        private String theBootLogForTheMessage() {
            return theLogSoFar(log);
        }

        /**
         * The play-back proof: the log line an AOT-processed boot prints and
         * a reflective one never does. Read before the process is asked to
         * stop.
         */
        private String theWholeLog() throws IOException {
            return Files.exists(log) ? Files.readString(log, StandardCharsets.UTF_8) : "";
        }

        @Override
        public void close() {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException theStopWasInterrupted) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    /**
     * A boot of one packaged form on one port the test picks, and the
     * catalogue's public read polled until it answers. Where the boot dies
     * first, the failure is composed of the caller's guidance - what is
     * missing and what move it asks for - and of the boot's own tail, with
     * Boot's failure analysis riding inside it when the boot died. A boot the
     * read never lands on is failed with the same composition.
     */
    private static void assertItAnswersTheApi(Boot boot, String theForm, String theGuidance) {
        URI titles = URI.create("http://localhost:%d/api/titles".formatted(boot.port()));
        long deadline = System.nanoTime() + BOOT_PATIENCE.toNanos();

        HttpResponse<String> answering = null;
        Integer lastStatus = null;
        while (System.nanoTime() < deadline && boot.alive()) {
            try {
                HttpRequest request = HttpRequest.newBuilder(titles).GET().build();
                HttpResponse<String> answer =
                        HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                lastStatus = answer.statusCode();
                if (answer.statusCode() == 200) {
                    answering = answer;
                    break;
                }
            } catch (IOException theReadDidNotLandYet) {
                // The socket before the boot listens: the poll simply retries
                // when the server is not accepting yet.
            } catch (InterruptedException thePollWasInterrupted) {
                Thread.currentThread().interrupt();
                break;
            }
            awaitThePoll();
        }

        if (answering != null) {
            return;
        }
        if (!boot.alive()) {
            fail(
                    "The packaged entrance the diagnostics ran - %s - did not answer the API and did not"
                            + " survive the whole boot: the process exited with code %s. What the Lab asks"
                            + " for and what is missing: %s The boot's own output is the evidence:%n%s",
                    theForm, boot.exitCodeIfTheBootDied(), theGuidance, boot.theBootLogForTheMessage());
        }
        fail(
                "The packaged entrance the diagnostics ran - %s - never answered the API (the last"
                        + " response code read was %s), and %d seconds was the diagnostic's ceiling."
                        + " What the Lab asks for and what is missing: %s The boot's own output is the"
                        + " evidence:%n%s",
                theForm, lastStatus, BOOT_PATIENCE.toSeconds(), theGuidance, boot.theBootLogForTheMessage());
    }

    /**
     * The play-back proof read off the boot the test just polled green: the
     * AOT-processed boot names itself in the log ("Starting AOT-processed") -
     * a reflective boot does not. A dormant switch that boots is exactly the
     * misreading this catches.
     */
    private static void optimisedAssertsTheAotPlaybackNamed(Boot boot, String theForm) throws IOException {
        assertThat(boot.theWholeLog())
                .as(
                        "The %s answered the API under the AOT switch, but its boot log names no AOT"
                                + " playback (the log line read is \"Starting AOT-processed\"): a boot under"
                                + " spring.aot.enabled=true comes up on the generated initializer, and a boot"
                                + " saying nothing of it is the reflective startup with the switch nobody"
                                + " honoured - the same bind as the run that never came up. Bind the"
                                + " process-aot goal in the packaging, package again, and the log names the"
                                + " playback.",
                        theForm)
                .contains("Starting AOT-processed");
    }

    private static void awaitThePoll() {
        try {
            Thread.sleep(Duration.ofSeconds(1).toMillis());
        } catch (InterruptedException theSleepWasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** A port no socket on this machine holds - held for a moment, then freed. */
    private static int anUnusedPort() throws IOException {
        try (ServerSocket borrowed = new ServerSocket(0)) {
            return borrowed.getLocalPort();
        }
    }

    /**
     * A scratch directory the diagnostics' own processes write into, one per
     * run, under {@code target/} where {@code clean} finds it.
     */
    private static Path aScratchDirectoryUnderTarget(String prefix) throws IOException {
        Path under = Path.of("target", "m12-deployment-forms").toAbsolutePath();
        Files.createDirectories(under);
        return Files.createTempDirectory(under, prefix + "-");
    }

    /** The {@code java} this build's own JDK ships - same floor, same release. */
    private static String theJdkBinary(String name) {
        return Path.of(System.getProperty("java.home"), "bin", name).toString();
    }

    /**
     * A process's log so far, for the failure messages that quote it: whole
     * when small, tail-trimmed when a dead boot ran long before dying.
     */
    private static String theLogSoFar(Path log) {
        if (!Files.exists(log)) {
            return " (no log file was written)";
        }
        try {
            String content = Files.readString(log, StandardCharsets.UTF_8);
            return content.length() <= LOG_TAIL_CHARS
                    ? ":\n" + content
                    : "(earlier output trimmed):\n" + content.substring(content.length() - LOG_TAIL_CHARS);
        } catch (IOException theLogWasUnreadable) {
            return " (the log file was not readable: " + log.toAbsolutePath() + ")";
        }
    }
}
