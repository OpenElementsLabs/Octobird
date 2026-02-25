package org.hiero.bot.handler.impl;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.*;
import org.hiero.bot.model.event.IssuesEvent;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeRabbitPlanTriggerHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private CodeRabbitPlanTriggerHandler handler;

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
        handler = new CodeRabbitPlanTriggerHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesIssuesLabeled() {
        assertTrue(handler.matches(GitHubEventType.ISSUES, GitHubAction.LABELED));
    }

    @Test
    void doesNotMatchOtherEvents() {
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.ASSIGNED));
        assertFalse(handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.CREATED));
    }

    @Test
    void triggersOnBeginnerLabel() throws IOException {
        final IssuesEvent event = buildEvent("beginner");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq("<!-- CodeRabbit Plan Trigger -->"))).thenReturn(false);

            handler.handle(event, registry, CONFIG);

            verify(issue).comment(argThat(msg -> msg.contains("@coderabbitai plan")));
        }
    }

    @Test
    void triggersOnIntermediateLabel() throws IOException {
        final IssuesEvent event = buildEvent("intermediate");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq("<!-- CodeRabbit Plan Trigger -->"))).thenReturn(false);

            handler.handle(event, registry, CONFIG);

            verify(issue).comment(argThat(msg -> msg.contains("@coderabbitai plan")));
        }
    }

    @Test
    void triggersOnAdvancedLabel() throws IOException {
        final IssuesEvent event = buildEvent("advanced");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq("<!-- CodeRabbit Plan Trigger -->"))).thenReturn(false);

            handler.handle(event, registry, CONFIG);

            verify(issue).comment(argThat(msg -> msg.contains("@coderabbitai plan")));
        }
    }

    @Test
    void skipsNonTriggerLabel() throws IOException {
        final IssuesEvent event = buildEvent("bug");

        handler.handle(event, registry, CONFIG);

        verifyNoInteractions(repo, issue);
    }

    @Test
    void skipsDuplicateTrigger() throws IOException {
        final IssuesEvent event = buildEvent("beginner");

        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq("<!-- CodeRabbit Plan Trigger -->"))).thenReturn(true);

            handler.handle(event, registry, CONFIG);

            verify(issue, never()).comment(any());
        }
    }

    private IssuesEvent buildEvent(final String labelName) {
        final User sender = new User(1, "alice", "User", null, null, false);
        final Label label = new Label(1, labelName, null, null);
        final Issue modelIssue = new Issue(1, 42, "Test", null, "open", null, null,
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new IssuesEvent(GitHubAction.LABELED, modelIssue, null, label, repository, sender, installation);
    }
}