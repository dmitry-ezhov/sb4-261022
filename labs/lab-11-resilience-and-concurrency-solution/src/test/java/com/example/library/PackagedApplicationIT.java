package com.example.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

/**
 * The packaged application must be an executable jar.
 *
 * <p>M02 is about the build, so the build's output is now part of what the
 * application is tested for. This test runs after the package phase, so the
 * jar it inspects is the one the build just produced.
 */
class PackagedApplicationIT {

    private static final String LAUNCHER = "org.springframework.boot.loader.launch.JarLauncher";

    @Test
    void thePackagedJarIsAnExecutableSpringBootJar() throws IOException {
        Path jar = packagedJar();

        try (JarFile archive = new JarFile(jar.toFile())) {
            Manifest manifest = archive.getManifest();

            assertThat(manifest.getMainAttributes().getValue("Main-Class"))
                    .as("%s must be executable", jar.getFileName())
                    .isEqualTo(LAUNCHER);
            assertThat(manifest.getMainAttributes().getValue("Start-Class"))
                    .as("%s must know the application class", jar.getFileName())
                    .isEqualTo(LibraryApplication.class.getName());
            assertThat(archive.stream().anyMatch(entry -> entry.getName().startsWith("BOOT-INF/classes/")))
                    .as("%s must carry the application classes under BOOT-INF/classes", jar.getFileName())
                    .isTrue();
            assertThat(archive.stream().anyMatch(entry -> entry.getName().startsWith("BOOT-INF/lib/")))
                    .as("%s must carry its dependencies under BOOT-INF/lib", jar.getFileName())
                    .isTrue();
        }
    }

    private Path packagedJar() throws IOException {
        Path target = Path.of("target");
        try (var files = Files.list(target)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("no packaged jar in " + target.toAbsolutePath()));
        }
    }
}
