package org.hiero.bot.handler.impl;

import org.hiero.bot.config.*;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.*;
import org.hiero.bot.model.event.IssuesEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentLimitHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();
    private static final String SPAM_LIST_PATH = CONFIG.paths().spamList();

    private AssignmentLimitHandler handler;

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

    @BeforeEach
    void setUp() {
        handler = new AssignmentLimitHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
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
        final IssuesEvent event = buildEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class)) {
            pc.when(() -> PermissionChecker.isMaintainer(repo, "alice")).thenReturn(true);

            handler.handle(event, registry, CONFIG);

            verify(issue, never()).removeAssignees(any(GHUser.class));
        }
    }

    @Test
    void normalUserWithinLimitIsAllowed() throws IOException {
        final IssuesEvent event = buildEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isMaintainer(repo, "alice")).thenReturn(false);
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "alice", SPAM_LIST_PATH)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(2);

            handler.handle(event, registry, CONFIG);

            verify(issue, never()).removeAssignees(any(GHUser.class));
        }
    }

    @Test
    void normalUserExceedingLimitIsRemoved() throws IOException {
        final IssuesEvent event = buildEvent("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(gitHub.getUser("alice")).thenReturn(assigneeUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isMaintainer(repo, "alice")).thenReturn(false);
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "alice", SPAM_LIST_PATH)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(3);

            handler.handle(event, registry, CONFIG);

            verify(issue).removeAssignees(assigneeUser);
            verify(issue).comment(argThat(msg -> msg.contains("exceed the limit of 2")));
        }
    }

    @Test
    void spamUserOnNonGfiIsRemoved() throws IOException {
        final IssuesEvent event = buildEvent("spammer");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getLabels()).thenReturn(List.of());
        when(gitHub.getUser("spammer")).thenReturn(assigneeUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class)) {
            pc.when(() -> PermissionChecker.isMaintainer(repo, "spammer")).thenReturn(false);
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer", SPAM_LIST_PATH)).thenReturn(true);

            handler.handle(event, registry, CONFIG);

            verify(issue).removeAssignees(assigneeUser);
            verify(issue).comment(argThat(msg -> msg.contains("limited assignment privileges")));
        }
    }

    @Test
    void spamUserOnGfiWithinLimitIsAllowed() throws IOException {
        final IssuesEvent event = buildEvent("spammer");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isMaintainer(repo, "spammer")).thenReturn(false);
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer", SPAM_LIST_PATH)).thenReturn(true);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "spammer")).thenReturn(1);

            handler.handle(event, registry, CONFIG);

            verify(issue, never()).removeAssignees(any(GHUser.class));
        }
    }

    @Test
    void spamUserOnGfiExceedingLimitIsRemoved() throws IOException {
        final IssuesEvent event = buildEvent("spammer");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(gitHub.getUser("spammer")).thenReturn(assigneeUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            pc.when(() -> PermissionChecker.isMaintainer(repo, "spammer")).thenReturn(false);
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer", SPAM_LIST_PATH)).thenReturn(true);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "spammer")).thenReturn(2);

            handler.handle(event, registry, CONFIG);

            verify(issue).removeAssignees(assigneeUser);
            verify(issue).comment(argThat(msg -> msg.contains("limited assignment privileges")
                    && msg.contains("1 open assignment")));
        }
    }

    private IssuesEvent buildEvent(final String assignee) {
        final User assigneeModel = new User(1, assignee, "User", null, null, false);
        final Issue modelIssue = new Issue(1, 42, "Test", null, "open", null, null,
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new IssuesEvent(GitHubAction.ASSIGNED, modelIssue, assigneeModel, null, repository, assigneeModel, installation);
    }
}