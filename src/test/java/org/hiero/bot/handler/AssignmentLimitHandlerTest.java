package org.hiero.bot.handler;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.config.RepoConfig;
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
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentLimitHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();
    private static final String SPAM_LIST_PATH = CONFIG.paths().spamList();

    private AssignmentLimitHandler handler;

    @Mock private SpamListLoader spamListLoader;
    @Mock private PermissionChecker permissionChecker;
    @Mock private IssueSearchHelper searchHelper;
    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHIssue issue;
    @Mock private GHUser assigneeUser;

    @BeforeEach
    void setUp() {
        handler = new AssignmentLimitHandler(spamListLoader, permissionChecker, searchHelper);
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

        handler.handle(event, gitHub, CONFIG);

        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    void normalUserWithinLimitIsAllowed() throws IOException {
        IssuesEvent event = buildEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "alice")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "alice", SPAM_LIST_PATH)).thenReturn(false);
        when(searchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(2);

        handler.handle(event, gitHub, CONFIG);

        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    void normalUserExceedingLimitIsRemoved() throws IOException {
        IssuesEvent event = buildEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "alice")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "alice", SPAM_LIST_PATH)).thenReturn(false);
        when(searchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(3);

        when(gitHub.getUser("alice")).thenReturn(assigneeUser);

        handler.handle(event, gitHub, CONFIG);

        verify(issue).removeAssignees(assigneeUser);
        verify(issue).comment(argThat(msg -> msg.contains("exceed the limit of 2")));
    }

    @Test
    void spamUserOnNonGfiIsRemoved() throws IOException {
        IssuesEvent event = buildEvent("spammer");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "spammer")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "spammer", SPAM_LIST_PATH)).thenReturn(true);
        when(issue.getLabels()).thenReturn(List.of());

        when(gitHub.getUser("spammer")).thenReturn(assigneeUser);

        handler.handle(event, gitHub, CONFIG);

        verify(issue).removeAssignees(assigneeUser);
        verify(issue).comment(argThat(msg -> msg.contains("limited assignment privileges")));
    }

    @Test
    void spamUserOnGfiWithinLimitIsAllowed() throws IOException {
        IssuesEvent event = buildEvent("spammer");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "spammer")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "spammer", SPAM_LIST_PATH)).thenReturn(true);

        GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(searchHelper.countOpenAssignments(gitHub, "owner/repo", "spammer")).thenReturn(1);

        handler.handle(event, gitHub, CONFIG);

        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    void spamUserOnGfiExceedingLimitIsRemoved() throws IOException {
        IssuesEvent event = buildEvent("spammer");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(permissionChecker.isMaintainer(repo, "spammer")).thenReturn(false);
        when(spamListLoader.isSpamUser(gitHub, "owner/repo", "spammer", SPAM_LIST_PATH)).thenReturn(true);

        GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(searchHelper.countOpenAssignments(gitHub, "owner/repo", "spammer")).thenReturn(2);

        when(gitHub.getUser("spammer")).thenReturn(assigneeUser);

        handler.handle(event, gitHub, CONFIG);

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
