package com.openelements.octobird.scheduled;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepoRegistryTest {

    private RepoRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RepoRegistry();
    }

    @Test
    void registerAddsEntry() {
        // Given
        // empty registry

        // When
        registry.register(1L, "owner/repo", 100L);

        // Then
        final Map<Long, RepoRegistry.RegistrationEntry> all = registry.getAll();
        assertEquals(1, all.size());
        assertEquals("owner/repo", all.get(1L).repoFullName());
        assertEquals(100L, all.get(1L).installationId());
    }

    @Test
    void registerIsIdempotent() {
        // Given
        registry.register(1L, "owner/repo", 100L);

        // When — register same repo again
        registry.register(1L, "owner/repo", 100L);

        // Then — still one entry, no error
        assertEquals(1, registry.getAll().size());
    }

    @Test
    void registerUpdatesExistingEntry() {
        // Given
        registry.register(1L, "owner/old-name", 100L);

        // When — register same repoId with new name (repo renamed)
        registry.register(1L, "owner/new-name", 100L);

        // Then — entry is updated
        assertEquals("owner/new-name", registry.getAll().get(1L).repoFullName());
    }

    @Test
    void registerAddsNewEntriesAlongsideExisting() {
        // Given
        registry.register(1L, "owner/repo-a", 100L);

        // When
        registry.register(2L, "owner/repo-b", 100L);

        // Then
        assertEquals(2, registry.getAll().size());
        assertNotNull(registry.getAll().get(1L));
        assertNotNull(registry.getAll().get(2L));
    }

    @Test
    void getAllReturnsImmutableSnapshot() {
        // Given
        registry.register(1L, "owner/repo", 100L);
        final Map<Long, RepoRegistry.RegistrationEntry> snapshot = registry.getAll();

        // When — add another repo after snapshot
        registry.register(2L, "owner/repo-2", 100L);

        // Then — snapshot is unaffected
        assertEquals(1, snapshot.size());
        assertEquals(2, registry.getAll().size());
    }

    @Test
    void emptyRegistryReturnsEmptyMap() {
        // Given
        // empty registry

        // When
        final Map<Long, RepoRegistry.RegistrationEntry> all = registry.getAll();

        // Then
        assertTrue(all.isEmpty());
    }

    @Test
    void registerRejectsNullRepoFullName() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
                registry.register(1L, null, 100L));
    }
}
