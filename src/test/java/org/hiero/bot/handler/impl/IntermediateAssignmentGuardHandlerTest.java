package org.hiero.bot.handler.impl;

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
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IntermediateAssignmentGuardHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private IntermediateAssignmentGuardHandler handler;

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
        handler = new IntermediateAssignmentGuardHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesIssuesAssigned() {
        // Given
        // handler initialized in setUp

        // When
        final boolean result = handler.matches(GitHubEventType.ISSUES, GitHubAction.ASSIGNED);

        // Then
        assertTrue(result);
    }

    @Test
    void doesNotMatchOtherEvents() {
        // Given
        // handler initialized in setUp

        // When / Then
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.LABELED));
        assertFalse(handler.matches(GitHubEventType.ISSUE_COMMENT, GitHubAction.CREATED));
    }

    @Test
    void guardIsDeactivatedWhenRequiredCountIsZero() throws IOException {
        // Given
        // Default guards config has requiredBeginnerCountForIntermediate == 0, so guard is deactivated
        final IssuesEvent event = buildEvent("alice");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        // Should return immediately without any interactions
        verifyNoInteractions(repo, issue);
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