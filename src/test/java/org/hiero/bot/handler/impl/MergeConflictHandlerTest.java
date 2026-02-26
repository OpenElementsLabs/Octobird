package org.hiero.bot.handler.impl;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.*;
import org.hiero.bot.model.event.PullRequestEvent;
import org.hiero.bot.util.CommentMarkerChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MergeConflictHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private MergeConflictHandler handler;

    @Mock
    private ServiceRegistry registry;
    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHPullRequest ghPR;
    @Mock
    private GHIssue prAsIssue;

    @BeforeEach
    void setUp() {
        handler = new MergeConflictHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesPullRequestOpenedSynchronizeReopened() {
        // Given
        // handler initialized in setUp

        // When / Then
        assertTrue(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.OPENED));
        assertTrue(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.SYNCHRONIZE));
        assertTrue(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.REOPENED));
    }

    @Test
    void doesNotMatchOtherEvents() {
        // Given
        // handler initialized in setUp

        // When / Then
        assertFalse(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.CLOSED));
        assertFalse(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.EDITED));
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.OPENED));
    }

    @Test
    void ignoresBotSenders() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent(GitHubAction.OPENED, "Bot", false);

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    void ignoresDraftPrs() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent(GitHubAction.OPENED, "User", true);

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    void postsCommentWhenConflictDetected() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent(GitHubAction.OPENED, "User", false);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getPullRequest(42)).thenReturn(ghPR);
        when(ghPR.getMergeableState()).thenReturn("dirty");
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(prAsIssue), eq("<!-- MergeConflictBotSignature-v1 -->"))).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(prAsIssue).comment(argThat(msg ->
                    msg.contains("<!-- MergeConflictBotSignature-v1 -->")
                            && msg.contains("merge conflicts")));
        }
    }

    @Test
    void skipsCleanPullRequests() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent(GitHubAction.SYNCHRONIZE, "User", false);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getPullRequest(42)).thenReturn(ghPR);
        when(ghPR.getMergeableState()).thenReturn("clean");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(repo, never()).getIssue(anyInt());
    }

    @Test
    void skipsDuplicateConflictComment() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent(GitHubAction.REOPENED, "User", false);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getPullRequest(42)).thenReturn(ghPR);
        when(ghPR.getMergeableState()).thenReturn("dirty");
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(prAsIssue), eq("<!-- MergeConflictBotSignature-v1 -->"))).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(prAsIssue, never()).comment(any());
        }
    }

    private PullRequestEvent buildEvent(final GitHubAction action, final String senderType,
                                        final boolean draft) {
        final User sender = new User(1, "alice", senderType, null, null, false);
        final User prUser = new User(2, "bob", "User", null, null, false);
        final PullRequest pullRequest = new PullRequest(1, 42, "Test PR", null, "open", draft, false,
                prUser, null, List.of(), List.of(), List.of(), null, null, null,
                null, null, null, null, null, null, 0, 0, 0, 0);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new PullRequestEvent(action, 42, pullRequest, repository, sender, installation);
    }
}
