package com.openelements.octobird.scheduled;

import com.openelements.octobird.auth.GitHubAppAuth;
import com.openelements.octobird.scheduled.InstallationLoader.RepoInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallationLoaderTest {

    @Mock
    private GitHubAppAuth auth;

    private RepoRegistry repoRegistry;
    private InstallationLoader loader;

    @BeforeEach
    void setUp() {
        repoRegistry = new RepoRegistry();
        loader = new InstallationLoader(auth, repoRegistry);
    }

    @Test
    void loadsReposFromMultipleInstallations() throws IOException {
        // Given
        when(auth.listInstallationIds()).thenReturn(List.of(100L, 200L));
        when(auth.listInstallationRepos(100L)).thenReturn(List.of(
                new RepoInfo(1L, "org-a/repo-1")));
        when(auth.listInstallationRepos(200L)).thenReturn(List.of(
                new RepoInfo(2L, "org-b/repo-2")));

        // When
        loader.loadAll();

        // Then
        final Map<Long, RepoRegistry.RegistrationEntry> all = repoRegistry.getAll();
        assertEquals(2, all.size());
        assertEquals("org-a/repo-1", all.get(1L).repoFullName());
        assertEquals(100L, all.get(1L).installationId());
        assertEquals("org-b/repo-2", all.get(2L).repoFullName());
        assertEquals(200L, all.get(2L).installationId());
    }

    @Test
    void loadsMultipleReposPerInstallation() throws IOException {
        // Given
        when(auth.listInstallationIds()).thenReturn(List.of(100L));
        when(auth.listInstallationRepos(100L)).thenReturn(List.of(
                new RepoInfo(1L, "org/repo-a"),
                new RepoInfo(2L, "org/repo-b"),
                new RepoInfo(3L, "org/repo-c")));

        // When
        loader.loadAll();

        // Then
        assertEquals(3, repoRegistry.getAll().size());
    }

    @Test
    void emptyInstallationListResultsInEmptyRegistry() throws IOException {
        // Given
        when(auth.listInstallationIds()).thenReturn(List.of());

        // When
        loader.loadAll();

        // Then
        assertTrue(repoRegistry.getAll().isEmpty());
    }

    @Test
    void continuesWhenOneInstallationFails() throws IOException {
        // Given
        when(auth.listInstallationIds()).thenReturn(List.of(100L, 200L));
        when(auth.listInstallationRepos(100L)).thenReturn(List.of(
                new RepoInfo(1L, "org-a/repo-1")));
        when(auth.listInstallationRepos(200L)).thenThrow(new IOException("API error"));

        // When
        loader.loadAll();

        // Then
        assertEquals(1, repoRegistry.getAll().size());
        assertEquals("org-a/repo-1", repoRegistry.getAll().get(1L).repoFullName());
    }

    @Test
    void throwsWhenGitHubApiCompletelyUnreachable() throws IOException {
        // Given
        when(auth.listInstallationIds()).thenThrow(new IOException("Connection refused"));

        // When / Then
        assertThrows(IOException.class, () -> loader.loadAll());
        assertTrue(repoRegistry.getAll().isEmpty());
    }
}
