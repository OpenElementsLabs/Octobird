package org.hiero.bot.scheduled.impl;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.FeaturesConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestQueryBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkedIssueEnforcerTaskTest {

    private final LinkedIssueEnforcerTask task = new LinkedIssueEnforcerTask();

    @Mock
    private ServiceRegistry registry;
    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHPullRequest pr;
    @Mock
    private GHIssue prAsIssue;
    @Mock
    private GHUser user;

    @Test
    void isActiveWhenFeatureEnabled() {
        // Given
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo");

        // When
        final boolean active = task.isActive(config);

        // Then
        assertTrue(active);
    }

    @Test
    void isInactiveWhenFeatureDisabled() {
        // Given
        final RepoConfig config = configWithFeatureDisabled();

        // When
        final boolean active = task.isActive(config);

        // Then
        assertFalse(active);
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_closesPrWithNoLinkedIssue() throws IOException {
        // Given
        final PagedIterable<GHPullRequest> prPaged = pagedOf(List.of(pr));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo"); // linkedIssueEnforcerDays = 3
        final LinkedIssueEnforcerTask task = taskWithAge(5L); // 5 days > threshold of 3
        final GHPullRequestQueryBuilder queryBuilder = mock(GHPullRequestQueryBuilder.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryPullRequests()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(prPaged);
        when(pr.getUser()).thenReturn(user);
        when(user.getType()).thenReturn("User");
        when(pr.getBody()).thenReturn("This PR has no closing keyword");
        when(pr.getNumber()).thenReturn(42);
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        // When
        task.run(registry, config);

        // Then
        verify(prAsIssue).comment(argThat(msg -> msg.contains("not linked to any issue")));
        verify(pr).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_skipsBotPr() throws IOException {
        // Given
        final PagedIterable<GHPullRequest> prPaged = pagedOf(List.of(pr));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo");
        final LinkedIssueEnforcerTask task = taskWithAge(5L);
        final GHPullRequestQueryBuilder queryBuilder = mock(GHPullRequestQueryBuilder.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryPullRequests()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(prPaged);
        when(pr.getUser()).thenReturn(user);
        when(user.getType()).thenReturn("Bot");

        // When
        task.run(registry, config);

        // Then
        verify(pr, never()).close();
        verifyNoInteractions(prAsIssue);
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_skipsPrYoungerThanThreshold() throws IOException {
        // Given
        final PagedIterable<GHPullRequest> prPaged = pagedOf(List.of(pr));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo"); // linkedIssueEnforcerDays = 3
        final LinkedIssueEnforcerTask task = taskWithAge(1L); // 1 day < threshold of 3
        final GHPullRequestQueryBuilder queryBuilder = mock(GHPullRequestQueryBuilder.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryPullRequests()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(prPaged);
        when(pr.getUser()).thenReturn(user);
        when(user.getType()).thenReturn("User");

        // When
        task.run(registry, config);

        // Then
        verify(pr, never()).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_closesPrWhenAuthorNotAssignedToLinkedIssue() throws IOException {
        // Given
        final PagedIterable<GHPullRequest> prPaged = pagedOf(List.of(pr));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo"); // requireAuthorAssigned = true
        final LinkedIssueEnforcerTask task = taskWithAge(5L);
        final GHPullRequestQueryBuilder queryBuilder = mock(GHPullRequestQueryBuilder.class);
        final GHIssue linkedIssue = mock(GHIssue.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryPullRequests()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(prPaged);
        when(pr.getUser()).thenReturn(user);
        when(user.getType()).thenReturn("User");
        when(user.getLogin()).thenReturn("alice");
        when(pr.getBody()).thenReturn("Fixes #100");
        when(repo.getIssue(100)).thenReturn(linkedIssue);
        when(linkedIssue.getState()).thenReturn(GHIssueState.OPEN);
        when(linkedIssue.getAssignees()).thenReturn(List.of()); // alice is not assigned
        when(pr.getNumber()).thenReturn(42);
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        // When
        task.run(registry, config);

        // Then
        verify(prAsIssue).comment(argThat(msg -> msg.contains("not assigned to the linked issue")));
        verify(pr).close();
    }

    @SuppressWarnings("unchecked")
    private <T> PagedIterable<T> pagedOf(final List<T> items) {
        final PagedIterator<T> pageIter = mock(PagedIterator.class);
        final Iterator<T> delegate = items.iterator();
        doAnswer(inv -> delegate.hasNext()).when(pageIter).hasNext();
        if (!items.isEmpty()) {
            doAnswer(inv -> delegate.next()).when(pageIter).next();
        }
        return new PagedIterable<T>() {
            @Override
            public PagedIterator<T> _iterator(int pageSize) {
                return pageIter;
            }
        };
    }

    private static LinkedIssueEnforcerTask taskWithAge(final long ageInDays) {
        return new LinkedIssueEnforcerTask() {
            @Override
            long prAgeInDays(final GHPullRequest ignored) {
                return ageInDays;
            }
        };
    }

    private static RepoConfig configWithFeatureDisabled() {
        final DefaultRepoConfig defaults = DefaultRepoConfig.allDefaults("owner/repo");
        final FeaturesConfig features = new FeaturesConfig(
                true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true,
                true, true, true, false, true, true);
        return new DefaultRepoConfig(
                "owner/repo", defaults.labels(), defaults.assignmentLimits(), defaults.guards(),
                features, defaults.markers(), defaults.commands(),
                defaults.paths(), defaults.codeRabbit(), defaults.teams(), defaults.scheduled());
    }
}
