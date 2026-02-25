package org.hiero.bot.config;

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

    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHContent content;

    @AfterEach
    void tearDown() {
        MentorRosterLoader.invalidateCache("owner/repo");
    }

    @Test
    void loadsRosterFromFile() throws IOException {
        final String json = "{\"order\": [\"mentor1\", \"mentor2\", \"mentor3\"]}";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(ROSTER_PATH)).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        final List<String> roster = MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);

        assertEquals(List.of("mentor1", "mentor2", "mentor3"), roster);
    }

    @Test
    void returnsEmptyListWhenFileNotFound() throws IOException {
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(ROSTER_PATH)).thenThrow(new IOException("Not found"));

        final List<String> roster = MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);

        assertTrue(roster.isEmpty());
    }

    @Test
    void selectMentorReturnsNullForEmptyRoster() {
        assertNull(MentorRosterLoader.selectMentor(List.of()));
    }

    @Test
    void selectMentorReturnsMentorFromRoster() {
        final List<String> roster = List.of("mentor1", "mentor2", "mentor3");
        final String selected = MentorRosterLoader.selectMentor(roster);
        assertTrue(roster.contains(selected));
    }

    @Test
    void cachesRosterPerRepo() throws IOException {
        final String json = "{\"order\": [\"mentor1\"]}";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(ROSTER_PATH)).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);
        MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);

        verify(repo, times(1)).getFileContent(ROSTER_PATH);
    }

    @Test
    void invalidateCacheForcesReload() throws IOException {
        final String json = "{\"order\": [\"mentor1\"]}";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(ROSTER_PATH)).thenReturn(content);
        when(content.read()).thenReturn(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);
        MentorRosterLoader.invalidateCache("owner/repo");
        MentorRosterLoader.loadRoster(gitHub, "owner/repo", ROSTER_PATH);

        verify(repo, times(2)).getFileContent(ROSTER_PATH);
    }
}
