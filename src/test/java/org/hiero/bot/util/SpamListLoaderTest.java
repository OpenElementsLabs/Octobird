package org.hiero.bot.util;

import org.hiero.bot.util.SpamListLoader;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpamListLoaderTest {

    private static final String SPAM_LIST_PATH = ".github/spam-list.txt";

    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHContent content;

    @AfterEach
    void tearDown() {
        SpamListLoader.invalidateCache("owner/repo");
    }

    @Test
    void detectsSpamUser() throws IOException {
        // Given
        final String spamList = "# Comment line\nspammer1\nspammer2\n\n# Another comment\nspammer3\n";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(SPAM_LIST_PATH)).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8)));

        // When / Then
        assertTrue(SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer1", SPAM_LIST_PATH));
        assertTrue(SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer2", SPAM_LIST_PATH));
        assertTrue(SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer3", SPAM_LIST_PATH));
        assertFalse(SpamListLoader.isSpamUser(gitHub, "owner/repo", "legitimate-user", SPAM_LIST_PATH));
    }

    @Test
    void ignoresCommentsAndEmptyLines() throws IOException {
        // Given
        final String spamList = "# This is a comment\n\n  \nspammer1\n# another comment\n";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(SPAM_LIST_PATH)).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8)));

        // When / Then
        assertTrue(SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer1", SPAM_LIST_PATH));
        assertFalse(SpamListLoader.isSpamUser(gitHub, "owner/repo", "# This is a comment", SPAM_LIST_PATH));
    }

    @Test
    void treatsEmptyAsNoSpam() throws IOException {
        // Given
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(SPAM_LIST_PATH)).thenThrow(new IOException("Not found"));

        // When
        final boolean result = SpamListLoader.isSpamUser(gitHub, "owner/repo", "anyone", SPAM_LIST_PATH);

        // Then
        assertFalse(result);
    }

    @Test
    void cachesResults() throws IOException {
        // Given
        final String spamList = "spammer1\n";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(SPAM_LIST_PATH)).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8)));

        // When
        SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer1", SPAM_LIST_PATH);
        SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer1", SPAM_LIST_PATH);

        // Then
        // Should only load once due to caching
        verify(repo, times(1)).getFileContent(SPAM_LIST_PATH);
    }

    @Test
    void invalidateCacheForcesReload() throws IOException {
        // Given
        final String spamList = "spammer1\n";
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(SPAM_LIST_PATH)).thenReturn(content);
        when(content.read()).thenReturn(
                new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8))
        );

        // When
        SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer1", SPAM_LIST_PATH);
        SpamListLoader.invalidateCache("owner/repo");
        SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer1", SPAM_LIST_PATH);

        // Then
        verify(repo, times(2)).getFileContent(SPAM_LIST_PATH);
    }
}