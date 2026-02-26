package org.hiero.bot.handler.impl;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.*;
import org.hiero.bot.model.event.PullRequestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NextIssueRecommendationHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private NextIssueRecommendationHandler handler;

    @Mock
    private ServiceRegistry registry;
    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHIssue linkedIssue;
    @Mock
    private GHIssue prAsIssue;
    @Mock
    private GHIssueSearchBuilder searchBuilder;

    @BeforeEach
    void setUp() {
        handler = new NextIssueRecommendationHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesPullRequestClosed() {
        // Given
        // handler initialized in setUp

        // When
        final boolean result = handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.CLOSED);

        // Then
        assertTrue(result);
    }

    @Test
    void doesNotMatchOtherEvents() {
        // Given
        // handler initialized in setUp

        // When / Then
        assertFalse(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.OPENED));
        assertFalse(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.SYNCHRONIZE));
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.CLOSED));
    }

    @Test
    void ignoresBotSenders() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent("Bot", true, "Fixes #10");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    void skipsNonMergedClosedPrs() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent("User", false, "Fixes #10");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    void skipsWhenNoLinkedIssueInBody() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent("User", true, "Great changes!");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    void skipsWhenBodyIsNull() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent("User", true, null);

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    @SuppressWarnings("unchecked")
    void postsRecommendationForBeginnerIssue() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent("User", true, "Closes #10");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);

        // Linked issue has 'beginner' label
        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(repo.getIssue(10)).thenReturn(linkedIssue);
        when(linkedIssue.getLabels()).thenReturn((Collection) List.of(beginnerLabel));

        // Search results
        final GHIssue recommendedIssue = mock(GHIssue.class);
        when(recommendedIssue.getNumber()).thenReturn(20);
        when(recommendedIssue.getTitle()).thenReturn("Fix some other beginner issue");
        when(recommendedIssue.getHtmlUrl()).thenReturn(new URL("https://github.com/owner/repo/issues/20"));

        final PagedSearchIterable<GHIssue> searchResults = mock(PagedSearchIterable.class);
        final PagedIterator<GHIssue> searchIterator = mock(PagedIterator.class);
        when(searchResults.iterator()).thenReturn(searchIterator);
        when(searchIterator.hasNext()).thenReturn(true, false);
        when(searchIterator.next()).thenReturn(recommendedIssue);

        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(anyString())).thenReturn(searchBuilder);
        when(searchBuilder.list()).thenReturn(searchResults);

        when(repo.getIssue(42)).thenReturn(prAsIssue);

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(prAsIssue).comment(argThat(msg ->
                msg.contains("<!-- next-issue-bot-marker -->")
                        && msg.contains("Fix some other beginner issue")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void skipsIntermediateIssues() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent("User", true, "Resolves #10");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);

        final GHLabel intermediateLabel = mock(GHLabel.class);
        when(intermediateLabel.getName()).thenReturn("intermediate");
        when(repo.getIssue(10)).thenReturn(linkedIssue);
        when(linkedIssue.getLabels()).thenReturn((Collection) List.of(intermediateLabel));

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(prAsIssue, never()).comment(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void skipsIssuesWithoutDifficultyLabel() throws IOException {
        // Given
        final PullRequestEvent event = buildEvent("User", true, "Fixes #10");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);

        final GHLabel bugLabel = mock(GHLabel.class);
        when(bugLabel.getName()).thenReturn("bug");
        when(repo.getIssue(10)).thenReturn(linkedIssue);
        when(linkedIssue.getLabels()).thenReturn((Collection) List.of(bugLabel));

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(prAsIssue, never()).comment(any());
    }

    private PullRequestEvent buildEvent(final String senderType, final boolean merged,
                                        final String body) {
        final User sender = new User(1, "alice", senderType, null, null, false);
        final User prUser = new User(2, "bob", "User", null, null, false);
        final PullRequest pullRequest = new PullRequest(1, 42, "Test PR", body, "closed", false, merged,
                prUser, null, List.of(), List.of(), List.of(), null, null, null,
                null, null, null, null, null, null, 0, 0, 0, 0);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new PullRequestEvent(GitHubAction.CLOSED, 42, pullRequest, repository, sender, installation);
    }
}
