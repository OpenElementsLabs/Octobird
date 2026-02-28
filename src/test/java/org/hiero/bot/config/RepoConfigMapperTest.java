package org.hiero.bot.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RepoConfigMapperTest {

    @Test
    void emptyMapReturnsAllDefaults() {
        // Given
        final Map<String, Object> raw = Map.of();

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals(LabelsConfig.defaults(), config.labels());
        assertEquals(AssignmentLimitsConfig.defaults(), config.assignmentLimits());
        assertEquals(GuardsConfig.defaults(), config.guards());
        assertEquals(FeaturesConfig.defaults(), config.features());
        assertEquals(MarkersConfig.defaults(), config.markers());
        assertEquals(CommandsConfig.defaults(), config.commands());
        assertEquals(PathsConfig.defaults(), config.paths());
        assertEquals(CodeRabbitConfig.defaults(), config.codeRabbit());
        assertEquals(TeamsConfig.defaults(), config.teams());
        assertEquals(ScheduledConfig.defaults(), config.scheduled());
    }

    @Test
    void partialLabelsConfigMergesWithDefaults() {
        // Given
        final Map<String, Object> raw = Map.of(
                "labels", Map.of("beginner", "Beginner Task")
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals("Good First Issue", config.labels().goodFirstIssue());
        assertEquals("Beginner Task", config.labels().beginner());
        assertEquals("intermediate", config.labels().intermediate());
        assertEquals("advanced", config.labels().advanced());
        assertEquals("p0", config.labels().p0());
        assertEquals("good first issue candidate", config.labels().gfiCandidate());
    }

    @Test
    void customAssignmentLimits() {
        // Given
        final Map<String, Object> raw = Map.of(
                "assignment-limits", Map.of(
                        "normal-user-max", 5,
                        "spam-user-max", 2
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals(5, config.assignmentLimits().normalUserMax());
        assertEquals(2, config.assignmentLimits().spamUserMax());
    }

    @Test
    void customGuards() {
        // Given
        final Map<String, Object> raw = Map.of(
                "guards", Map.of(
                        "required-gfi-count-for-beginner", 3,
                        "required-intermediate-count-for-advanced", 2
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals(3, config.guards().requiredGfiCountForBeginner());
        assertEquals(0, config.guards().requiredBeginnerCountForIntermediate());
        assertEquals(2, config.guards().requiredIntermediateCountForAdvanced());
    }

    @Test
    void disableFeatures() {
        // Given
        final Map<String, Object> raw = Map.of(
                "features", Map.of(
                        "unassign-command", false,
                        "coderabbit-plan-trigger", false
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertFalse(config.features().unassignCommand());
        assertTrue(config.features().workingCommand());
        assertFalse(config.features().codeRabbitPlanTrigger());
    }

    @Test
    void customPaths() {
        // Given
        final Map<String, Object> raw = Map.of(
                "paths", Map.of(
                        "spam-list", "custom/spam.txt",
                        "mentor-roster", "custom/mentors.json"
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals("custom/spam.txt", config.paths().spamList());
        assertEquals("custom/mentors.json", config.paths().mentorRoster());
    }

    @Test
    void customCodeRabbitTriggerLabels() {
        // Given
        final Map<String, Object> raw = Map.of(
                "coderabbit", Map.of(
                        "trigger-labels", List.of("beginner", "expert")
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals(Set.of("beginner", "expert"), config.codeRabbit().triggerLabels());
    }

    @Test
    void nonMapValuesAreIgnored() {
        // Given
        final Map<String, Object> raw = Map.of(
                "labels", "not-a-map",
                "assignment-limits", 42
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals(LabelsConfig.defaults(), config.labels());
        assertEquals(AssignmentLimitsConfig.defaults(), config.assignmentLimits());
    }

    @Test
    void customCommands() {
        // Given
        final Map<String, Object> raw = Map.of(
                "commands", Map.of(
                        "assign-pattern", "/grab\\b"
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals("/grab\\b", config.commands().assignPattern());
        assertEquals("(^|\\s)/unassign(\\s|$)", config.commands().unassignPattern());
    }

    @Test
    void fullCustomConfig() {
        // Given
        final Map<String, Object> raw = Map.of(
                "labels", Map.of(
                        "good-first-issue", "GFI",
                        "beginner", "starter",
                        "intermediate", "mid",
                        "advanced", "expert"
                ),
                "assignment-limits", Map.of(
                        "normal-user-max", 3,
                        "spam-user-max", 0
                ),
                "features", Map.of(
                        "assign-command", false
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals("GFI", config.labels().goodFirstIssue());
        assertEquals("starter", config.labels().beginner());
        assertEquals(3, config.assignmentLimits().normalUserMax());
        assertEquals(0, config.assignmentLimits().spamUserMax());
        assertFalse(config.features().assignCommand());
    }

    @Test
    void customP0AndGfiCandidateLabels() {
        // Given
        final Map<String, Object> raw = Map.of(
                "labels", Map.of(
                        "p0", "critical",
                        "gfi-candidate", "gfi-review"
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals("critical", config.labels().p0());
        assertEquals("gfi-review", config.labels().gfiCandidate());
        assertEquals("Good First Issue", config.labels().goodFirstIssue());
    }

    @Test
    void customTeamsConfig() {
        // Given
        final Map<String, Object> raw = Map.of(
                "teams", Map.of(
                        "p0-teams", List.of("@org/maintainers", "@org/triage"),
                        "gfi-candidate-team", "@org/gfi-support"
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals(List.of("@org/maintainers", "@org/triage"), config.teams().p0Teams());
        assertEquals("@org/gfi-support", config.teams().gfiCandidateTeam());
    }

    @Test
    void emptyTeamsConfigReturnsDefaults() {
        // Given
        final Map<String, Object> raw = Map.of(
                "teams", Map.of()
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals(List.of(), config.teams().p0Teams());
        assertEquals("", config.teams().gfiCandidateTeam());
    }

    @Test
    void disablePhase4Features() {
        // Given
        final Map<String, Object> raw = Map.of(
                "features", Map.of(
                        "p0-issue-alarm", false,
                        "gfi-candidate-notification", false
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertFalse(config.features().p0IssueAlarm());
        assertFalse(config.features().gfiCandidateNotification());
        assertTrue(config.features().workflowFailureNotification());
    }

    @Test
    void customScheduledThresholds() {
        // Given
        final Map<String, Object> raw = Map.of(
                "scheduled", Map.of(
                        "inactivity-days", 30,
                        "issue-reminder-days", 14,
                        "pr-inactivity-days", 7,
                        "linked-issue-enforcer-days", 5,
                        "require-author-assigned", false
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals(30, config.scheduled().inactivityDays());
        assertEquals(14, config.scheduled().issueReminderDays());
        assertEquals(7, config.scheduled().prInactivityDays());
        assertEquals(5, config.scheduled().linkedIssueEnforcerDays());
        assertFalse(config.scheduled().requireAuthorAssigned());
    }

    @Test
    void customCommunityCallConfig() {
        // Given
        final Map<String, Object> raw = Map.of(
                "scheduled", Map.of(
                        "community-call", Map.of(
                                "anchor-date", "2024-01-03",
                                "meeting-link", "https://zoom.us/j/123",
                                "calendar-link", "https://calendar.google.com/123",
                                "cancelled-dates", List.of("2024-01-17"),
                                "excluded-authors", List.of("bot-user", "admin")
                        )
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals("2024-01-03", config.scheduled().communityCall().anchorDate());
        assertEquals("https://zoom.us/j/123", config.scheduled().communityCall().meetingLink());
        assertEquals("https://calendar.google.com/123", config.scheduled().communityCall().calendarLink());
        assertEquals(List.of("2024-01-17"), config.scheduled().communityCall().cancelledDates());
        assertEquals(List.of("bot-user", "admin"), config.scheduled().communityCall().excludedAuthors());
    }

    @Test
    void customOfficeHoursConfig() {
        // Given
        final Map<String, Object> raw = Map.of(
                "scheduled", Map.of(
                        "office-hours", Map.of(
                                "anchor-date", "2024-01-10",
                                "meeting-link", "https://zoom.us/j/456"
                        )
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertEquals("2024-01-10", config.scheduled().officeHours().anchorDate());
        assertEquals("https://zoom.us/j/456", config.scheduled().officeHours().meetingLink());
        assertEquals("", config.scheduled().officeHours().calendarLink());
        assertEquals(List.of(), config.scheduled().officeHours().cancelledDates());
    }

    @Test
    void disablePhase5Features() {
        // Given
        final Map<String, Object> raw = Map.of(
                "features", Map.of(
                        "inactivity-unassign", false,
                        "issue-reminder-no-pr", false,
                        "pr-inactivity-reminder", false,
                        "linked-issue-enforcer", false,
                        "community-call-reminder", false,
                        "office-hours-reminder", false
                )
        );

        // When
        final RepoConfig config = RepoConfigMapper.fromMap("", raw);

        // Then
        assertFalse(config.features().inactivityUnassign());
        assertFalse(config.features().issueReminderNoPr());
        assertFalse(config.features().prInactivityReminder());
        assertFalse(config.features().linkedIssueEnforcer());
        assertFalse(config.features().communityCallReminder());
        assertFalse(config.features().officeHoursReminder());
        assertTrue(config.features().p0IssueAlarm());
    }
}