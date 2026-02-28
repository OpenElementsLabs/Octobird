package org.hiero.bot.config;

/**
 * HTML comment markers used to prevent duplicate bot actions on issues.
 *
 * @param unassignPrefix                 marker prefix for unassign confirmations
 * @param gfiReminder                    marker for GFI assign reminders
 * @param beginnerReminder               marker for beginner assign reminders
 * @param beginnerGfiGuard               marker for beginner GFI prerequisite guard
 * @param mentorAssignment               marker for mentor assignment
 * @param intermediateGuard              marker for intermediate assignment guard
 * @param advancedGuard                  marker for advanced assignment guard
 * @param missingLinkedIssue             marker for missing linked issue reminder
 * @param verifiedCommits                marker for verified commits check
 * @param mergeConflict                  marker for merge conflict detection
 * @param nextIssueRecommendation        marker for next issue recommendation
 * @param workflowFailureNotification    marker for workflow failure notification
 * @param gfiCandidateNotification       marker for GFI candidate team notification
 * @param inactivityUnassign             marker for inactivity unassign comment
 * @param issueReminderNoPr              marker for issue reminder (no PR) comment
 * @param prInactivityReminder           marker for PR inactivity reminder comment
 * @param linkedIssueEnforcer            marker for linked-issue enforcer close comment
 * @param communityCallReminder          marker for community-call reminder comment
 * @param officeHoursReminder            marker for office-hours reminder comment
 */
public record MarkersConfig(String unassignPrefix,
                            String gfiReminder,
                            String beginnerReminder,
                            String beginnerGfiGuard,
                            String mentorAssignment,
                            String intermediateGuard,
                            String advancedGuard,
                            String missingLinkedIssue,
                            String verifiedCommits,
                            String mergeConflict,
                            String nextIssueRecommendation,
                            String workflowFailureNotification,
                            String gfiCandidateNotification,
                            String inactivityUnassign,
                            String issueReminderNoPr,
                            String prInactivityReminder,
                            String linkedIssueEnforcer,
                            String communityCallReminder,
                            String officeHoursReminder) {

    /**
     * Returns the guard marker prefix for the given issue level. This marker is used to
     * prevent duplicate prerequisite-check comments.
     *
     * @param level the target issue level (must be {@link IssueLevel#BEGINNER} or higher)
     * @return the guard marker prefix
     * @throws IllegalArgumentException if level is {@link IssueLevel#GOOD_FIRST_ISSUE}
     */
    public String guardMarkerFor(final IssueLevel level) {
        return switch (level) {
            case BEGINNER -> beginnerGfiGuard;
            case INTERMEDIATE -> intermediateGuard;
            case ADVANCED -> advancedGuard;
            case GOOD_FIRST_ISSUE -> throw new IllegalArgumentException(
                    "GOOD_FIRST_ISSUE has no guard marker");
        };
    }

    /**
     * Returns the default marker configuration.
     *
     * @return a {@code MarkersConfig} with standard HTML comment markers
     */
    public static MarkersConfig defaults() {
        return new MarkersConfig(
                "<!-- unassign-requested:",
                "<!-- GFI assign reminder -->",
                "<!-- beginner assign reminder -->",
                "<!-- beginner-gfi-guard -->",
                "<!-- Mentor Assignment Bot -->",
                "<!-- Intermediate Issue Guard -->",
                "<!-- advanced-check:unqualified -->",
                "<!-- LinkBot Missing Issue -->",
                "<!-- commit-verification-bot -->",
                "<!-- MergeConflictBotSignature-v1 -->",
                "<!-- next-issue-bot-marker -->",
                "<!-- workflowbot:workflow-failure-notifier -->",
                "<!-- GFI Candidate Notification -->",
                "<!-- inactivity-unassign-bot -->",
                "<!-- issue-reminder-bot -->",
                "<!-- pr-inactivity-bot-marker -->",
                "<!-- linked-issue-enforcer -->",
                "<!-- community-call-reminder -->",
                "<!-- office-hours-reminder -->"
        );
    }
}
