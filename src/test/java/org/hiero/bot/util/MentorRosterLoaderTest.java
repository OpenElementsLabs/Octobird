package org.hiero.bot.util;

import org.hiero.bot.util.MentorRosterLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorRosterLoaderTest {

    private static final String ROSTER_PATH = ".github/mentor_roster.json";

    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHContent content;

    @AfterEach
    void tearDown() {
        MentorRosterLoader.invalidateCache("owner/repo");
    }

    @Test
    void loadsRosterFromFile() throws IOException {
        // Given
        final String json = "{\"order\": [\"mentor1\", \"mentor2\", \"mentor3\"]}";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(ROSTER_PATH)).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        // When
        final List<String> roster = MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);

        // Then
        assertEquals(List.of("mentor1", "mentor2", "mentor3"), roster);
    }

    @Test
    void returnsEmptyListWhenFileNotFound() throws IOException {
        // Given
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(ROSTER_PATH)).thenThrow(new IOException("Not found"));

        // When
        final List<String> roster = MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);

        // Then
        assertTrue(roster.isEmpty());
    }

    @Test
    void selectMentorReturnsNullForEmptyRoster() {
        // Given
        final List<String> emptyRoster = List.of();

        // When
        final String selected = MentorRosterLoader.selectMentor(emptyRoster);

        // Then
        assertNull(selected);
    }

    @Test
    void selectMentorReturnsMentorFromRoster() {
        // Given
        final List<String> roster = List.of("mentor1", "mentor2", "mentor3");

        // When
        final String selected = MentorRosterLoader.selectMentor(roster);

        // Then
        assertTrue(roster.contains(selected));
    }

    @Test
    void cachesRosterPerRepo() throws IOException {
        // Given
        final String json = "{\"order\": [\"mentor1\"]}";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(ROSTER_PATH)).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        // When
        MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);
        MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);

        // Then
        verify(repo, times(1)).getFileContent(ROSTER_PATH);
    }

    @Test
    void invalidateCacheForcesReload() throws IOException {
        // Given
        final String json = "{\"order\": [\"mentor1\"]}";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(ROSTER_PATH)).thenReturn(content);
        when(content.read()).thenReturn(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        // When
        MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);
        MentorRosterLoader.invalidateCache("owner/repo");
        MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);

        // Then
        verify(repo, times(2)).getFileContent(ROSTER_PATH);
    }
}