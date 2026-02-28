package org.hiero.bot.scheduled.impl;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.FeaturesConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.util.CommentMarkerChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrInactivityReminderTaskTest {

    private final PrInactivityReminderTask task = new PrInactivityReminderTask();

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
    @Mock
    private GHCommitPointer commitPointer;
    @Mock
    private GHCommit commit;

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
    void run_postsReminderForInactivePr() throws IOException {
        // Given
        final PagedIterable<GHPullRequest> prPaged = pagedOf(List.of(pr));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo"); // prInactivityDays = 10
        final GHPullRequestQueryBuilder queryBuilder = mock(GHPullRequestQueryBuilder.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryPullRequests()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(prPaged);
        when(pr.getUser()).thenReturn(user);
        when(user.getType()).thenReturn("User");
        when(user.getLogin()).thenReturn("alice");
        when(pr.getHead()).thenReturn(commitPointer);
        when(commitPointer.getSha()).thenReturn("abc123");
        when(repo.getCommit("abc123")).thenReturn(commit);
        when(commit.getCommitDate()).thenReturn(daysAgo(11)); // 11 days > threshold of 10
        when(pr.getNumber()).thenReturn(42);
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(prAsIssue), any())).thenReturn(false);

            // When
            task.run(registry, config);

            // Then
            verify(prAsIssue).comment(any());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_skipsBotPr() throws IOException {
        // Given
        final PagedIterable<GHPullRequest> prPaged = pagedOf(List.of(pr));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo");
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
        verifyNoMoreInteractions(prAsIssue);
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_skipsActivePr() throws IOException {
        // Given
        final PagedIterable<GHPullRequest> prPaged = pagedOf(List.of(pr));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo"); // prInactivityDays = 10
        final GHPullRequestQueryBuilder queryBuilder = mock(GHPullRequestQueryBuilder.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryPullRequests()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(prPaged);
        when(pr.getUser()).thenReturn(user);
        when(user.getType()).thenReturn("User");
        when(pr.getHead()).thenReturn(commitPointer);
        when(commitPointer.getSha()).thenReturn("abc123");
        when(repo.getCommit("abc123")).thenReturn(commit);
        when(commit.getCommitDate()).thenReturn(daysAgo(3)); // 3 days < threshold of 10

        // When
        task.run(registry, config);

        // Then
        verifyNoInteractions(prAsIssue);
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_skipsDuplicateReminder() throws IOException {
        // Given
        final PagedIterable<GHPullRequest> prPaged = pagedOf(List.of(pr));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo"); // prInactivityDays = 10
        final GHPullRequestQueryBuilder queryBuilder = mock(GHPullRequestQueryBuilder.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryPullRequests()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(prPaged);
        when(pr.getUser()).thenReturn(user);
        when(user.getType()).thenReturn("User");
        when(pr.getHead()).thenReturn(commitPointer);
        when(commitPointer.getSha()).thenReturn("abc123");
        when(repo.getCommit("abc123")).thenReturn(commit);
        when(commit.getCommitDate()).thenReturn(daysAgo(11));
        when(pr.getNumber()).thenReturn(42);
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(prAsIssue), any())).thenReturn(true);

            // When
            task.run(registry, config);

            // Then
            verify(prAsIssue, never()).comment(any());
        }
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

    private static Date daysAgo(final int days) {
        return Date.from(Instant.now().minus(days, ChronoUnit.DAYS));
    }

    private static RepoConfig configWithFeatureDisabled() {
        final DefaultRepoConfig defaults = DefaultRepoConfig.allDefaults("owner/repo");
        final FeaturesConfig features = new FeaturesConfig(
                true, true, true,
                true, true, true, true, true,
                true, true, false, true, true, true);
        return new DefaultRepoConfig(
                "owner/repo", defaults.labels(), defaults.assignmentLimits(), defaults.guards(),
                features, defaults.markers(), defaults.commands(),
                defaults.paths(), defaults.teams(), defaults.scheduled());
    }
}
