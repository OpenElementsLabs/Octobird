package org.hiero.bot.scheduled.impl;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.FeaturesConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.util.IssueSearchHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueEvent;
import org.kohsuke.github.GHIssueQueryBuilder;
import org.kohsuke.github.GHIssueState;
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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InactivityUnassignTaskTest {

    private final InactivityUnassignTask task = new InactivityUnassignTask();

    @Mock
    private ServiceRegistry registry;
    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHIssue issue;
    @Mock
    private GHUser assigneeUser;

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
    void run_skipsIssuesWithNoAssignees() throws IOException {
        // Given
        final PagedIterable<GHIssue> issuePaged = pagedOf(List.of(issue));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo");
        final GHIssueQueryBuilder.ForRepository queryBuilder = mock(GHIssueQueryBuilder.ForRepository.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryIssues()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(issuePaged);
        when(issue.isPullRequest()).thenReturn(false);
        when(issue.getAssignees()).thenReturn(List.of());

        // When
        task.run(registry, config);

        // Then
        verify(issue, never()).comment(any());
        verify(issue, never()).removeAssignees((Collection<GHUser>) any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_phaseA_unassignsWhenExpiredAndNoPr() throws IOException {
        // Given
        final GHIssueEvent assignEvent = mock(GHIssueEvent.class);
        final PagedIterable<GHIssue> issuePaged = pagedOf(List.of(issue));
        final PagedIterable<GHIssueEvent> eventPaged = pagedOf(List.of(assignEvent));
        final PagedIterable<GHIssueComment> commentPaged = pagedOf(List.of());
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo"); // inactivityDays = 21
        final GHIssueQueryBuilder.ForRepository queryBuilder = mock(GHIssueQueryBuilder.ForRepository.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryIssues()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(issuePaged);
        when(issue.isPullRequest()).thenReturn(false);
        when(issue.getAssignees()).thenReturn(List.of(assigneeUser));
        when(assigneeUser.getLogin()).thenReturn("alice");
        when(issue.listEvents()).thenReturn(eventPaged);
        when(assignEvent.getEvent()).thenReturn("assigned");
        when(assignEvent.getAssignee()).thenReturn(assigneeUser);
        when(assignEvent.getCreatedAt()).thenReturn(daysAgo(25)); // 25 days > threshold of 21
        when(issue.listComments()).thenReturn(commentPaged);
        when(issue.getNumber()).thenReturn(42);
        when(repo.getFullName()).thenReturn("owner/repo");

        try (final MockedStatic<IssueSearchHelper> ish = mockStatic(IssueSearchHelper.class)) {
            ish.when(() -> IssueSearchHelper.findOpenPrsLinkingToIssue(any(), any(), anyInt()))
                    .thenReturn(List.of());

            // When
            task.run(registry, config);

            // Then
            verify(issue).comment(any());
            verify(issue).removeAssignees((Collection<GHUser>) List.of(assigneeUser));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_skipsWhenAssignedRecently() throws IOException {
        // Given
        final GHIssueEvent assignEvent = mock(GHIssueEvent.class);
        final PagedIterable<GHIssue> issuePaged = pagedOf(List.of(issue));
        final PagedIterable<GHIssueEvent> eventPaged = pagedOf(List.of(assignEvent));
        final RepoConfig config = DefaultRepoConfig.allDefaults("owner/repo"); // inactivityDays = 21
        final GHIssueQueryBuilder.ForRepository queryBuilder = mock(GHIssueQueryBuilder.ForRepository.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryIssues()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(issuePaged);
        when(issue.isPullRequest()).thenReturn(false);
        when(issue.getAssignees()).thenReturn(List.of(assigneeUser));
        when(assigneeUser.getLogin()).thenReturn("alice");
        when(issue.listEvents()).thenReturn(eventPaged);
        when(assignEvent.getEvent()).thenReturn("assigned");
        when(assignEvent.getAssignee()).thenReturn(assigneeUser);
        when(assignEvent.getCreatedAt()).thenReturn(daysAgo(5)); // 5 days < threshold of 21

        // When
        task.run(registry, config);

        // Then
        verify(issue, never()).comment(any());
        verify(issue, never()).removeAssignees((Collection<GHUser>) any());
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
                true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true,
                false, true, true, true, true, true);
        return new DefaultRepoConfig(
                "owner/repo", defaults.labels(), defaults.assignmentLimits(), defaults.guards(),
                features, defaults.markers(), defaults.commands(),
                defaults.paths(), defaults.codeRabbit(), defaults.teams(), defaults.scheduled());
    }
}
