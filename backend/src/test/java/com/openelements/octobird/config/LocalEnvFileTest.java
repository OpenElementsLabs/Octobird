package com.openelements.octobird.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalEnvFileTest {

    @Test
    void findsNearestEnvFileInAncestorDirectory(@TempDir final Path tempDir) throws IOException {
        final Path repoRoot = Files.createDirectory(tempDir.resolve("repo"));
        final Path backendDir = Files.createDirectories(repoRoot.resolve("backend").resolve("target"));
        Files.writeString(repoRoot.resolve(".env"), """
                BOT_APP_ID=3006578
                GITHUB_CLIENT_ID="client-id"
                """);

        final Map<String, String> values = LocalEnvFile.findNearest(backendDir);

        assertEquals("3006578", values.get("BOT_APP_ID"));
        assertEquals("client-id", values.get("GITHUB_CLIENT_ID"));
    }
}
