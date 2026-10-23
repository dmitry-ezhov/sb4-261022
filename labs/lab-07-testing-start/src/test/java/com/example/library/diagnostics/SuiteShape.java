package com.example.library.diagnostics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The shape of this module's test suite, read where it is stored.
 *
 * <p>The M07 Lab's work is the suite itself: which context each test class
 * earns, and which annotations put it there. A suite's shape exists in
 * exactly one place the build trusts - the compiled test classes under
 * {@code target/test-classes}, the classes the default run executes. This
 * class reflects over them and never initialises any of them, so the
 * diagnostics can assert the shape while leaving each class's assertions -
 * the part the M07 checklist judges - the class's own business. A compiled
 * class's annotations are the same document the framework reads when it
 * chooses a context, so asserting this shape is asserting what the build and
 * the runtime both act on, not how any test's code is arranged.
 *
 * <p>The classes in the {@code diagnostics} package are themselves outside
 * the shape: the diagnostics must never find themselves.
 */
final class SuiteShape {

    private static final List<String> FEATURE_CONTROLLERS = List.of(
            "com.example.library.book.TitleController",
            "com.example.library.loan.LoanController",
            "com.example.library.reservation.ReservationController");

    private final List<Class<?>> suite;

    private SuiteShape(List<Class<?>> suite) {
        this.suite = List.copyOf(suite);
    }

    static SuiteShape read() {
        Path testClasses = Path.of("target", "test-classes");
        if (!Files.isDirectory(testClasses)) {
            throw new IllegalStateException("""
                    No compiled test classes under target/test-classes: the diagnostics \
                    read the shape of the suite off the compiled test classes, so run \
                    them through the build: ./mvnw -f labs -pl <module> verify -Pdiagnostics.""");
        }

        String diagnosticsPackage = SuiteShape.class.getPackageName() + ".";
        try (Stream<Path> classFiles = Files.walk(testClasses)) {
            List<Class<?>> suite = new ArrayList<>();
            for (String name : classNamesOf(classFiles, testClasses, diagnosticsPackage)) {
                Class<?> testClass = load(name);
                if (testClass != null && carriesATest(testClass)) {
                    suite.add(testClass);
                }
            }
            return new SuiteShape(suite);
        } catch (IOException reading) {
            throw new UncheckedIOException("target/test-classes could not be read", reading);
        }
    }

    /**
     * The web tier's slice tests: test classes that assert this tier under
     * {@code @WebMvcTest} while naming the feature controller they own - the
     * slice's subject, and the annotation of the tier the app boots for it.
     */
    List<Class<?>> slicesNamingAFeatureController() {
        return suite.stream()
                .filter(testClass -> testClass.isAnnotationPresent(WebMvcTest.class))
                .filter(testClass -> namesAFeatureController(testClass.getAnnotation(WebMvcTest.class)))
                .toList();
    }

    /**
     * Whether a slice substitutes a collaborator the way the framework means:
     * a field under Framework's {@code @MockitoBean} - or
     * {@code @MockitoSpyBean}, where the real bean, wrapped, is the fixture -
     * rather than an imported configuration that reached in to hide the real
     * bean.
     */
    boolean substitutesACollaboratorWithAMockitoAnnotation(Class<?> slice) {
        return Arrays.stream(slice.getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(MockitoBean.class)
                        || field.isAnnotationPresent(MockitoSpyBean.class));
    }

    /**
     * Every test class that boots the whole application for the web tier's
     * answers alone: {@code @SpringBootTest} with {@code @AutoConfigureMockMvc}
     * and no container of its own. One such home is the wiring's; more than
     * one is the suite carrying the full context for assertions a slice can
     * prove.
     */
    List<Class<?>> fullContextMockMvcHomes() {
        return suite.stream()
                .filter(testClass -> testClass.isAnnotationPresent(SpringBootTest.class))
                .filter(testClass -> testClass.isAnnotationPresent(AutoConfigureMockMvc.class))
                .filter(testClass -> !usesATestcontainersContainer(testClass))
                .toList();
    }

    /**
     * The repository's container tests, in the suite: test classes booting the
     * application on PostgreSQL through {@code @ServiceConnection}, guarded by
     * {@code @EnabledIfDockerAvailable} so a machine without a daemon is
     * skipped, never failed.
     */
    List<Class<?>> postgreSqlContainerTests() {
        return suite.stream().filter(this::bootsAServicedPostgresContainer).toList();
    }

    private boolean bootsAServicedPostgresContainer(Class<?> testClass) {
        if (!testClass.isAnnotationPresent(SpringBootTest.class)) {
            return false;
        }
        if (!testClass.isAnnotationPresent(EnabledIfDockerAvailable.class)) {
            return false;
        }
        return Arrays.stream(testClass.getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(Container.class)
                        && field.isAnnotationPresent(ServiceConnection.class)
                        && PostgreSQLContainer.class.isAssignableFrom(field.getType()));
    }

    private static boolean usesATestcontainersContainer(Class<?> testClass) {
        if (testClass.isAnnotationPresent(Testcontainers.class)) {
            return true;
        }
        return Arrays.stream(testClass.getDeclaredFields())
                .anyMatch(field -> field.isAnnotationPresent(Container.class));
    }

    private static boolean namesAFeatureController(WebMvcTest slice) {
        List<String> targets = new ArrayList<>();
        for (Class<?> named : slice.value()) {
            targets.add(named.getName());
        }
        for (Class<?> named : slice.controllers()) {
            targets.add(named.getName());
        }
        return targets.stream().anyMatch(FEATURE_CONTROLLERS::contains);
    }

    private static List<String> classNamesOf(Stream<Path> classFiles, Path testClasses, String diagnosticsPackage) {
        return classFiles
                .filter(file -> file.getFileName().toString().endsWith(".class"))
                .filter(file -> !file.getFileName().toString().contains("$"))
                .map(testClasses::relativize)
                .map(Path::toString)
                .map(SuiteShape::cutClassSuffix)
                .map(SuiteShape::packageFromPath)
                .filter(name -> !name.startsWith(diagnosticsPackage))
                .toList();
    }

    private static String cutClassSuffix(String name) {
        return name.substring(0, name.length() - ".class".length());
    }

    private static String packageFromPath(String path) {
        return path.replace('\\', '.').replace('/', '.');
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, SuiteShape.class.getClassLoader());
        } catch (ClassNotFoundException notOnTheClasspath) {
            return null;
        }
    }

    private static boolean carriesATest(Class<?> testClass) {
        return Arrays.stream(testClass.getDeclaredMethods()).anyMatch(method -> method.isAnnotationPresent(Test.class));
    }
}
