package org.hiero.bot.handler.impl;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.*;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkingCommandHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private WorkingCommandHandler handler;

    @Mock
    private ServiceRegistry registry;
    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHIssue issue;

    @BeforeEach
    void setUp() {
        handler = new WorkingCommandHandler();
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
        assertFalse(handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.EDITED));
    }

    @Test
    void ignoresCommentWithoutWorkingCommand() throws IOException {
        // Given
        IssueCommentEvent event = buildIssueEvent("just a regular comment", "alice");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    void ignoresBotComments() throws IOException {
        // Given
        IssueCommentEvent event = buildBotEvent("/working", "bot-user");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    void ignoresNonAssigneeOnIssue() throws IOException {
        // Given
        IssueCommentEvent event = buildIssueEvent("/working", "alice");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getAssignees()).thenReturn(List.of());

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(issue, never()).getComments();
    }

    @Test
    void reactsWhenAssigneeUsesWorking() throws IOException {
        // Given
        IssueCommentEvent event = buildIssueEvent("/working", "alice");

        GHUser assignee = mock(GHUser.class);
        when(assignee.getLogin()).thenReturn("alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);
        when(issue.getAssignees()).thenReturn(List.of(assignee));

        GHIssueComment ghComment = mock(GHIssueComment.class);
        when(ghComment.getBody()).thenReturn("/working");
        when(issue.getComments()).thenReturn(List.of(ghComment));

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(ghComment).createReaction(ReactionContent.EYES);
    }

    @Test
    void reactsWhenPrAuthorUsesWorking() throws IOException {
        // Given
        IssueCommentEvent event = buildPrEvent("/working", "alice");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        GHIssueComment ghComment = mock(GHIssueComment.class);
        when(ghComment.getBody()).thenReturn("/working");
        when(issue.getComments()).thenReturn(List.of(ghComment));

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verify(ghComment).createReaction(ReactionContent.EYES);
    }

    private IssueCommentEvent buildIssueEvent(String commentBody, String username) {
        User commentUser = new User(1, username, "User", null, null, false);
        Comment comment = new Comment(100, commentBody, commentUser, null, null, null, null);
        Issue modelIssue = new Issue(1, 42, "Test", null, "open", null,
                new User(2, "someone-else", "User", null, null, false),
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        Installation installation = new Installation(1, 1);
        return new IssueCommentEvent(GitHubAction.CREATED, comment, modelIssue, repository, commentUser, installation);
    }

    private IssueCommentEvent buildPrEvent(String commentBody, String username) {
        User commentUser = new User(1, username, "User", null, null, false);
        Comment comment = new Comment(200, commentBody, commentUser, null, null, null, null);
        Issue modelIssue = new Issue(1, 42, "Test", null, "open", null,
                new User(1, username, "User", null, null, false),
                null, List.of(), List.of(), false, null, true, null, null, null, null);
        Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        Installation installation = new Installation(1, 1);
        return new IssueCommentEvent(GitHubAction.CREATED, comment, modelIssue, repository, commentUser, installation);
    }

    private IssueCommentEvent buildBotEvent(String commentBody, String username) {
        User commentUser = new User(1, username, "Bot", null, null, false);
        Comment comment = new Comment(100, commentBody, commentUser, null, null, null, null);
        Issue modelIssue = new Issue(1, 42, "Test", null, "open", null, null,
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        Installation installation = new Installation(1, 1);
        return new IssueCommentEvent(GitHubAction.CREATED, comment, modelIssue, repository, commentUser, installation);
    }
}