package org.hiero.bot.handler.impl;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.*;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.hiero.bot.util.CommentMarkerChecker;
import org.hiero.bot.util.IssueSearchHelper;
import org.hiero.bot.util.PermissionChecker;
import org.hiero.bot.util.SpamListLoader;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GfiAssignCommandHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();
    private static final String SPAM_LIST_PATH = CONFIG.paths().spamList();

    private GfiAssignCommandHandler handler;

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

    @BeforeEach
    void setUp() {
        handler = new GfiAssignCommandHandler();
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
    void skipsNonGfiIssues() throws IOException {
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

        try (final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "alice", SPAM_LIST_PATH)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(0);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).addAssignees(ghUser);
            verify(issue).comment(argThat(msg -> msg.contains("has been assigned")));
        }
    }

    @Test
    void rejectsAlreadyAssignedUser() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));

        final GHUser assignee = mock(GHUser.class);
        when(assignee.getLogin()).thenReturn("alice");
        when(issue.getAssignees()).thenReturn(List.of(assignee));

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(issue).comment(argThat(msg -> msg.contains("already assigned")));
        verify(issue, never()).addAssignees(any(GHUser.class));
    }

    @Test
    void rejectsSpamUserExceedingLimit() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "spammer", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer", SPAM_LIST_PATH)).thenReturn(true);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "spammer")).thenReturn(1);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg -> msg.contains("limited assignment privileges")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    @Test
    void rejectsNormalUserExceedingLimit() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class)) {
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "alice", SPAM_LIST_PATH)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(2);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg -> msg.contains("exceed the limit")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    @Test
    void postsReminderOnUnassignedGfi() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("Thanks for this issue!", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCollaborator(repo, "alice")).thenReturn(false);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq("<!-- GFI assign reminder -->"))).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg ->
                    msg.contains("<!-- GFI assign reminder -->") && msg.contains("/assign")));
        }
    }

    @Test
    void skipsReminderForCollaborator() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("Some comment", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class)) {
            pc.when(() -> PermissionChecker.isCollaborator(repo, "alice")).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue, never()).comment(any());
        }
    }

    @Test
    void skipsReminderWhenAlreadyAssigned() throws IOException {
        // Given
        final IssueCommentEvent event = buildEvent("Some comment", "alice", "User");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));

        final GHUser assignee = mock(GHUser.class);
        when(issue.getAssignees()).thenReturn(List.of(assignee));

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(issue, never()).comment(any());
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