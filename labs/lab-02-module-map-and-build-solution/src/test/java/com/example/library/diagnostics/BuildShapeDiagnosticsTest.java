package com.example.library.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Diagnostics for the M02 lab.
 *
 * <p>The Lab is about the build, so these tests read the module's own POM
 * rather than an HTTP response. They are excluded from the default build (the
 * parent POM excludes {@code **&#47;diagnostics/*Test.java}) so that the start
 * module stays green while the defects are still present. Run them with:
 *
 * <pre>mvn -f labs verify -Pdiagnostics</pre>
 */
class BuildShapeDiagnosticsTest {

    private ModulePom pom;

    @BeforeEach
    void readTheModulePom() {
        pom = ModulePom.read();
    }

    @Test
    void noDependencyPinsAVersion() {
        assertThat(pom.dependenciesWithPinnedVersions())
                .as("no dependency may pin a version - the parent's dependencyManagement owns the versions")
                .isEmpty();
    }

    @Test
    void noPluginPinsAVersion() {
        assertThat(pom.pluginsWithPinnedVersions())
                .as("no plugin may pin a version - the parent's pluginManagement owns the plugin versions")
                .isEmpty();
    }

    @Test
    void inheritedDependenciesAreNotDeclaredAgain() {
        assertThat(pom.inheritedDependenciesDeclaredAgain())
                .as("the parent already gives every child these dependencies")
                .isEmpty();
    }

    @Test
    void theModuleDependsOnTheBoot4WebStarter() {
        assertThat(pom.declares("spring-boot-starter-webmvc"))
                .as("the web starter in Boot 4 is spring-boot-starter-webmvc")
                .isTrue();
    }

    @Test
    void theModuleDoesNotDependOnTheDeprecatedWebStarter() {
        assertThat(pom.declares("spring-boot-starter-web"))
                .as("spring-boot-starter-web is deprecated in Boot 4; it still resolves, which is the trap")
                .isFalse();
    }
}
