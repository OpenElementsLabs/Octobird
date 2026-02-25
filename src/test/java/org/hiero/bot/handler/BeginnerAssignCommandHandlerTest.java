package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.config.SpamListLoader;
import org.hiero.bot.model.Comment;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.Installation;
import org.hiero.bot.model.Issue;
import org.hiero.bot.model.Repository;
import org.hiero.bot.model.User;
import org.hiero.bot.model.event.IssueCommentEvent;
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
class BeginnerAssignCommandHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();
    private static final String SPAM_LIST_PATH = CONFIG.paths().spamList();

    private BeginnerAssignCommandHandler handler;

    @Mock private ServiceRegistry registry;
    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHIssue issue;
    @Mock private GHUser ghUser;

    @BeforeEach
    void setUp() {
        handler = new BeginnerAssignCommandHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesIssueCommentCreated() {
        assertTrue(handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.CREATED));
    }

    @Test
    void doesNotMatchOtherEvents() {
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.ASSIGNED));
    }

    @Test
    void skipsBotComments() throws IOException {
        final IssueCommentEvent event = buildEvent("/assign", "bot", "Bot");
        handler.handle(event, registry, CONFIG);
        verifyNoInteractions(repo, issue);
    }

    @Test
    void skipsNonBeginnerIssues() throws IOException {
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getLabels()).thenReturn(List.of());

        handler.handle(event, registry, CONFIG);

        verify(issue, never()).comment(any());
        verify(issue, never()).addAssignees(any(GHUser.class));
    }

    @Test
    void rejectsUserWithoutGfiPrerequisite() throws IOException {
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(issue.getLabels()).thenReturn(List.of(beginnerLabel));

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isExemptFromGuard(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "Good First Issue")).thenReturn(0);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), contains("@alice"))).thenReturn(false);

            handler.handle(event, registry, CONFIG);

            verify(issue).comment(argThat(msg -> msg.contains("Good First Issue")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    @Test
    void blocksSpamUsersCompletely() throws IOException {
        final IssueCommentEvent event = buildEvent("/assign", "spammer", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(issue.getLabels()).thenReturn(List.of(beginnerLabel));

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class)) {
            pc.when(() -> PermissionChecker.isExemptFromGuard(repo, "spammer")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "spammer", "Good First Issue")).thenReturn(1);
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "spammer", SPAM_LIST_PATH)).thenReturn(true);

            handler.handle(event, registry, CONFIG);

            verify(issue).comment(argThat(msg -> msg.contains("limited assignment privileges")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    @Test
    void assignsQualifiedUser() throws IOException {
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(issue.getLabels()).thenReturn(List.of(beginnerLabel));
        when(issue.getAssignees()).thenReturn(List.of());
        when(gitHub.getUser("alice")).thenReturn(ghUser);

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class)) {
            pc.when(() -> PermissionChecker.isExemptFromGuard(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "Good First Issue")).thenReturn(1);
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "alice", SPAM_LIST_PATH)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(0);

            handler.handle(event, registry, CONFIG);

            verify(issue).addAssignees(ghUser);
            verify(issue).comment(argThat(msg -> msg.contains("has been assigned")));
        }
    }

    @Test
    void rejectsUserExceedingAssignmentLimit() throws IOException {
        final IssueCommentEvent event = buildEvent("/assign", "alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(issue.getLabels()).thenReturn(List.of(beginnerLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<IssueSearchHelper> sh = mockStatic(IssueSearchHelper.class);
             final MockedStatic<SpamListLoader> sl = mockStatic(SpamListLoader.class)) {
            pc.when(() -> PermissionChecker.isExemptFromGuard(repo, "alice")).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "Good First Issue")).thenReturn(1);
            sl.when(() -> SpamListLoader.isSpamUser(gitHub, "owner/repo", "alice", SPAM_LIST_PATH)).thenReturn(false);
            sh.when(() -> IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice")).thenReturn(2);

            handler.handle(event, registry, CONFIG);

            verify(issue).comment(argThat(msg -> msg.contains("exceed the limit")));
            verify(issue, never()).addAssignees(any(GHUser.class));
        }
    }

    @Test
    void postsReminderOnUnassignedBeginnerIssue() throws IOException {
        final IssueCommentEvent event = buildEvent("Thanks!", "alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(issue.getLabels()).thenReturn(List.of(beginnerLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class);
             final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            pc.when(() -> PermissionChecker.isCollaborator(repo, "alice")).thenReturn(false);
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq("<!-- beginner assign reminder -->"))).thenReturn(false);

            handler.handle(event, registry, CONFIG);

            verify(issue).comment(argThat(msg ->
                    msg.contains("<!-- beginner assign reminder -->") && msg.contains("/assign")));
        }
    }

    @Test
    void skipsReminderForCollaborator() throws IOException {
        final IssueCommentEvent event = buildEvent("Some comment", "alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel beginnerLabel = mock(GHLabel.class);
        when(beginnerLabel.getName()).thenReturn("beginner");
        when(issue.getLabels()).thenReturn(List.of(beginnerLabel));
        when(issue.getAssignees()).thenReturn(List.of());

        try (final MockedStatic<PermissionChecker> pc = mockStatic(PermissionChecker.class)) {
            pc.when(() -> PermissionChecker.isCollaborator(repo, "alice")).thenReturn(true);

            handler.handle(event, registry, CONFIG);

            verify(issue, never()).comment(any());
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