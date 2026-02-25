package org.hiero.bot.handler;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
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
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnassignCommandHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private UnassignCommandHandler handler;

    @Mock private ServiceRegistry registry;
    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHIssue issue;
    @Mock private GHUser user;

    @BeforeEach
    void setUp() {
        handler = new UnassignCommandHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesIssueCommentCreated() {
        assertTrue(handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.CREATED));
    }

    @Test
    void doesNotMatchOtherEvents() {
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.OPENED));
        assertFalse(handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.DELETED));
    }

    @Test
    void ignoresCommentWithoutUnassignCommand() throws IOException {
        IssueCommentEvent event = buildEvent("/assign", "alice", "open", false, "User");
        handler.handle(event, registry, CONFIG);
        verifyNoInteractions(gitHub);
    }

    @Test
    void ignoresPullRequests() throws IOException {
        IssueCommentEvent event = buildEvent("/unassign", "alice", "open", true, "User");
        handler.handle(event, registry, CONFIG);
        verifyNoInteractions(gitHub);
    }

    @Test
    void ignoresClosedIssues() throws IOException {
        IssueCommentEvent event = buildEvent("/unassign", "alice", "closed", false, "User");
        handler.handle(event, registry, CONFIG);
        verifyNoInteractions(gitHub);
    }

    @Test
    void ignoresBotComments() throws IOException {
        IssueCommentEvent event = buildEvent("/unassign", "bot-user", "open", false, "Bot");
        handler.handle(event, registry, CONFIG);
        verifyNoInteractions(gitHub);
    }

    @Test
    void ignoresNonAssigneeUnassign() throws IOException {
        IssueCommentEvent event = buildEvent("/unassign", "alice", "open", false, "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getAssignees()).thenReturn(List.of());

        handler.handle(event, registry, CONFIG);

        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void unassignsCurrentAssignee() throws IOException {
        IssueCommentEvent event = buildEvent("/unassign", "alice", "open", false, "User");

        GHUser assignee = mock(GHUser.class);
        when(assignee.getLogin()).thenReturn("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getAssignees()).thenReturn(List.of(assignee));

        PagedIterable<GHIssueComment> pagedIterable = mock(PagedIterable.class);
        PagedIterator<GHIssueComment> pagedIterator = mock(PagedIterator.class);
        when(pagedIterable.iterator()).thenReturn(pagedIterator);
        when(pagedIterator.hasNext()).thenReturn(false);
        when(issue.listComments()).thenReturn(pagedIterable);

        when(gitHub.getUser("alice")).thenReturn(user);

        handler.handle(event, registry, CONFIG);

        verify(issue).removeAssignees(user);
        verify(issue).comment(argThat(msg -> msg.contains("<!-- unassign-requested:alice -->")
                && msg.contains("you've been unassigned")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void skipsDuplicateUnassign() throws IOException {
        IssueCommentEvent event = buildEvent("/unassign", "alice", "open", false, "User");

        GHUser assignee = mock(GHUser.class);
        when(assignee.getLogin()).thenReturn("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getAssignees()).thenReturn(List.of(assignee));

        GHIssueComment existingComment = mock(GHIssueComment.class);
        when(existingComment.getBody()).thenReturn("<!-- unassign-requested:alice -->\n\nSome text");

        PagedIterable<GHIssueComment> pagedIterable = mock(PagedIterable.class);
        Iterator<GHIssueComment> iterator = List.of(existingComment).iterator();
        when(pagedIterable.iterator()).thenReturn((PagedIterator<GHIssueComment>) mock(PagedIterator.class, invocation -> {
            if (invocation.getMethod().getName().equals("hasNext")) return iterator.hasNext();
            if (invocation.getMethod().getName().equals("next")) return iterator.next();
            return invocation.callRealMethod();
        }));
        when(issue.listComments()).thenReturn(pagedIterable);

        handler.handle(event, registry, CONFIG);

        verify(issue, never()).removeAssignees(any(GHUser.class));
    }

    @Test
    void recognizesUnassignInMiddleOfText() throws IOException {
        IssueCommentEvent event = buildEvent("I want to /unassign from this", "alice", "open", false, "User");

        GHUser assignee = mock(GHUser.class);
        when(assignee.getLogin()).thenReturn("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getAssignees()).thenReturn(List.of(assignee));

        @SuppressWarnings("unchecked")
        PagedIterable<GHIssueComment> pagedIterable = mock(PagedIterable.class);
        @SuppressWarnings("unchecked")
        PagedIterator<GHIssueComment> pagedIterator = mock(PagedIterator.class);
        when(pagedIterable.iterator()).thenReturn(pagedIterator);
        when(pagedIterator.hasNext()).thenReturn(false);
        when(issue.listComments()).thenReturn(pagedIterable);

        when(gitHub.getUser("alice")).thenReturn(user);

        handler.handle(event, registry, CONFIG);

        verify(issue).removeAssignees(user);
    }

    private IssueCommentEvent buildEvent(String commentBody, String username, String state,
                                          boolean hasPr, String userType) {
        User commentUser = new User(1, username, userType, null, null, false);
        Comment comment = new Comment(100, commentBody, commentUser, null, null, null, null);
        Issue modelIssue = new Issue(1, 42, "Test", null, state, null,
                new User(2, "someone", "User", null, null, false),
                null, List.of(), List.of(), false, null, hasPr, null, null, null, null);
        Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        Installation installation = new Installation(1, 1);
        return new IssueCommentEvent(GitHubAction.CREATED, comment, modelIssue, repository, commentUser, installation);
    }
}