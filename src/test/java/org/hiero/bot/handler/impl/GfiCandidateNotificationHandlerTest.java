package org.hiero.bot.handler.impl;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.FeaturesConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.config.TeamsConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.Installation;
import org.hiero.bot.model.Issue;
import org.hiero.bot.model.Label;
import org.hiero.bot.model.Repository;
import org.hiero.bot.model.User;
import org.hiero.bot.model.event.IssuesEvent;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GfiCandidateNotificationHandlerTest {

    private static final RepoConfig CONFIG = configWithTeam("@org/gfi-support");

    private GfiCandidateNotificationHandler handler;

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
        handler = new GfiCandidateNotificationHandler();
        lenient().when(registry.getGitHub()).thenReturn(gitHub);
    }

    @Test
    void matchesIssuesLabeled() {
        // Given
        // handler initialized in setUp

        // When
        final boolean result = handler.matches(GitHubEventType.ISSUES, GitHubAction.LABELED);

        // Then
        assertTrue(result);
    }

    @Test
    void doesNotMatchOtherEvents() {
        // Given
        // handler initialized in setUp

        // When / Then
        assertFalse(handler.matches(GitHubEventType.ISSUES, GitHubAction.OPENED));
        assertFalse(handler.matches(GitHubEventType.PULL_REQUEST, GitHubAction.LABELED));
    }

    @Test
    void postsNotificationWhenGfiCandidateLabelAdded() throws IOException {
        // Given
        final IssuesEvent event = buildEvent("good first issue candidate");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq("<!-- GFI Candidate Notification -->"))).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(argThat(msg ->
                    msg.contains("<!-- GFI Candidate Notification -->")
                            && msg.contains("@org/gfi-support")
                            && msg.contains("Good First Issue Candidate")
                            && msg.contains("#42")));
        }
    }

    @Test
    void postsNotificationForMixedCaseCandidateLabel() throws IOException {
        // Given
        final IssuesEvent event = buildEvent("Good First Issue Candidate");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), any())).thenReturn(false);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue).comment(any());
        }
    }

    @Test
    void skipsNonCandidateLabel() throws IOException {
        // Given
        final IssuesEvent event = buildEvent("good first issue");

        // When
        handler.handle(event, registry, CONFIG);

        // Then
        verifyNoInteractions(repo, issue);
    }

    @Test
    void skipsDuplicateNotification() throws IOException {
        // Given
        final IssuesEvent event = buildEvent("good first issue candidate");
        when(gitHub.getRepository("owner/repo")).thenReturn(repo);
        when(repo.getIssue(42)).thenReturn(issue);

        try (final MockedStatic<CommentMarkerChecker> mc = mockStatic(CommentMarkerChecker.class)) {
            mc.when(() -> CommentMarkerChecker.hasMarker(eq(issue), eq("<!-- GFI Candidate Notification -->"))).thenReturn(true);

            // When
            handler.handle(event, registry, CONFIG);

            // Then
            verify(issue, never()).comment(any());
        }
    }

    @Test
    void skipsWhenNoTeamConfigured() throws IOException {
        // Given
        final RepoConfig configWithoutTeam = configWithTeam("");
        final IssuesEvent event = buildEvent("good first issue candidate");

        // When
        handler.handle(event, registry, configWithoutTeam);

        // Then
        verifyNoInteractions(gitHub, repo, issue);
    }

    @Test
    void isInactiveWhenFeatureDisabled() {
        // Given
        final RepoConfig disabledConfig = configWithFeatureDisabled();

        // When
        final boolean active = handler.isActive(disabledConfig);

        // Then
        assertFalse(active);
    }

    private static IssuesEvent buildEvent(final String labelName) {
        final User sender = new User(1, "alice", "User", null, null, false);
        final Label label = new Label(1, labelName, null, null);
        final Issue modelIssue = new Issue(1, 42, "Improve docs", null, "open", null, null,
                null, List.of(), List.of(), false, null, false, null, null, null, null);
        final Repository repository = new Repository(1, "repo", "owner/repo", null, false, null, null, null);
        final Installation installation = new Installation(1, 1);
        return new IssuesEvent(GitHubAction.LABELED, modelIssue, null, label, repository, sender, installation);
    }

    private static RepoConfig configWithTeam(final String teamMention) {
        final DefaultRepoConfig defaults = DefaultRepoConfig.allDefaults();
        return new DefaultRepoConfig(
                "",
                defaults.labels(),
                defaults.assignmentLimits(),
                defaults.guards(),
                defaults.features(),
                defaults.markers(),
                defaults.commands(),
                defaults.paths(),
                new TeamsConfig(teamMention),
                defaults.scheduled()
        );
    }

    private static RepoConfig configWithFeatureDisabled() {
        final DefaultRepoConfig defaults = DefaultRepoConfig.allDefaults();
        final FeaturesConfig features = new FeaturesConfig(
                true, true, true,
                true, true, true, true, false,
                true, true, true, true, true, true);
        return new DefaultRepoConfig(
                "",
                defaults.labels(),
                defaults.assignmentLimits(),
                defaults.guards(),
                features,
                defaults.markers(),
                defaults.commands(),
                defaults.paths(),
                defaults.teams(),
                defaults.scheduled()
        );
    }
}
