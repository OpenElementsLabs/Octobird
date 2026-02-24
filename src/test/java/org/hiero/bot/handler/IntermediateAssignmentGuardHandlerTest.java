package org.hiero.bot.handler;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
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
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntermediateAssignmentGuardHandlerTest {

    private static final RepoConfig CONFIG = DefaultRepoConfig.allDefaults();

    private IntermediateAssignmentGuardHandler handler;

    @Mock private GitHub gitHub;
    @Mock private GHRepository repo;
    @Mock private GHIssue issue;

    @BeforeEach
    void setUp() {
        handler = new IntermediateAssignmentGuardHandler();
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
    void guardIsDeactivatedWhenRequiredCountIsZero() throws IOException {
        // Default guards config has requiredBeginnerCountForIntermediate == 0, so guard is deactivated
        final IssuesEvent event = buildEvent("alice");

        handler.handle(event, gitHub, CONFIG);

        // Should return immediately without any interactions
        verifyNoInteractions(gitHub, repo, issue);
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
