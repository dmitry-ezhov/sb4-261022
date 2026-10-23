package com.example.library.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Diagnostics for the M07 lab: the testing strategy.
 *
 * <p>The application the previous module left is right - the borrow request's
 * 400, the Loan limit's 409, the words the boundary refuses in. What this lab
 * asks for is the shape of the suite that proves it, three layers at a time:
 * a slice test for the web layer, one full-context home for the wiring, and a
 * container test for the repository against the engine the application ships.
 * The suite as it stands proves everything through {@code @SpringBootTest}
 * full contexts, so these diagnostics read that shape rather than boot it:
 * they reflect over the compiled test classes the build runs (under
 * {@code target/test-classes}, outside the {@code diagnostics} package) for
 * the annotations that put a class in its context. They open no context of
 * their own, and what a test class asserts inside itself stays the class's
 * own business - the M07 checklist judges that; these tests judge which
 * context it earns.
 *
 * <p>They are excluded from the default build (the parent POM excludes
 * {@code **&#47;diagnostics/*Test.java}) so that the start module stays green
 * while the suite is still in its one-context shape. Run them with:
 *
 * <pre>./mvnw -f labs -pl lab-07-testing-start verify -Pdiagnostics</pre>
 *
 * <p>Diagnostics are asserted in CI with the solutions-only profile: the same
 * tests, run only against the reference implementations by
 * {@code ./mvnw -f labs verify -Pdiagnostics-solutions}.
 */
class TestingStrategyDiagnosticsTest {

    /**
     * The first requirement: the web layer's answers - the route, the
     * validation, the status, the media type, the problem detail's words -
     * are asserted at a {@code @WebMvcTest} slice that names the feature
     * controller it owns, with the controller's collaborators substituted
     * through Framework's {@code @MockitoBean}.
     */
    @Test
    void theWebLayerIsAssertedAtASlice() {
        SuiteShape shape = SuiteShape.read();
        List<Class<?>> slices = shape.slicesNamingAFeatureController();

        List<Class<?>> slicesWithSubstitutes = slices.stream()
                .filter(shape::substitutesACollaboratorWithAMockitoAnnotation)
                .toList();

        assertThat(slicesWithSubstitutes)
                .as("No test class in the suite asserts the web layer at a slice: every class "
                        + "boots @SpringBootTest, so a status code a slice could prove brings "
                        + "up the whole application every time - the domain, the persistence, "
                        + "the Flyway migration, the availability client, all of it, for an "
                        + "answer the web tier alone makes. Move the boundary's tests under "
                        + "@WebMvcTest from spring-boot-starter-webmvc-test, naming the feature "
                        + "controller they own (for example @WebMvcTest(LoanController.class) - "
                        + "the annotation lives in its artifact's own package now, "
                        + "org.springframework.boot.webmvc.test.autoconfigure), and substitute "
                        + "the collaborators the controller's constructor asks for with "
                        + "@MockitoBean - Framework's annotation at "
                        + "org.springframework.test.context.bean.override.mockito, which "
                        + "replaced the removed @MockBean. The slice boots the controller, its "
                        + "validation and the Jackson 3 serialization alone; the slices' "
                        + "assertions stay the boundary's: status, media type, payload shape, "
                        + "the problem detail's words.")
                .isNotEmpty();
    }

    /**
     * The second requirement: the seam level - the configuration bound in M03,
     * the availability client, the persistence wiring - has one full-context
     * home whose subject is the wiring, and the suite does not repeat the full
     * context for the classes whose assertions the slice answers.
     */
    @Test
    void theWiringHasOneFullContextHome() {
        SuiteShape shape = SuiteShape.read();
        List<Class<?>> homes = shape.fullContextMockMvcHomes();

        assertThat(homes)
                .as("The full context is the suite's default, not its exception: more than one "
                        + "class boots @SpringBootTest and asserts through MockMvc, so the "
                        + "boundary's answers pay the whole application's boot again every "
                        + "time. The wiring has one home: a @SpringBootTest class whose "
                        + "subject is the seam - the Loan limit bound in M03 read by the real "
                        + "LoanService, the availability client consulted through its real "
                        + "HTTP Service client, the persistence reached through the real "
                        + "repositories - asserted once with the real beans and no "
                        + "substitutes field, because a wiring test that stubs every bean it "
                        + "claims to wire is a slice wearing the full context's annotations. "
                        + "Move every other web-tier class's assertions to the slice the "
                        + "first diagnostic asks for, and let the full context be the home "
                        + "that the wiring earns.")
                .hasSize(1);
    }

    /**
     * The third requirement: the repository is tested against the engine the
     * application ships on - PostgreSQL through Testcontainers 2 and
     * {@code @ServiceConnection}, in the suite rather than behind the
     * diagnostics profile, and skipped rather than failed where no Docker
     * daemon runs.
     */
    @Test
    void theRepositoryIsTestedAgainstTheEngineItShips() {
        SuiteShape shape = SuiteShape.read();
        List<Class<?>> containers = shape.postgreSqlContainerTests();

        assertThat(containers)
                .as("No test class in the suite runs the repository against PostgreSQL: the "
                        + "engine's assertions - what the schema holds on the second engine, "
                        + "what H2 would forgive - live behind the diagnostics profile only, "
                        + "so the suite itself asserts nothing about the engine the "
                        + "application can actually ship on. Give the repository a container "
                        + "test in the suite: a static @Container field of "
                        + "PostgreSQLContainer - Testcontainers 2 moved the type to "
                        + "org.testcontainers.postgresql, at the renamed "
                        + "testcontainers-postgresql artifact - with @ServiceConnection "
                        + "letting Boot 4 read the container's address into the datasource "
                        + "with no configuration line, and @EnabledIfDockerAvailable "
                        + "guarding the class so a machine without a Docker daemon is "
                        + "skipped, never failed - a skip is a skip, never a pass.")
                .isNotEmpty();
    }
}
