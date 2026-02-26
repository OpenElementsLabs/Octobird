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
import org.kohsuke.github.*;
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
class VerifiedCommitsHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private VerifiedCommitsHandler handler;

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
    @Mock
    private GHCommit ghCommit;
    @Mock
    private GHCommit.ShortInfo shortInfo;
    @Mock
    private GHVerification verification;

    @BeforeEach
    void setUp() {
        handler = new VerifiedCommitsHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesPullRequestOpenedAndSynchronize() {
        // Given
        // handler initialized in setUp

        // When / Then
        assertTrue(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.OPENED));
        assertTrue(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.SYNCHRONIZE));
    }

    @Test
    void doesNotMatchOtherEvents() {
        // Given
        // handler initialized in setUp

        // When / Then
        assertFalse(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.CLOSED));
        assertFalse(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.REOPENED));
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.OPENED));
    }

    @Test
    void ignoresBotSenders() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent(GitHubAction.OPENED, "Bot");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    @SuppressWarnings("unchecked")
    void postsCommentWhenUnverifiedCommitsFound() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent(GitHubAction.OPENED, "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getPullRequest(42)).thenReturn(ghPR);
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        final GHPullRequestCommitDetail commitDetail = mock(GHPullRequestCommitDetail.class);
        final GHPullRequestCommitDetail.Commit commit = mock(GHPullRequestCommitDetail.Commit.class);

        when(commitDetail.getSha()).thenReturn("abcdef1234567890");
        when(commitDetail.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn("feat: some feature");

        when(repo.getCommit("abcdef1234567890")).thenReturn(ghCommit);
        when(ghCommit.getCommitShortInfo()).thenReturn(shortInfo);
        when(shortInfo.getVerification()).thenReturn(verification);
        when(verification.isVerified()).thenReturn(false);

        final PagedIterable<GHPullRequestCommitDetail> pagedIterable = mock(PagedIterable.class);
        final PagedIterator<GHPullRequestCommitDetail> pagedIterator = mock(PagedIterator.class);
        when(pagedIterable.iterator()).thenReturn(pagedIterator);
        when(pagedIterator.hasNext()).thenReturn(true, false);
        when(pagedIterator.next()).thenReturn(commitDetail);
        when(ghPR.listCommits()).thenReturn(pagedIterable);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(prAsIssue), eq("<!-- commit-verification-bot -->"))).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(prAsIssue).comment(argThat(msg ->
                    msg.contains("<!-- commit-verification-bot -->")
                            && msg.contains("unverified commit")));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void skipsCommentWhenAllCommitsVerified() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent(GitHubAction.SYNCHRONIZE, "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getPullRequest(42)).thenReturn(ghPR);

        final GHPullRequestCommitDetail commitDetail = mock(GHPullRequestCommitDetail.class);

        when(commitDetail.getSha()).thenReturn("abcdef1234567890");
        when(repo.getCommit("abcdef1234567890")).thenReturn(ghCommit);
        when(ghCommit.getCommitShortInfo()).thenReturn(shortInfo);
        when(shortInfo.getVerification()).thenReturn(verification);
        when(verification.isVerified()).thenReturn(true);

        final PagedIterable<GHPullRequestCommitDetail> pagedIterable = mock(PagedIterable.class);
        final PagedIterator<GHPullRequestCommitDetail> pagedIterator = mock(PagedIterator.class);
        when(pagedIterable.iterator()).thenReturn(pagedIterator);
        when(pagedIterator.hasNext()).thenReturn(true, false);
        when(pagedIterator.next()).thenReturn(commitDetail);
        when(ghPR.listCommits()).thenReturn(pagedIterable);

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(repo, never()).getIssue(anyInt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void skipsDuplicateComment() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent(GitHubAction.OPENED, "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getPullRequest(42)).thenReturn(ghPR);
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        final GHPullRequestCommitDetail commitDetail = mock(GHPullRequestCommitDetail.class);
        final GHPullRequestCommitDetail.Commit commit = mock(GHPullRequestCommitDetail.Commit.class);

        when(commitDetail.getSha()).thenReturn("abcdef1234567890");
        when(commitDetail.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn("feat: some feature");
        when(repo.getCommit("abcdef1234567890")).thenReturn(ghCommit);
        when(ghCommit.getCommitShortInfo()).thenReturn(shortInfo);
        when(shortInfo.getVerification()).thenReturn(verification);
        when(verification.isVerified()).thenReturn(false);

        final PagedIterable<GHPullRequestCommitDetail> pagedIterable = mock(PagedIterable.class);
        final PagedIterator<GHPullRequestCommitDetail> pagedIterator = mock(PagedIterator.class);
        when(pagedIterable.iterator()).thenReturn(pagedIterator);
        when(pagedIterator.hasNext()).thenReturn(true, false);
        when(pagedIterator.next()).thenReturn(commitDetail);
        when(ghPR.listCommits()).thenReturn(pagedIterable);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(prAsIssue), eq("<!-- commit-verification-bot -->"))).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(prAsIssue, never()).comment(any());
        }
    }

    private PullRequestEvent buildEvent(final GitHubAction action, final String senderType) {
        final User sender = new User(1, "alice", senderType, null, null, false);
        final User prUser = new User(2, "bob", "User", null, null, false);
        final PullRequest pullRequest = new PullRequest(1, 42, "Test PR", null, "open", false, false,
                prUser, null, List.of(), List.of(), List.of(), null, null, null,
                null, null, null, null, null, null, 0, 0, 0, 0);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new PullRequestEvent(action, 42, pullRequest, repository, sender, installation);
    }
}
