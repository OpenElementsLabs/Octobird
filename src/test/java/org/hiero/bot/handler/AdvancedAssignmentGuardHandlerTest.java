package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.PermissionChecker;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvancedAssignmentGuardHandlerTest {

    private AdvancedAssignmentGuardHandler handler;

    @Mock private PermissionChecker permissionChecker;
    @Mock private IssueSearchHelper searchHelper;
    @Mock private CommentMarkerChecker markerChecker;
    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHIssue issue;
    @Mock private GHUser assigneeUser;

    @BeforeEach
    void setUp() {
        handler = new AdvancedAssignmentGuardHandler(permissionChecker, searchHelper, markerChecker);
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

        handler.handle(event, gitHub, Map.of());

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
        when(permissionChecker.isExemptFromGuard(repo, "admin-user")).thenReturn(true);

        handler.handle(event, gitHub, Map.of());

        verify(issue, never()).comment(any());
        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    void removesUnqualifiedUserOnAssigned() throws IOException {
        final IssuesEvent event = buildAssignedEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel advancedLabel = mock(GHLabel.class);
        when(advancedLabel.getName()).thenReturn("advanced");
        when(issue.getLabels()).thenReturn(List.of(advancedLabel));
        when(permissionChecker.isExemptFromGuard(repo, "alice")).thenReturn(false);
        when(markerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);
        when(searchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "intermediate")).thenReturn(0);
        when(gitHub.getUser("alice")).thenReturn(assigneeUser);

        handler.handle(event, gitHub, Map.of());

        verify(issue).removeAssignees(assigneeUser);
        verify(issue).comment(argThat(msg -> msg.contains("advanced") && msg.contains("@alice")));
    }

    @Test
    void allowsQualifiedUserOnAssigned() throws IOException {
        final IssuesEvent event = buildAssignedEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel advancedLabel = mock(GHLabel.class);
        when(advancedLabel.getName()).thenReturn("advanced");
        when(issue.getLabels()).thenReturn(List.of(advancedLabel));
        when(permissionChecker.isExemptFromGuard(repo, "alice")).thenReturn(false);
        when(markerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);
        when(searchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "intermediate")).thenReturn(1);

        handler.handle(event, gitHub, Map.of());

        verify(issue, never()).removeAssignees(any(GHUser.class));
        verify(issue, never()).comment(any());
    }

    @Test
    void checksAllAssigneesOnLabeled() throws IOException {
        final IssuesEvent event = buildLabeledEvent("advanced");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHUser ghUser = mock(GHUser.class);
        when(ghUser.getLogin()).thenReturn("alice");
        when(issue.getAssignees()).thenReturn(List.of(ghUser));
        when(permissionChecker.isExemptFromGuard(repo, "alice")).thenReturn(false);
        when(markerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);
        when(searchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "intermediate")).thenReturn(0);
        when(gitHub.getUser("alice")).thenReturn(assigneeUser);

        handler.handle(event, gitHub, Map.of());

        verify(issue).removeAssignees(assigneeUser);
    }

    @Test
    void skipsNonAdvancedLabelOnLabeled() throws IOException {
        final IssuesEvent event = buildLabeledEvent("beginner");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        handler.handle(event, gitHub, Map.of());

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
