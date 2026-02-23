package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.MentorRosterLoader;
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
class MentorAssignmentHandlerTest {

    private MentorAssignmentHandler handler;

    @Mock private MentorRosterLoader rosterLoader;
    @Mock private IssueSearchHelper searchHelper;
    @Mock private CommentMarkerChecker markerChecker;
    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHIssue issue;

    @BeforeEach
    void setUp() {
        handler = new MentorAssignmentHandler(rosterLoader, searchHelper, markerChecker);
    }

    @Test
    void matchesIssuesAssigned() {
        assertTrue(handler.matches(GitHubEventType.ISSUES, GitHubAction.ASSIGNED));
    }

    @Test
    void doesNotMatchOtherEvents() {
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.LABELED));
        assertFalse(handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.CREATED));
    }

    @Test
    void skipsBotAssignees() throws IOException {
        final IssuesEvent event = buildEvent("bot-user", "Bot");
        handler.handle(event, gitHub, Map.of());
        verifyNoInteractions(repo, issue);
    }

    @Test
    void skipsNonGfiIssues() throws IOException {
        final IssuesEvent event = buildEvent("alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getLabels()).thenReturn(List.of());

        handler.handle(event, gitHub, Map.of());

        verify(issue, never()).comment(any());
    }

    @Test
    void skipsWhenMarkerAlreadyExists() throws IOException {
        final IssuesEvent event = buildEvent("alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(markerChecker.hasMarker(eq(issue), eq("<!-- Mentor Assignment Bot -->"))).thenReturn(true);

        handler.handle(event, gitHub, Map.of());

        verify(issue, never()).comment(any());
    }

    @Test
    void skipsExperiencedContributors() throws IOException {
        final IssuesEvent event = buildEvent("alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(markerChecker.hasMarker(eq(issue), eq("<!-- Mentor Assignment Bot -->"))).thenReturn(false);
        when(searchHelper.hasNoMergedPullRequests(gitHub, "owner/repo", "alice")).thenReturn(false);

        handler.handle(event, gitHub, Map.of());

        verify(issue, never()).comment(any());
    }

    @Test
    void postsMentorCommentForNewContributor() throws IOException {
        final IssuesEvent event = buildEvent("alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(markerChecker.hasMarker(eq(issue), eq("<!-- Mentor Assignment Bot -->"))).thenReturn(false);
        when(searchHelper.hasNoMergedPullRequests(gitHub, "owner/repo", "alice")).thenReturn(true);
        when(rosterLoader.loadRoster(gitHub, "owner/repo")).thenReturn(List.of("mentor1", "mentor2"));
        when(rosterLoader.selectMentor(List.of("mentor1", "mentor2"))).thenReturn("mentor1");

        handler.handle(event, gitHub, Map.of());

        verify(issue).comment(argThat(msg ->
                msg.contains("<!-- Mentor Assignment Bot -->")
                        && msg.contains("@alice")
                        && msg.contains("@mentor1")));
    }

    @Test
    void skipsWhenNoMentorsAvailable() throws IOException {
        final IssuesEvent event = buildEvent("alice", "User");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        final GHLabel gfiLabel = mock(GHLabel.class);
        when(gfiLabel.getName()).thenReturn("Good First Issue");
        when(issue.getLabels()).thenReturn(List.of(gfiLabel));
        when(markerChecker.hasMarker(eq(issue), eq("<!-- Mentor Assignment Bot -->"))).thenReturn(false);
        when(searchHelper.hasNoMergedPullRequests(gitHub, "owner/repo", "alice")).thenReturn(true);
        when(rosterLoader.loadRoster(gitHub, "owner/repo")).thenReturn(List.of());
        when(rosterLoader.selectMentor(List.of())).thenReturn(null);

        handler.handle(event, gitHub, Map.of());

        verify(issue, never()).comment(any());
    }

    private IssuesEvent buildEvent(final String assignee, final String type) {
        final User assigneeModel = new User(1, assignee, type, null, null, false);
        final Issue modelIssue = new Issue(1, 42, "Test", null, "open", null, null,
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new IssuesEvent(GitHubAction.ASSIGNED, modelIssue, assigneeModel, null, repository, assigneeModel, installation);
    }
}
