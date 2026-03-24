package com.openelements.octobird.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads a nearby {@code .env} file for local development convenience.
 */
final class LocalEnvFile {

    private LocalEnvFile() {
    }

    static Map<String, String> load() {
        for (Path searchRoot : searchRoots()) {
            final Map<String, String> envValues = findNearest(searchRoot);
            if (!envValues.isEmpty()) {
                return envValues;
            }
        }
        return Map.of();
    }

    static Map<String, String> findNearest(final Path startDirectory) {
        Path current = startDirectory.toAbsolutePath().normalize();
        while (current != null) {
            final Path envFile = current.resolve(".env");
            if (Files.isRegularFile(envFile)) {
                return parse(envFile);
            }
            current = current.getParent();
        }
        return Map.of();
    }

    static Map<String, String> parse(final Path envFile) {
        final Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(envFile)) {
                final String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                final String assignment = trimmed.startsWith("export ")
                        ? trimmed.substring("export ".length()).trim()
                        : trimmed;
                final int separatorIndex = assignment.indexOf('=');
                if (separatorIndex < 0) {
                    continue;
                }

                final String key = assignment.substring(0, separatorIndex).trim();
                final String rawValue = assignment.substring(separatorIndex + 1).trim();
                if (key.isEmpty()) {
                    continue;
                }
                values.put(key, stripQuotes(rawValue));
            }
            return Map.copyOf(values);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to read " + envFile, e);
        }
    }

    private static Set<Path> searchRoots() {
        final Set<Path> searchRoots = new LinkedHashSet<>();
        searchRoots.add(Paths.get(""));

        final CodeSource codeSource = LocalEnvFile.class.getProtectionDomain().getCodeSource();
        if (codeSource != null && codeSource.getLocation() != null) {
            try {
                Path location = Path.of(codeSource.getLocation().toURI());
                if (Files.isRegularFile(location)) {
                    location = location.getParent();
                }
                if (location != null) {
                    searchRoots.add(location);
                }
            } catch (final URISyntaxException e) {
                throw new IllegalStateException("Failed to resolve application location", e);
            }
        }
        return searchRoots;
    }

    private static String stripQuotes(final String value) {
        if (value.length() >= 2) {
            final char first = value.charAt(0);
            final char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
