package org.hiero.bot.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maps a raw YAML configuration map to a typed {@link RepoConfig}, merging partial values with
 * defaults. Missing keys fall back to the corresponding default value.
 */
public final class RepoConfigMapper {

    private RepoConfigMapper() {
    }

    /**
     * Creates a {@link RepoConfig} from a raw map parsed from YAML, merging with defaults.
     *
     * @param repoFullName full repository name in {@code owner/repo} format
     * @param raw          the raw configuration map (may be empty)
     * @return a fully-populated {@link RepoConfig}
     */
    public static RepoConfig fromMap(final String repoFullName, final Map<String, Object> raw) {
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        Objects.requireNonNull(raw, "raw must not be null");

        final LabelsConfig labels = mapLabels(asMap(raw.get("labels")));
        final AssignmentLimitsConfig limits = mapAssignmentLimits(asMap(raw.get("assignment-limits")));
        final GuardsConfig guards = mapGuards(asMap(raw.get("guards")));
        final FeaturesConfig features = mapFeatures(asMap(raw.get("features")));
        final MarkersConfig markers = mapMarkers(asMap(raw.get("markers")));
        final CommandsConfig commands = mapCommands(asMap(raw.get("commands")));
        final TeamsConfig teams = mapTeams(asMap(raw.get("teams")));
        final ScheduledConfig scheduled = mapScheduled(asMap(raw.get("scheduled")));

        return new DefaultRepoConfig(0, repoFullName, labels, limits, guards, features, markers, commands,
                teams, scheduled);
    }

    private static LabelsConfig mapLabels(final Map<String, Object> m) {
        final LabelsConfig d = LabelsConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        final Map<IssueLevel, String> levelLabels = new EnumMap<>(IssueLevel.class);
        levelLabels.put(IssueLevel.GOOD_FIRST_ISSUE,
                stringOr(m.get("good-first-issue"), d.labelFor(IssueLevel.GOOD_FIRST_ISSUE)));
        levelLabels.put(IssueLevel.BEGINNER,
                stringOr(m.get("beginner"), d.labelFor(IssueLevel.BEGINNER)));
        levelLabels.put(IssueLevel.INTERMEDIATE,
                stringOr(m.get("intermediate"), d.labelFor(IssueLevel.INTERMEDIATE)));
        levelLabels.put(IssueLevel.ADVANCED,
                stringOr(m.get("advanced"), d.labelFor(IssueLevel.ADVANCED)));
        return new LabelsConfig(levelLabels, stringOr(m.get("gfi-candidate"), d.gfiCandidate()));
    }

    private static AssignmentLimitsConfig mapAssignmentLimits(final Map<String, Object> m) {
        final AssignmentLimitsConfig d = AssignmentLimitsConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        return new AssignmentLimitsConfig(
                intOr(m.get("normal-user-max"), d.normalUserMax()),
                intOr(m.get("spam-user-max"), d.spamUserMax())
        );
    }

    private static GuardsConfig mapGuards(final Map<String, Object> m) {
        final GuardsConfig d = GuardsConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        final Map<IssueLevel, Integer> counts = new EnumMap<>(IssueLevel.class);
        counts.put(IssueLevel.GOOD_FIRST_ISSUE, 0);
        counts.put(IssueLevel.BEGINNER,
                intOr(m.get("required-gfi-count-for-beginner"), d.requiredCountFor(IssueLevel.BEGINNER)));
        counts.put(IssueLevel.INTERMEDIATE,
                intOr(m.get("required-beginner-count-for-intermediate"), d.requiredCountFor(IssueLevel.INTERMEDIATE)));
        counts.put(IssueLevel.ADVANCED,
                intOr(m.get("required-intermediate-count-for-advanced"), d.requiredCountFor(IssueLevel.ADVANCED)));
        return new GuardsConfig(counts);
    }

    private static FeaturesConfig mapFeatures(final Map<String, Object> m) {
        final FeaturesConfig d = FeaturesConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        return new FeaturesConfig(
                boolOr(m.get("unassign-command"), d.unassignCommand()),
                boolOr(m.get("assign-command"), d.assignCommand()),
                boolOr(m.get("missing-linked-issue"), d.missingLinkedIssue()),
                boolOr(m.get("verified-commits"), d.verifiedCommits()),
                boolOr(m.get("merge-conflict"), d.mergeConflict()),
                boolOr(m.get("next-issue-recommendation"), d.nextIssueRecommendation()),
                boolOr(m.get("workflow-failure-notification"), d.workflowFailureNotification()),
                boolOr(m.get("gfi-candidate-notification"), d.gfiCandidateNotification()),
                boolOr(m.get("inactivity-unassign"), d.inactivityUnassign()),
                boolOr(m.get("issue-reminder-no-pr"), d.issueReminderNoPr()),
                boolOr(m.get("pr-inactivity-reminder"), d.prInactivityReminder()),
                boolOr(m.get("linked-issue-enforcer"), d.linkedIssueEnforcer()),
                boolOr(m.get("community-call-reminder"), d.communityCallReminder()),
                boolOr(m.get("office-hours-reminder"), d.officeHoursReminder())
        );
    }

    private static MarkersConfig mapMarkers(final Map<String, Object> m) {
        final MarkersConfig d = MarkersConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        return new MarkersConfig(
                stringOr(m.get("unassign-prefix"), d.unassignPrefix()),
                stringOr(m.get("gfi-reminder"), d.gfiReminder()),
                stringOr(m.get("beginner-reminder"), d.beginnerReminder()),
                stringOr(m.get("beginner-gfi-guard"), d.beginnerGfiGuard()),
                stringOr(m.get("mentor-assignment"), d.mentorAssignment()),
                stringOr(m.get("intermediate-guard"), d.intermediateGuard()),
                stringOr(m.get("advanced-guard"), d.advancedGuard()),
                stringOr(m.get("missing-linked-issue"), d.missingLinkedIssue()),
                stringOr(m.get("verified-commits"), d.verifiedCommits()),
                stringOr(m.get("merge-conflict"), d.mergeConflict()),
                stringOr(m.get("next-issue-recommendation"), d.nextIssueRecommendation()),
                stringOr(m.get("workflow-failure-notification"), d.workflowFailureNotification()),
                stringOr(m.get("gfi-candidate-notification"), d.gfiCandidateNotification()),
                stringOr(m.get("inactivity-unassign"), d.inactivityUnassign()),
                stringOr(m.get("issue-reminder-no-pr"), d.issueReminderNoPr()),
                stringOr(m.get("pr-inactivity-reminder"), d.prInactivityReminder()),
                stringOr(m.get("linked-issue-enforcer"), d.linkedIssueEnforcer()),
                stringOr(m.get("community-call-reminder"), d.communityCallReminder()),
                stringOr(m.get("office-hours-reminder"), d.officeHoursReminder())
        );
    }

    private static CommandsConfig mapCommands(final Map<String, Object> m) {
        final CommandsConfig d = CommandsConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        return new CommandsConfig(
                stringOr(m.get("assign-pattern"), d.assignPattern()),
                stringOr(m.get("unassign-pattern"), d.unassignPattern()),
                stringOr(m.get("working-pattern"), d.workingPattern())
        );
    }

    private static TeamsConfig mapTeams(final Map<String, Object> m) {
        final TeamsConfig d = TeamsConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        return new TeamsConfig(stringOr(m.get("gfi-candidate-team"), d.gfiCandidateTeam()));
    }

    private static ScheduledConfig mapScheduled(final Map<String, Object> m) {
        final ScheduledConfig d = ScheduledConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        return new ScheduledConfig(
                intOr(m.get("inactivity-days"), d.inactivityDays()),
                intOr(m.get("issue-reminder-days"), d.issueReminderDays()),
                intOr(m.get("pr-inactivity-days"), d.prInactivityDays()),
                intOr(m.get("linked-issue-enforcer-days"), d.linkedIssueEnforcerDays()),
                boolOr(m.get("require-author-assigned"), d.requireAuthorAssigned()),
                mapCommunityCall(asMap(m.get("community-call"))),
                mapOfficeHours(asMap(m.get("office-hours")))
        );
    }

    private static CommunityCallConfig mapCommunityCall(final Map<String, Object> m) {
        final CommunityCallConfig d = CommunityCallConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        return new CommunityCallConfig(
                stringOr(m.get("anchor-date"), d.anchorDate()),
                stringOr(m.get("meeting-link"), d.meetingLink()),
                stringOr(m.get("calendar-link"), d.calendarLink()),
                stringList(m.get("cancelled-dates"), d.cancelledDates()),
                stringList(m.get("excluded-authors"), d.excludedAuthors())
        );
    }

    private static OfficeHoursConfig mapOfficeHours(final Map<String, Object> m) {
        final OfficeHoursConfig d = OfficeHoursConfig.defaults();
        if (m.isEmpty()) {
            return d;
        }
        return new OfficeHoursConfig(
                stringOr(m.get("anchor-date"), d.anchorDate()),
                stringOr(m.get("meeting-link"), d.meetingLink()),
                stringOr(m.get("calendar-link"), d.calendarLink()),
                stringList(m.get("cancelled-dates"), d.cancelledDates()),
                stringList(m.get("excluded-authors"), d.excludedAuthors())
        );
    }

    private static List<String> stringList(final Object value, final List<String> defaultValue) {
        if (value instanceof Collection<?> collection) {
            final List<String> result = new ArrayList<>();
            for (final Object item : collection) {
                result.add(String.valueOf(item));
            }
            return List.copyOf(result);
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(final Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String stringOr(final Object value, final String defaultValue) {
        return value instanceof String s ? s : defaultValue;
    }

    private static int intOr(final Object value, final int defaultValue) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    private static boolean boolOr(final Object value, final boolean defaultValue) {
        if (value instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }
}
