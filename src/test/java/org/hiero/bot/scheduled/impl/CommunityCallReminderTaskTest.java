package org.hiero.bot.scheduled.impl;

import org.hiero.bot.config.CommunityCallConfig;
import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.FeaturesConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.config.ScheduledConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.util.CommentMarkerChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityCallReminderTaskTest {

    private final CommunityCallReminderTask task = new CommunityCallReminderTask();

    @Mock
    private ServiceRegistry registry;
    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHIssue issue;
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
    void isMeetingDay_anchorIsToday_returnsTrue() {
        // Given
        final String today = LocalDate.now(ZoneOffset.UTC).toString();

        // When
        final boolean result = CommunityCallReminderTask.isMeetingDay(today, List.of());

        // Then
        assertTrue(result);
    }

    @Test
    void isMeetingDay_anchorIs14DaysAgo_returnsTrue() {
        // Given
        final String anchor = LocalDate.now(ZoneOffset.UTC).minusDays(14).toString();

        // When
        final boolean result = CommunityCallReminderTask.isMeetingDay(anchor, List.of());

        // Then
        assertTrue(result);
    }

    @Test
    void isMeetingDay_anchorIs28DaysAgo_returnsTrue() {
        // Given
        final String anchor = LocalDate.now(ZoneOffset.UTC).minusDays(28).toString();

        // When
        final boolean result = CommunityCallReminderTask.isMeetingDay(anchor, List.of());

        // Then
        assertTrue(result);
    }

    @Test
    void isMeetingDay_anchorIs7DaysAgo_returnsFalse() {
        // Given
        final String anchor = LocalDate.now(ZoneOffset.UTC).minusDays(7).toString();

        // When
        final boolean result = CommunityCallReminderTask.isMeetingDay(anchor, List.of());

        // Then
        assertFalse(result);
    }

    @Test
    void isMeetingDay_anchorInFuture_returnsFalse() {
        // Given
        final String anchor = LocalDate.now(ZoneOffset.UTC).plusDays(14).toString();

        // When
        final boolean result = CommunityCallReminderTask.isMeetingDay(anchor, List.of());

        // Then
        assertFalse(result);
    }

    @Test
    void isMeetingDay_todayIsCancelled_returnsFalse() {
        // Given
        final String today = LocalDate.now(ZoneOffset.UTC).toString();

        // When
        final boolean result = CommunityCallReminderTask.isMeetingDay(today, List.of(today));

        // Then
        assertFalse(result);
    }

    @Test
    void isMeetingDay_blankAnchor_returnsFalse() {
        // Given
        // blank anchor date string

        // When
        final boolean result = CommunityCallReminderTask.isMeetingDay("", List.of());

        // Then
        assertFalse(result);
    }

    @Test
    void isMeetingDay_nullAnchor_returnsFalse() {
        // Given
        // null anchor date

        // When
        final boolean result = CommunityCallReminderTask.isMeetingDay(null, List.of());

        // Then
        assertFalse(result);
    }

    @Test
    void isMeetingDay_invalidAnchor_returnsFalse() {
        // Given
        // invalid date format

        // When
        final boolean result = CommunityCallReminderTask.isMeetingDay("not-a-date", List.of());

        // Then
        assertFalse(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_postsReminderOnNewestIssuePerAuthor() throws IOException {
        // Given
        final PagedIterable<GHIssue> issuePaged = pagedOf(List.of(issue));
        final RepoConfig config = configWithAnchorToday();
        final GHIssueQueryBuilder.ForRepository queryBuilder = mock(GHIssueQueryBuilder.ForRepository.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryIssues()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(issuePaged);
        when(issue.isPullRequest()).thenReturn(false);
        when(issue.getUser()).thenReturn(user);
        when(user.getLogin()).thenReturn("alice");
        when(user.getType()).thenReturn("User");

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), any())).thenReturn(false);

            // When
            task.run(registry, config);

            // Then
            verify(issue).comment(any());
        }
    }

    @Test
    void run_skipsWhenNotMeetingDay() throws IOException {
        // Given
        final RepoConfig config = configWithAnchorInFuture();

        // When
        task.run(registry, config);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_skipsIssueWithMarkerAlreadyPresent() throws IOException {
        // Given
        final PagedIterable<GHIssue> issuePaged = pagedOf(List.of(issue));
        final RepoConfig config = configWithAnchorToday();
        final GHIssueQueryBuilder.ForRepository queryBuilder = mock(GHIssueQueryBuilder.ForRepository.class);
        when(registry.getGitHub()).thenReturn(gitHub);
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.queryIssues()).thenReturn(queryBuilder);
        when(queryBuilder.state(GHIssueState.OPEN)).thenReturn(queryBuilder);
        when(queryBuilder.list()).thenReturn(issuePaged);
        when(issue.isPullRequest()).thenReturn(false);
        when(issue.getUser()).thenReturn(user);
        when(user.getLogin()).thenReturn("alice");
        when(user.getType()).thenReturn("User");

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), any())).thenReturn(true);

            // When
            task.run(registry, config);

            // Then
            verify(issue, never()).comment(any());
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

    private static RepoConfig configWithAnchorToday() {
        final DefaultRepoConfig defaults = DefaultRepoConfig.allDefaults("owner/repo");
        final String today = LocalDate.now(ZoneOffset.UTC).toString();
        final CommunityCallConfig communityCall = new CommunityCallConfig(today, "", "", List.of(), List.of());
        final ScheduledConfig scheduled = new ScheduledConfig(
                defaults.scheduled().inactivityDays(),
                defaults.scheduled().issueReminderDays(),
                defaults.scheduled().prInactivityDays(),
                defaults.scheduled().linkedIssueEnforcerDays(),
                defaults.scheduled().requireAuthorAssigned(),
                communityCall,
                defaults.scheduled().officeHours());
        return new DefaultRepoConfig(
                "owner/repo", defaults.labels(), defaults.assignmentLimits(), defaults.guards(),
                defaults.features(), defaults.markers(), defaults.commands(),
                defaults.paths(), defaults.codeRabbit(), defaults.teams(), scheduled);
    }

    private static RepoConfig configWithAnchorInFuture() {
        final DefaultRepoConfig defaults = DefaultRepoConfig.allDefaults("owner/repo");
        final String future = LocalDate.now(ZoneOffset.UTC).plusDays(14).toString();
        final CommunityCallConfig communityCall = new CommunityCallConfig(future, "", "", List.of(), List.of());
        final ScheduledConfig scheduled = new ScheduledConfig(
                defaults.scheduled().inactivityDays(),
                defaults.scheduled().issueReminderDays(),
                defaults.scheduled().prInactivityDays(),
                defaults.scheduled().linkedIssueEnforcerDays(),
                defaults.scheduled().requireAuthorAssigned(),
                communityCall,
                defaults.scheduled().officeHours());
        return new DefaultRepoConfig(
                "owner/repo", defaults.labels(), defaults.assignmentLimits(), defaults.guards(),
                defaults.features(), defaults.markers(), defaults.commands(),
                defaults.paths(), defaults.codeRabbit(), defaults.teams(), scheduled);
    }

    private static RepoConfig configWithFeatureDisabled() {
        final DefaultRepoConfig defaults = DefaultRepoConfig.allDefaults("owner/repo");
        final FeaturesConfig features = new FeaturesConfig(
                true, true, true, true, true,
                true, true, true, true, true, true,
                true, true, true, true, false, true);
        return new DefaultRepoConfig(
                "owner/repo", defaults.labels(), defaults.assignmentLimits(), defaults.guards(),
                features, defaults.markers(), defaults.commands(),
                defaults.paths(), defaults.codeRabbit(), defaults.teams(), defaults.scheduled());
    }
}
