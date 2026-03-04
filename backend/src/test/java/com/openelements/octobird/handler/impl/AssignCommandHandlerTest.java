package com.openelements.octobird.handler.impl;

import com.openelements.octobird.config.DefaultRepoConfig;
import com.openelements.octobird.config.GuardsConfig;
import com.openelements.octobird.config.IssueLevel;
import com.openelements.octobird.config.RepoConfig;
import com.openelements.octobird.handler.ServiceRegistry;
import com.openelements.octobird.model.*;
import com.openelements.octobird.model.event.IssueCommentEvent;
import com.openelements.octobird.service.MentorService;
import com.openelements.octobird.service.SpamUserService;
import com.openelements.octobird.util.CommentMarkerChecker;
import com.openelements.octobird.util.IssueSearchHelper;
import com.openelements.octobird.util.PermissionChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignCommandHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private AssignCommandHandler handler;

    @Mock
    private ServiceRegistry registry;
    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHIssue issue;
    @Mock
    private GHUser ghUser;
    @Mock
    private SpamUserService spamUserService;
    @Mock
    private MentorService mentorService;

    @BeforeEach
    void setUp() {
        handler = new AssignCommandHandler(spamUserService, mentorService);
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesIssueCommentCreated() {
        // Given
        // handler initialized in setUp

        // When
        final boolean result = handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.CREATED);

        // Then
        assertTrue(result);
    }

    @Test
    void doesNotMatchOtherEvents() {
        // Given
        // handler initialized in setUp

        // When / Then
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.ASSIGNED));
    }

    @Test
    void skipsBotComments() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "bot", "Bot");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(repo, issue);
    }

    @Test
    void skipsIssuesWithNoRecognizedLabel() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getLabels()).thenReturn(List.of());

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(issue, never()).comment(any());
        verify(issue, never()).addAssignees(any(GHUser.class));
    }

    @Test
    void rejectsWhenIssueAlreadyHasAssignee() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));

        final GHUser existingAssignee = mock(GHUser.class);
        when(issue.getAssignees()).thenReturn(List.of(existingAssignee));

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(issue).comment(argThat(msg -> msg.contains("already assigned")));
        verify(issue, never()).addAssignees(any(GHUser.class));
    }

    @Test
    void rejectsCommitterWithSelfAssignMessage() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "maintainer", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "maintainer")).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg -> msg.contains("committer")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    // ---- GFI tests ----

    @Test
    void assignsUserOnGfiWithAssignCommand() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());
        when(gitHub.getUser("alice")).thenReturn(ghUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "alice")).thenReturn(false);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq(CONFIG.markers().mentorAssignment()))).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).addAssignees(ghUser);
            verify(issue).comment(argThat(msg -> msg.contains("has been assigned")));
        }
    }

    @Test
    void rejectsUserExceedingAssignmentLimit() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(2);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg -> msg.contains("exceed the limit")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    @Test
    void rejectsSpamUser() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "spammer", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "spammer")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "spammer")).thenReturn(0);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg -> msg.contains("flagged for spam")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    // ---- Beginner tests ----

    @Test
    void rejectsUserWithoutGfiPrerequisiteForBeginner() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(issue.getLabels()).thenReturn(List.of(beginnerLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(0);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "Good First Issue")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg -> msg.contains("Good First Issue")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    @Test
    void assignsQualifiedUserToBeginner() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(issue.getLabels()).thenReturn(List.of(beginnerLabel));
        when(issue.getAssignees()).thenReturn(List.of());
        when(gitHub.getUser("alice")).thenReturn(ghUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(0);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "Good First Issue")).thenReturn(1);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).addAssignees(ghUser);
            verify(issue).comment(argThat(msg -> msg.contains("has been assigned")));
        }
    }

    // ---- Intermediate tests ----

    @Test
    void rejectsUserWithoutBeginnerPrerequisiteForIntermediate() throws IOException {
        // Given - use a config that requires 1 beginner issue for intermediate
        final RepoConfig config = configWithIntermediateRequirement(1);
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel intermediateLabel = mock(GHLabel.class);
        when(intermediateLabel.getName()).thenReturn("intermediate");
        when(issue.getLabels()).thenReturn(List.of(intermediateLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(0);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "beginner")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);

            // When
            handler.handle(event, registry, config);

            // Then
            verify(issue).comment(argThat(msg -> msg.contains("intermediate")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    @Test
    void committerIsToldToSelfAssign() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "maintainer", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel intermediateLabel = mock(GHLabel.class);
        when(intermediateLabel.getName()).thenReturn("intermediate");
        when(issue.getLabels()).thenReturn(List.of(intermediateLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "maintainer")).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg -> msg.contains("committer")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    // ---- Advanced tests ----

    @Test
    void rejectsUserWithoutIntermediatePrerequisiteForAdvanced() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel advancedLabel = mock(GHLabel.class);
        when(advancedLabel.getName()).thenReturn("advanced");
        when(issue.getLabels()).thenReturn(List.of(advancedLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(0);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "intermediate")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg -> msg.contains("advanced")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    @Test
    void advancedLabelTakesPrecedenceOverIntermediate() throws IOException {
        // Given - issue has both labels, advanced should win
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel advancedLabel = mock(GHLabel.class);
        when(advancedLabel.getName()).thenReturn("advanced");
        final GHLabel intermediateLabel = mock(GHLabel.class);
        when(issue.getLabels()).thenReturn(List.of(advancedLabel, intermediateLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(0);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "intermediate")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then - comment mentions "advanced" (not "intermediate")
            verify(issue).comment(argThat(msg -> msg.contains("advanced")));
        }
    }

    private static RepoConfig configWithIntermediateRequirement(final int requiredBeginner) {
        final DefaultRepoConfig defaults = DefaultRepoConfig.allDefaults();
        final Map<IssueLevel, Integer> counts = new EnumMap<>(defaults.guards().requiredCounts());
        counts.put(IssueLevel.INTERMEDIATE, requiredBeginner);
        final GuardsConfig guards = new GuardsConfig(counts);
        return new DefaultRepoConfig(
                0, "", defaults.labels(), defaults.assignmentLimits(), guards,
                defaults.features(), defaults.markers(), defaults.commands(),
                defaults.teams(), defaults.scheduled());
    }

    // ---- Mentor assignment tests ----

    @Test
    void assignsMentorToNewcomerOnGfi() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "newcomer", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());
        when(gitHub.getUser("newcomer")).thenReturn(ghUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "newcomer")).thenReturn(false);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "newcomer")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq(CONFIG.markers().mentorAssignment()))).thenReturn(false);
            sh.when(() -> IssueSearchHelper.hasNoMergedPullRequests(gitHub, "owner/repo", "newcomer")).thenReturn(true);
            when(mentorService.selectMentor(1)).thenReturn("mentor1");

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).addAssignees(ghUser);
            verify(issue, times(2)).comment(any());
            verify(issue, atLeastOnce()).comment(argThat(msg -> msg.contains("has been assigned")));
            verify(issue, atLeastOnce()).comment(argThat(msg -> msg.contains("@mentor1") && msg.contains("Welcome @newcomer")));
        }
    }

    @Test
    void skipsMentorForExperiencedContributor() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "experienced", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());
        when(gitHub.getUser("experienced")).thenReturn(ghUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "experienced")).thenReturn(false);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "experienced")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq(CONFIG.markers().mentorAssignment()))).thenReturn(false);
            sh.when(() -> IssueSearchHelper.hasNoMergedPullRequests(gitHub, "owner/repo", "experienced")).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).addAssignees(ghUser);
            verifyNoInteractions(mentorService);
        }
    }

    @Test
    void skipsMentorWhenMarkerExists() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "newcomer", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());
        when(gitHub.getUser("newcomer")).thenReturn(ghUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "newcomer")).thenReturn(false);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "newcomer")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq(CONFIG.markers().mentorAssignment()))).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).addAssignees(ghUser);
            verifyNoInteractions(mentorService);
        }
    }

    @Test
    void skipsMentorWhenNoMentorsAvailable() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "newcomer", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());
        when(gitHub.getUser("newcomer")).thenReturn(ghUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "newcomer")).thenReturn(false);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "newcomer")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq(CONFIG.markers().mentorAssignment()))).thenReturn(false);
            sh.when(() -> IssueSearchHelper.hasNoMergedPullRequests(gitHub, "owner/repo", "newcomer")).thenReturn(true);
            when(mentorService.selectMentor(1)).thenReturn(null);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).addAssignees(ghUser);
            // Only the assignment comment, no mentor comment
            verify(issue, times(1)).comment(any());
        }
    }

    @Test
    void skipsMentorOnNonGfiIssue() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(issue.getLabels()).thenReturn(List.of(beginnerLabel));
        when(issue.getAssignees()).thenReturn(List.of());
        when(gitHub.getUser("alice")).thenReturn(ghUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isCommitterOfRepo(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(0);
            when(spamUserService.isSpamUser(1, 1L)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "Good First Issue")).thenReturn(1);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).addAssignees(ghUser);
            verifyNoInteractions(mentorService);
        }
    }

    private IssueCommentEvent buildEvent(final String commentBody, final String username, final String type) {
        final User commentUser = new User(1, username, type, null, null, false);
        final Comment comment = new Comment(100, commentBody, commentUser, null, null, null, null);
        final Issue modelIssue = new Issue(1, 42, "Test", null, "open", null, null,
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new IssueCommentEvent(GitHubAction.CREATED, comment, modelIssue, repository, commentUser, installation);
    }
}
