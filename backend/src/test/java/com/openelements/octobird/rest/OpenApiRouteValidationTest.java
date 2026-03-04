package com.openelements.octobird.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiRouteValidationTest {

    /**
     * The API paths that are registered in {@code Main.setupRouting()}.
     * Static content paths ({@code /swagger-ui}, {@code /webjars}) are excluded
     * because they are served by {@code StaticContentService} and are not REST API routes.
     */
    private static final Set<String> REGISTERED_API_PATHS = Set.of(
            "/webhook",
            "/health",
            "/api/repos",
            "/api/repos/{owner}/{repo}/config",
            "/api/repos/{owner}/{repo}/spam-users",
            "/api/repos/{owner}/{repo}/mentors",
            "/api/repos/{owner}/{repo}/audit-log",
            "/swagger-ui"
    );

    @Test
    void allRegisteredApiPathsAreDocumentedInOpenApiSpec() throws Exception {
        // Given
        final YAMLMapper yamlMapper = new YAMLMapper();
        final InputStream specStream = getClass().getClassLoader().getResourceAsStream("openapi.yaml");
        assertFalse(specStream == null, "openapi.yaml must be on the classpath");
        final JsonNode root = yamlMapper.readTree(specStream);
        final JsonNode pathsNode = root.get("paths");
        assertFalse(pathsNode == null, "openapi.yaml must contain a 'paths' section");

        final Set<String> documentedPaths = new HashSet<>();
        final Iterator<String> fieldNames = pathsNode.fieldNames();
        while (fieldNames.hasNext()) {
            documentedPaths.add(fieldNames.next());
        }

        // When
        final Set<String> undocumentedPaths = new HashSet<>();
        for (final String registeredPath : REGISTERED_API_PATHS) {
            if (!documentedPaths.contains(registeredPath)) {
                undocumentedPaths.add(registeredPath);
            }
        }

        // Then
        assertTrue(undocumentedPaths.isEmpty(),
                "The following registered API paths are not documented in openapi.yaml: " + undocumentedPaths);
    }

    @Test
    void allDocumentedPathsAreRegisteredInApplication() throws Exception {
        // Given
        final YAMLMapper yamlMapper = new YAMLMapper();
        final InputStream specStream = getClass().getClassLoader().getResourceAsStream("openapi.yaml");
        assertFalse(specStream == null, "openapi.yaml must be on the classpath");
        final JsonNode root = yamlMapper.readTree(specStream);
        final JsonNode pathsNode = root.get("paths");
        assertFalse(pathsNode == null, "openapi.yaml must contain a 'paths' section");

        final Set<String> documentedPaths = new HashSet<>();
        final Iterator<String> fieldNames = pathsNode.fieldNames();
        while (fieldNames.hasNext()) {
            documentedPaths.add(fieldNames.next());
        }

        // When
        final Set<String> orphanedPaths = new HashSet<>();
        for (final String documentedPath : documentedPaths) {
            if (!REGISTERED_API_PATHS.contains(documentedPath)) {
                orphanedPaths.add(documentedPath);
            }
        }

        // Then
        assertTrue(orphanedPaths.isEmpty(),
                "The following paths in openapi.yaml are not registered in the application: " + orphanedPaths);
    }
}
