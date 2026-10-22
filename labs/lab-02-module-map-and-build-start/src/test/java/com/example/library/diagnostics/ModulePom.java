package com.example.library.diagnostics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A text view of the module's own POM.
 *
 * <p>M02 is the one Lab whose subject is the build rather than the
 * application, so its Diagnostics read the POM instead of an HTTP response.
 *
 * <p>This is a text scan rather than an XML parse, on purpose: the JDK's XML
 * parser lives in {@code javax.xml}, and this course spends a Module telling
 * you that a {@code javax.*} import in application code is a smell. XML
 * comments are stripped before anything else, so a comment that talks about a
 * version cannot be mistaken for one.
 */
final class ModulePom {

    /** The dependencies the course's root POM already gives every child. */
    static final List<String> INHERITED = List.of("spring-boot-starter-test", "jspecify");

    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern DEPENDENCY = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL);
    private static final Pattern PLUGIN = Pattern.compile("<plugin>(.*?)</plugin>", Pattern.DOTALL);
    private static final Pattern ARTIFACT_ID =
            Pattern.compile("<artifactId>\\s*(.*?)\\s*</artifactId>", Pattern.DOTALL);
    private static final Pattern VERSION = Pattern.compile("<version>\\s*(.*?)\\s*</version>", Pattern.DOTALL);

    private final List<Entry> dependencies;
    private final List<Entry> plugins;

    private ModulePom(List<Entry> dependencies, List<Entry> plugins) {
        this.dependencies = dependencies;
        this.plugins = plugins;
    }

    /**
     * Reads the POM of the module the test is running in.
     *
     * @return the module's own POM
     */
    static ModulePom read() {
        Path pom = Path.of("pom.xml");
        try {
            return parse(Files.readString(pom));
        } catch (IOException exception) {
            throw new UncheckedIOException("cannot read " + pom.toAbsolutePath(), exception);
        }
    }

    /**
     * Parses a POM.
     *
     * @param xml the POM's text
     * @return the declared dependencies and plugins
     */
    static ModulePom parse(String xml) {
        String text = COMMENT.matcher(xml).replaceAll("");
        String withoutManagement = remove(text, "dependencyManagement");
        String withoutBuild = remove(withoutManagement, "build");
        List<Entry> dependencies = entries(section(withoutBuild, "dependencies"), DEPENDENCY);
        String build = section(withoutManagement, "build");
        List<Entry> plugins = entries(section(remove(build, "pluginManagement"), "plugins"), PLUGIN);
        return new ModulePom(dependencies, plugins);
    }

    /**
     * The dependencies the module declares itself.
     *
     * @return the declared dependencies
     */
    List<Entry> dependencies() {
        return dependencies;
    }

    /**
     * The plugins the module declares itself.
     *
     * @return the declared plugins
     */
    List<Entry> plugins() {
        return plugins;
    }

    /**
     * The dependencies that pin a literal version instead of referring to one.
     *
     * @return the dependencies with a pinned version
     */
    List<Entry> dependenciesWithPinnedVersions() {
        return dependencies.stream().filter(Entry::pinsAVersion).toList();
    }

    /**
     * The plugins that pin a literal version instead of referring to one.
     *
     * @return the plugins with a pinned version
     */
    List<Entry> pluginsWithPinnedVersions() {
        return plugins.stream().filter(Entry::pinsAVersion).toList();
    }

    /**
     * The declared dependencies that the parent already provides.
     *
     * @return the duplicated dependencies
     */
    List<Entry> inheritedDependenciesDeclaredAgain() {
        return dependencies.stream()
                .filter(entry -> INHERITED.contains(entry.artifactId()))
                .toList();
    }

    /**
     * Whether the module declares the given artifact itself.
     *
     * @param artifactId the artifact to look for
     * @return whether the module declares it
     */
    boolean declares(String artifactId) {
        return dependencies.stream().anyMatch(entry -> entry.artifactId().equals(artifactId));
    }

    private static List<Entry> entries(String block, Pattern pattern) {
        List<Entry> entries = new ArrayList<>();
        Matcher matcher = pattern.matcher(block);
        while (matcher.find()) {
            String body = matcher.group(1);
            String artifactId = firstMatch(ARTIFACT_ID, body);
            if (artifactId != null) {
                entries.add(new Entry(artifactId, firstMatch(VERSION, body)));
            }
        }
        return entries;
    }

    private static String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String section(String text, String element) {
        int start = text.indexOf("<" + element + ">");
        int end = start < 0 ? -1 : text.indexOf("</" + element + ">", start);
        return end < 0 ? "" : text.substring(start, end);
    }

    private static String remove(String text, String element) {
        int start = text.indexOf("<" + element + ">");
        int end = start < 0 ? -1 : text.indexOf("</" + element + ">", start);
        if (end < 0) {
            return text;
        }
        return text.substring(0, start) + text.substring(end + element.length() + 3);
    }

    /**
     * One declared dependency or plugin.
     *
     * @param artifactId the artifact's name
     * @param version the version it pins, or {@code null} when it pins none
     */
    record Entry(String artifactId, String version) {

        /**
         * Whether the entry names a literal version rather than a property.
         *
         * @return whether a version is pinned
         */
        boolean pinsAVersion() {
            return version != null && !version.contains("${");
        }
    }
}
