package org.hiero.bot.config;

import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpamListLoaderTest {

    private SpamListLoader loader;

    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHContent content;

    @BeforeEach
    void setUp() {
        loader = new SpamListLoader();
    }

    @Test
    void detectsSpamUser() throws IOException {
        String spamList = "# Comment line\nspammer1\nspammer2\n\n# Another comment\nspammer3\n";

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(".github/spam-list.txt")).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8)));

        assertTrue(loader.isSpamUser(gitHub, "owner/repo", "spammer1"));
        assertTrue(loader.isSpamUser(gitHub, "owner/repo", "spammer2"));
        assertTrue(loader.isSpamUser(gitHub, "owner/repo", "spammer3"));
        assertFalse(loader.isSpamUser(gitHub, "owner/repo", "legitimate-user"));
    }

    @Test
    void ignoresCommentsAndEmptyLines() throws IOException {
        String spamList = "# This is a comment\n\n  \nspammer1\n# another comment\n";

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(".github/spam-list.txt")).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8)));

        assertTrue(loader.isSpamUser(gitHub, "owner/repo", "spammer1"));
        assertFalse(loader.isSpamUser(gitHub, "owner/repo", "# This is a comment"));
    }

    @Test
    void treatsEmptyAsNoSpam() throws IOException {
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(".github/spam-list.txt")).thenThrow(new IOException("Not found"));

        assertFalse(loader.isSpamUser(gitHub, "owner/repo", "anyone"));
    }

    @Test
    void cachesResults() throws IOException {
        String spamList = "spammer1\n";

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(".github/spam-list.txt")).thenReturn(content);
        when(content.read()).thenReturn(new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8)));

        loader.isSpamUser(gitHub, "owner/repo", "spammer1");
        loader.isSpamUser(gitHub, "owner/repo", "spammer1");

        // Should only load once due to caching
        verify(repo, times(1)).getFileContent(".github/spam-list.txt");
    }

    @Test
    void invalidateCacheForcesReload() throws IOException {
        String spamList = "spammer1\n";

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getFileContent(".github/spam-list.txt")).thenReturn(content);
        when(content.read()).thenReturn(
                new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayInputStream(spamList.getBytes(StandardCharsets.UTF_8))
        );

        loader.isSpamUser(gitHub, "owner/repo", "spammer1");
        loader.invalidateCache("owner/repo");
        loader.isSpamUser(gitHub, "owner/repo", "spammer1");

        verify(repo, times(2)).getFileContent(".github/spam-list.txt");
    }
}
