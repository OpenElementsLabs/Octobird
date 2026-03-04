package com.openelements.octobird.persistence.mapper;

import com.openelements.octobird.config.*;
import com.openelements.octobird.persistence.entity.RepoConfigEntity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Bidirectional mapper between {@link RepoConfigEntity} and the typed config records.
 * Null entity fields fall back to defaults from the corresponding {@code *Config.defaults()}.
 */
public final class EntityRecordMapper {

    private EntityRecordMapper() {
    }

    /**
     * Converts a {@link RepoConfigEntity} to a {@link DefaultRepoConfig}. Null entity fields
     * fall back to the corresponding default values.
     *
     * @param entity the entity to convert
     * @return the populated config record
     */
    public static DefaultRepoConfig toRepoConfig(final RepoConfigEntity entity) {
        return new DefaultRepoConfig(
                entity.getRepoId(),
                entity.getRepoFullName(),
                mapLabels(entity),
                mapAssignmentLimits(entity),
                mapGuards(entity),
                mapFeatures(entity),
                mapMarkers(entity),
                mapCommands(entity),
                mapTeams(entity),
                mapScheduled(entity)
        );
    }

    /**
     * Updates a {@link RepoConfigEntity} from a {@link RepoConfig}. Does not change the entity ID
     * or repo full name.
     *
     * @param entity the entity to update
     * @param config the config to read values from
     */
    public static void updateEntity(final RepoConfigEntity entity, final RepoConfig config) {
        // labels
        final LabelsConfig labels = config.labels();
        entity.setLabelGfi(labels.labelFor(IssueLevel.GOOD_FIRST_ISSUE));
        entity.setLabelBeginner(labels.labelFor(IssueLevel.BEGINNER));
        entity.setLabelIntermediate(labels.labelFor(IssueLevel.INTERMEDIATE));
        entity.setLabelAdvanced(labels.labelFor(IssueLevel.ADVANCED));
        entity.setLabelGfiCandidate(labels.gfiCandidate());

        // assignment limits
        entity.setNormalUserMax(config.assignmentLimits().normalUserMax());
        entity.setSpamUserMax(config.assignmentLimits().spamUserMax());

        // guards
        final GuardsConfig guards = config.guards();
        entity.setGuardGfi(guards.requiredCounts().get(IssueLevel.GOOD_FIRST_ISSUE));
        entity.setGuardBeginner(guards.requiredCounts().get(IssueLevel.BEGINNER));
        entity.setGuardIntermediate(guards.requiredCounts().get(IssueLevel.INTERMEDIATE));
        entity.setGuardAdvanced(guards.requiredCounts().get(IssueLevel.ADVANCED));

        // features
        final FeaturesConfig features = config.features();
        entity.setFeatUnassignCommand(features.unassignCommand());
        entity.setFeatAssignCommand(features.assignCommand());
        entity.setFeatMissingLinkedIssue(features.missingLinkedIssue());
        entity.setFeatVerifiedCommits(features.verifiedCommits());
        entity.setFeatMergeConflict(features.mergeConflict());
        entity.setFeatNextIssueRecommendation(features.nextIssueRecommendation());
        entity.setFeatWorkflowFailureNotification(features.workflowFailureNotification());
        entity.setFeatGfiCandidateNotification(features.gfiCandidateNotification());
        entity.setFeatInactivityUnassign(features.inactivityUnassign());
        entity.setFeatIssueReminderNoPr(features.issueReminderNoPr());
        entity.setFeatPrInactivityReminder(features.prInactivityReminder());
        entity.setFeatLinkedIssueEnforcer(features.linkedIssueEnforcer());
        entity.setFeatCommunityCallReminder(features.communityCallReminder());
        entity.setFeatOfficeHoursReminder(features.officeHoursReminder());

        // markers
        final MarkersConfig markers = config.markers();
        entity.setMarkerUnassignPrefix(markers.unassignPrefix());
        entity.setMarkerGfiReminder(markers.gfiReminder());
        entity.setMarkerBeginnerReminder(markers.beginnerReminder());
        entity.setMarkerBeginnerGfiGuard(markers.beginnerGfiGuard());
        entity.setMarkerMentorAssignment(markers.mentorAssignment());
        entity.setMarkerIntermediateGuard(markers.intermediateGuard());
        entity.setMarkerAdvancedGuard(markers.advancedGuard());
        entity.setMarkerMissingLinkedIssue(markers.missingLinkedIssue());
        entity.setMarkerVerifiedCommits(markers.verifiedCommits());
        entity.setMarkerMergeConflict(markers.mergeConflict());
        entity.setMarkerNextIssueRecommendation(markers.nextIssueRecommendation());
        entity.setMarkerWorkflowFailureNotification(markers.workflowFailureNotification());
        entity.setMarkerGfiCandidateNotification(markers.gfiCandidateNotification());
        entity.setMarkerInactivityUnassign(markers.inactivityUnassign());
        entity.setMarkerIssueReminderNoPr(markers.issueReminderNoPr());
        entity.setMarkerPrInactivityReminder(markers.prInactivityReminder());
        entity.setMarkerLinkedIssueEnforcer(markers.linkedIssueEnforcer());
        entity.setMarkerCommunityCallReminder(markers.communityCallReminder());
        entity.setMarkerOfficeHoursReminder(markers.officeHoursReminder());

        // commands
        entity.setCmdAssignPattern(config.commands().assignPattern());
        entity.setCmdUnassignPattern(config.commands().unassignPattern());
        entity.setCmdWorkingPattern(config.commands().workingPattern());

        // teams
        entity.setTeamGfiCandidate(config.teams().gfiCandidateTeam());

        // scheduled
        final ScheduledConfig scheduled = config.scheduled();
        entity.setSchedInactivityDays(scheduled.inactivityDays());
        entity.setSchedIssueReminderDays(scheduled.issueReminderDays());
        entity.setSchedPrInactivityDays(scheduled.prInactivityDays());
        entity.setSchedLinkedIssueEnforcerDays(scheduled.linkedIssueEnforcerDays());
        entity.setSchedRequireAuthorAssigned(scheduled.requireAuthorAssigned());

        // community call
        final CommunityCallConfig cc = scheduled.communityCall();
        entity.setSchedCcAnchorDate(cc.anchorDate());
        entity.setSchedCcMeetingLink(cc.meetingLink());
        entity.setSchedCcCalendarLink(cc.calendarLink());
        entity.setSchedCcCancelledDates(new ArrayList<>(cc.cancelledDates()));
        entity.setSchedCcExcludedAuthors(new ArrayList<>(cc.excludedAuthors()));

        // office hours
        final OfficeHoursConfig oh = scheduled.officeHours();
        entity.setSchedOhAnchorDate(oh.anchorDate());
        entity.setSchedOhMeetingLink(oh.meetingLink());
        entity.setSchedOhCalendarLink(oh.calendarLink());
        entity.setSchedOhCancelledDates(new ArrayList<>(oh.cancelledDates()));
        entity.setSchedOhExcludedAuthors(new ArrayList<>(oh.excludedAuthors()));
    }

    private static LabelsConfig mapLabels(final RepoConfigEntity e) {
        final LabelsConfig defaults = LabelsConfig.defaults();
        final Map<IssueLevel, String> labels = new EnumMap<>(IssueLevel.class);
        labels.put(IssueLevel.GOOD_FIRST_ISSUE, or(e.getLabelGfi(), defaults.labelFor(IssueLevel.GOOD_FIRST_ISSUE)));
        labels.put(IssueLevel.BEGINNER, or(e.getLabelBeginner(), defaults.labelFor(IssueLevel.BEGINNER)));
        labels.put(IssueLevel.INTERMEDIATE, or(e.getLabelIntermediate(), defaults.labelFor(IssueLevel.INTERMEDIATE)));
        labels.put(IssueLevel.ADVANCED, or(e.getLabelAdvanced(), defaults.labelFor(IssueLevel.ADVANCED)));
        return new LabelsConfig(labels, or(e.getLabelGfiCandidate(), defaults.gfiCandidate()));
    }

    private static AssignmentLimitsConfig mapAssignmentLimits(final RepoConfigEntity e) {
        final AssignmentLimitsConfig defaults = AssignmentLimitsConfig.defaults();
        return new AssignmentLimitsConfig(
                or(e.getNormalUserMax(), defaults.normalUserMax()),
                or(e.getSpamUserMax(), defaults.spamUserMax())
        );
    }

    private static GuardsConfig mapGuards(final RepoConfigEntity e) {
        final GuardsConfig defaults = GuardsConfig.defaults();
        final Map<IssueLevel, Integer> counts = new EnumMap<>(IssueLevel.class);
        counts.put(IssueLevel.GOOD_FIRST_ISSUE, or(e.getGuardGfi(), defaults.requiredCounts().get(IssueLevel.GOOD_FIRST_ISSUE)));
        counts.put(IssueLevel.BEGINNER, or(e.getGuardBeginner(), defaults.requiredCounts().get(IssueLevel.BEGINNER)));
        counts.put(IssueLevel.INTERMEDIATE, or(e.getGuardIntermediate(), defaults.requiredCounts().get(IssueLevel.INTERMEDIATE)));
        counts.put(IssueLevel.ADVANCED, or(e.getGuardAdvanced(), defaults.requiredCounts().get(IssueLevel.ADVANCED)));
        return new GuardsConfig(counts);
    }

    private static FeaturesConfig mapFeatures(final RepoConfigEntity e) {
        final FeaturesConfig d = FeaturesConfig.defaults();
        return new FeaturesConfig(
                or(e.getFeatUnassignCommand(), d.unassignCommand()),
                or(e.getFeatAssignCommand(), d.assignCommand()),
                or(e.getFeatMissingLinkedIssue(), d.missingLinkedIssue()),
                or(e.getFeatVerifiedCommits(), d.verifiedCommits()),
                or(e.getFeatMergeConflict(), d.mergeConflict()),
                or(e.getFeatNextIssueRecommendation(), d.nextIssueRecommendation()),
                or(e.getFeatWorkflowFailureNotification(), d.workflowFailureNotification()),
                or(e.getFeatGfiCandidateNotification(), d.gfiCandidateNotification()),
                or(e.getFeatInactivityUnassign(), d.inactivityUnassign()),
                or(e.getFeatIssueReminderNoPr(), d.issueReminderNoPr()),
                or(e.getFeatPrInactivityReminder(), d.prInactivityReminder()),
                or(e.getFeatLinkedIssueEnforcer(), d.linkedIssueEnforcer()),
                or(e.getFeatCommunityCallReminder(), d.communityCallReminder()),
                or(e.getFeatOfficeHoursReminder(), d.officeHoursReminder())
        );
    }

    private static MarkersConfig mapMarkers(final RepoConfigEntity e) {
        final MarkersConfig d = MarkersConfig.defaults();
        return new MarkersConfig(
                or(e.getMarkerUnassignPrefix(), d.unassignPrefix()),
                or(e.getMarkerGfiReminder(), d.gfiReminder()),
                or(e.getMarkerBeginnerReminder(), d.beginnerReminder()),
                or(e.getMarkerBeginnerGfiGuard(), d.beginnerGfiGuard()),
                or(e.getMarkerMentorAssignment(), d.mentorAssignment()),
                or(e.getMarkerIntermediateGuard(), d.intermediateGuard()),
                or(e.getMarkerAdvancedGuard(), d.advancedGuard()),
                or(e.getMarkerMissingLinkedIssue(), d.missingLinkedIssue()),
                or(e.getMarkerVerifiedCommits(), d.verifiedCommits()),
                or(e.getMarkerMergeConflict(), d.mergeConflict()),
                or(e.getMarkerNextIssueRecommendation(), d.nextIssueRecommendation()),
                or(e.getMarkerWorkflowFailureNotification(), d.workflowFailureNotification()),
                or(e.getMarkerGfiCandidateNotification(), d.gfiCandidateNotification()),
                or(e.getMarkerInactivityUnassign(), d.inactivityUnassign()),
                or(e.getMarkerIssueReminderNoPr(), d.issueReminderNoPr()),
                or(e.getMarkerPrInactivityReminder(), d.prInactivityReminder()),
                or(e.getMarkerLinkedIssueEnforcer(), d.linkedIssueEnforcer()),
                or(e.getMarkerCommunityCallReminder(), d.communityCallReminder()),
                or(e.getMarkerOfficeHoursReminder(), d.officeHoursReminder())
        );
    }

    private static CommandsConfig mapCommands(final RepoConfigEntity e) {
        final CommandsConfig d = CommandsConfig.defaults();
        return new CommandsConfig(
                or(e.getCmdAssignPattern(), d.assignPattern()),
                or(e.getCmdUnassignPattern(), d.unassignPattern()),
                or(e.getCmdWorkingPattern(), d.workingPattern())
        );
    }

    private static TeamsConfig mapTeams(final RepoConfigEntity e) {
        final TeamsConfig d = TeamsConfig.defaults();
        return new TeamsConfig(or(e.getTeamGfiCandidate(), d.gfiCandidateTeam()));
    }

    private static ScheduledConfig mapScheduled(final RepoConfigEntity e) {
        final ScheduledConfig d = ScheduledConfig.defaults();
        final CommunityCallConfig cc = new CommunityCallConfig(
                or(e.getSchedCcAnchorDate(), d.communityCall().anchorDate()),
                or(e.getSchedCcMeetingLink(), d.communityCall().meetingLink()),
                or(e.getSchedCcCalendarLink(), d.communityCall().calendarLink()),
                e.getSchedCcCancelledDates() != null ? List.copyOf(e.getSchedCcCancelledDates()) : d.communityCall().cancelledDates(),
                e.getSchedCcExcludedAuthors() != null ? List.copyOf(e.getSchedCcExcludedAuthors()) : d.communityCall().excludedAuthors()
        );
        final OfficeHoursConfig oh = new OfficeHoursConfig(
                or(e.getSchedOhAnchorDate(), d.officeHours().anchorDate()),
                or(e.getSchedOhMeetingLink(), d.officeHours().meetingLink()),
                or(e.getSchedOhCalendarLink(), d.officeHours().calendarLink()),
                e.getSchedOhCancelledDates() != null ? List.copyOf(e.getSchedOhCancelledDates()) : d.officeHours().cancelledDates(),
                e.getSchedOhExcludedAuthors() != null ? List.copyOf(e.getSchedOhExcludedAuthors()) : d.officeHours().excludedAuthors()
        );
        return new ScheduledConfig(
                or(e.getSchedInactivityDays(), d.inactivityDays()),
                or(e.getSchedIssueReminderDays(), d.issueReminderDays()),
                or(e.getSchedPrInactivityDays(), d.prInactivityDays()),
                or(e.getSchedLinkedIssueEnforcerDays(), d.linkedIssueEnforcerDays()),
                or(e.getSchedRequireAuthorAssigned(), d.requireAuthorAssigned()),
                cc, oh
        );
    }

    private static String or(final String value, final String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static int or(final Integer value, final int defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static boolean or(final Boolean value, final boolean defaultValue) {
        return value != null ? value : defaultValue;
    }
}
