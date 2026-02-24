package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.Installation;
import org.hiero.bot.model.Issue;
import org.hiero.bot.model.Label;
import org.hiero.bot.model.Repository;
import org.hiero.bot.model.User;
import org.hiero.bot.model.event.IssuesEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvancedAssignmentGuardHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private AdvancedAssignmentGuardHandler handler;

    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHIssue issue;
    @Mock private GHUser assigneeUser;

    @BeforeEach
    void setUp() {
        handler = new AdvancedAssignmentGuardHandler();
    }

    @Test
    void matchesIssuesAssigned() {
        assertTrue(handler.matches(GitHubEventType.ISSUES, GitHubAction.ASSIGNED));
    }

    @Test
    void matchesIssuesLabeled() {
        assertTrue(handler.matches(GitHubEventType.ISSUES, GitHubAction.LABELED));
    }

    @Test
    void doesNotMatchOtherEvents() {
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.OPENED));
        assertFalse(handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.CREATED));
    }

    @Test
    void skipsNonAdvancedIssuesOnAssigned() throws IOException {
        final IssuesEvent event = buildAssignedEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getLabels()).thenReturn(List.of());

        handler.handle(event, gitHub, CONFIG);

        verify(issue, never()).comment(any());
        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    void skipsExemptUsersOnAssigned() throws IOException {
        final IssuesEvent event = buildAssignedEvent("admin-user");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel advancedLabel = mock(GHLabel.class);
        when(advancedLabel.getName()).thenReturn("advanced");
        when(issue.getLabels()).thenReturn(List.of(advancedLabel));

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class)) {
            pc.when(() -> PermissionChecker.isExemptFromGuard(repo, "admin-user")).thenReturn(true);

            handler.handle(event, gitHub, CONFIG);

            verify(issue, never()).comment(any());
            verify(issue, never()).removeAssignees(any(GHUser.class));
        }
    }

    @Test
    void removesUnqualifiedUserOnAssigned() throws IOException {
        final IssuesEvent event = buildAssignedEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel advancedLabel = mock(GHLabel.class);
        when(advancedLabel.getName()).thenReturn("advanced");
        when(issue.getLabels()).thenReturn(List.of(advancedLabel));
        when(gitHub.getUser("alice")).thenReturn(assigneeUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isExemptFromGuard(repo, "alice")).thenReturn(false);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "intermediate")).thenReturn(0);

            handler.handle(event, gitHub, CONFIG);

            verify(issue).removeAssignees(assigneeUser);
            verify(issue).comment(argThat(msg -> msg.contains("advanced") && msg.contains("@alice")));
        }
    }

    @Test
    void allowsQualifiedUserOnAssigned() throws IOException {
        final IssuesEvent event = buildAssignedEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel advancedLabel = mock(GHLabel.class);
        when(advancedLabel.getName()).thenReturn("advanced");
        when(issue.getLabels()).thenReturn(List.of(advancedLabel));

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isExemptFromGuard(repo, "alice")).thenReturn(false);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "intermediate")).thenReturn(1);

            handler.handle(event, gitHub, CONFIG);

            verify(issue, never()).removeAssignees(any(GHUser.class));
            verify(issue, never()).comment(any());
        }
    }

    @Test
    void checksAllAssigneesOnLabeled() throws IOException {
        final IssuesEvent event = buildLabeledEvent("advanced");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHUser ghUser = mock(GHUser.class);
        when(ghUser.getLogin()).thenReturn("alice");
        when(issue.getAssignees()).thenReturn(List.of(ghUser));
        when(gitHub.getUser("alice")).thenReturn(assigneeUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isExemptFromGuard(repo, "alice")).thenReturn(false);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "intermediate")).thenReturn(0);

            handler.handle(event, gitHub, CONFIG);

            verify(issue).removeAssignees(assigneeUser);
        }
    }

    @Test
    void skipsNonAdvancedLabelOnLabeled() throws IOException {
        final IssuesEvent event = buildLabeledEvent("beginner");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        handler.handle(event, gitHub, CONFIG);

        verify(issue, never()).comment(any());
        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    private IssuesEvent buildAssignedEvent(final String assignee) {
        final User assigneeModel = new User(1, assignee, "User", null, null, false);
        final Issue modelIssue = new Issue(1, 42, "Test", null, "open", null, null,
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new IssuesEvent(GitHubAction.ASSIGNED, modelIssue, assigneeModel, null, repository, assigneeModel, installation);
    }

    private IssuesEvent buildLabeledEvent(final String labelName) {
        final User sender = new User(1, "alice", "User", null, null, false);
        final Label label = new Label(1, labelName, null, null);
        final Issue modelIssue = new Issue(1, 42, "Test", null, "open", null, null,
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new IssuesEvent(GitHubAction.LABELED, modelIssue, null, label, repository, sender, installation);
    }
}
