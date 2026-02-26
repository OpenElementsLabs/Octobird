package org.hiero.bot.handler.impl;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.*;
import org.hiero.bot.model.event.WorkflowRunEvent;
import org.hiero.bot.util.CommentMarkerChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
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
class WorkflowFailureNotificationHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private WorkflowFailureNotificationHandler handler;

    @Mock
    private ServiceRegistry registry;
    @Mock
    private GitHub gitHub;
    @Mock
    private GHRepository repo;
    @Mock
    private GHIssue prAsIssue;

    @BeforeEach
    void setUp() {
        handler = new WorkflowFailureNotificationHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesWorkflowRunCompleted() {
        // Given
        // handler initialized in setUp

        // When
        final boolean result = handler.matches(GitHubEventType.WORKFLOW_RUN, GitHubAction.COMPLETED);

        // Then
        assertTrue(result);
    }

    @Test
    void doesNotMatchOtherEvents() {
        // Given
        // handler initialized in setUp

        // When / Then
        assertFalse(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.CLOSED));
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.CLOSED));
        assertFalse(handler.matches(GitHubEventType.WORKFLOW_RUN, GitHubAction.CREATED));
    }

    @Test
    void skipsNonFailureConclusion() throws IOException {
        // Given
        final WorkflowRunEvent event = buildEvent("success", List.of(42));

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    void skipsCancelledConclusion() throws IOException {
        // Given
        final WorkflowRunEvent event = buildEvent("cancelled", List.of(42));

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    @Test
    void postsCommentOnFailedWorkflowRun() throws IOException {
        // Given
        final WorkflowRunEvent event = buildEvent("failure", List.of(42));
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(prAsIssue), eq("<!-- workflowbot:workflow-failure-notifier -->"))).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(prAsIssue).comment(argThat(msg ->
                    msg.contains("<!-- workflowbot:workflow-failure-notifier -->")
                            && msg.contains("WorkflowBot")));
        }
    }

    @Test
    void postsCommentForMultiplePrs() throws IOException {
        // Given
        final WorkflowRunEvent event = buildEvent("failure", List.of(42, 43));
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);

        final GHIssue prAsIssue2 = mock(GHIssue.class);
        when(repo.getIssue(42)).thenReturn(prAsIssue);
        when(repo.getIssue(43)).thenReturn(prAsIssue2);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(any(GHIssue.class), any())).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(prAsIssue).comment(any());
            verify(prAsIssue2).comment(any());
        }
    }

    @Test
    void skipsDuplicateNotificationComment() throws IOException {
        // Given
        final WorkflowRunEvent event = buildEvent("failure", List.of(42));
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(prAsIssue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(prAsIssue), eq("<!-- workflowbot:workflow-failure-notifier -->"))).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(prAsIssue, never()).comment(any());
        }
    }

    @Test
    void skipsWhenNoPrsAssociated() throws IOException {
        // Given
        final WorkflowRunEvent event = buildEventWithBranch("failure", List.of(), null);

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(gitHub);
    }

    private WorkflowRunEvent buildEvent(final String conclusion, final List<Integer> prNumbers) {
        return buildEventWithBranch(conclusion, prNumbers, "feature/my-branch");
    }

    private WorkflowRunEvent buildEventWithBranch(final String conclusion,
                                                   final List<Integer> prNumbers,
                                                   final String headBranch) {
        final WorkflowRun workflowRun = new WorkflowRun(
                123L, "CI", headBranch, "abc123sha", conclusion,
                "https://github.com/owner/repo/actions/runs/123", prNumbers);
        final User sender = new User(1, "github-actions[bot]", "Bot", null, null, false);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new WorkflowRunEvent(GitHubAction.COMPLETED, workflowRun, repository, sender, installation);
    }
}
