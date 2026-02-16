package org.hiero.bot.handler;

import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.config.SpamListLoader;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.Installation;
import org.hiero.bot.model.Issue;
import org.hiero.bot.model.Repository;
import org.hiero.bot.model.User;
import org.hiero.bot.model.event.IssuesEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueSearchBuilder;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentLimitHandlerTest {

    private AssignmentLimitHandler handler;

    @Mock private SpamListLoader spamListLoader;
    @Mock private PermissionChecker permissionChecker;
    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHIssue issue;
    @Mock private GHUser assigneeUser;

    @BeforeEach
    void setUp() {
        handler = new AssignmentLimitHandler(spamListLoader, permissionChecker);
    }

    @Test
    void matchesIssuesAssigned() {
        assertTrue(handler.matches(GitHubEventType.ISSUES, GitHubAction.ASSIGNED));
    }

    @Test
    void doesNotMatchOtherEvents() {
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.OPENED));
        assertFalse(handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.CREATED));
    }

    @Test
    void maintainerHasNoLimit() throws IOException {
        IssuesEvent event = buildEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "alice")).thenReturn(true);

        handler.handle(event, gitHub, Map.of());

        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalUserWithinLimitIsAllowed() throws IOException {
        IssuesEvent event = buildEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "alice")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "alice")).thenReturn(false);

        GHIssueSearchBuilder searchBuilder = mock(GHIssueSearchBuilder.class);
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        PagedSearchIterable<GHIssue> searchResult = mock(PagedSearchIterable.class);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class), mock(GHIssue.class)));

        handler.handle(event, gitHub, Map.of());

        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void normalUserExceedingLimitIsRemoved() throws IOException {
        IssuesEvent event = buildEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "alice")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "alice")).thenReturn(false);

        GHIssueSearchBuilder searchBuilder = mock(GHIssueSearchBuilder.class);
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        PagedSearchIterable<GHIssue> searchResult = mock(PagedSearchIterable.class);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class), mock(GHIssue.class), mock(GHIssue.class)));

        when(gitHub.getUser("alice")).thenReturn(assigneeUser);

        handler.handle(event, gitHub, Map.of());

        verify(issue).removeAssignees(assigneeUser);
        verify(issue).comment(argThat(msg -> msg.contains("exceed the limit of 2")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void spamUserOnNonGfiIsRemoved() throws IOException {
        IssuesEvent event = buildEvent("spammer");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "spammer")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "spammer")).thenReturn(true);
        when(issue.getLabels()).thenReturn(List.of());

        when(gitHub.getUser("spammer")).thenReturn(assigneeUser);

        handler.handle(event, gitHub, Map.of());

        verify(issue).removeAssignees(assigneeUser);
        verify(issue).comment(argThat(msg -> msg.contains("limited assignment privileges")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void spamUserOnGfiWithinLimitIsAllowed() throws IOException {
        IssuesEvent event = buildEvent("spammer");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "spammer")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "spammer")).thenReturn(true);

        GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));

        GHIssueSearchBuilder searchBuilder = mock(GHIssueSearchBuilder.class);
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        PagedSearchIterable<GHIssue> searchResult = mock(PagedSearchIterable.class);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class)));

        handler.handle(event, gitHub, Map.of());

        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void spamUserOnGfiExceedingLimitIsRemoved() throws IOException {
        IssuesEvent event = buildEvent("spammer");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "spammer")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "spammer")).thenReturn(true);

        GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));

        GHIssueSearchBuilder searchBuilder = mock(GHIssueSearchBuilder.class);
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        PagedSearchIterable<GHIssue> searchResult = mock(PagedSearchIterable.class);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class), mock(GHIssue.class)));

        when(gitHub.getUser("spammer")).thenReturn(assigneeUser);

        handler.handle(event, gitHub, Map.of());

        verify(issue).removeAssignees(assigneeUser);
        verify(issue).comment(argThat(msg -> msg.contains("limited assignment privileges")
                && msg.contains("1 open assignment")));
    }

    private IssuesEvent buildEvent(String assignee) {
        User assigneeModel = new User(1, assignee, "User", null, null, false);
        Issue modelIssue = new Issue(1, 42, "Test", null, "open", null, null,
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        Installation installation = new Installation(1, 1);
        return new IssuesEvent(GitHubAction.ASSIGNED, modelIssue, assigneeModel, null, repository, assigneeModel, installation);
    }
}
