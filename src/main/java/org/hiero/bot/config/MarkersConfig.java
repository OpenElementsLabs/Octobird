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
 * @param codeRabbitPlanTrigger          marker for CodeRabbit plan trigger
 * @param missingLinkedIssue             marker for missing linked issue reminder
 * @param verifiedCommits                marker for verified commits check
 * @param mergeConflict                  marker for merge conflict detection
 * @param nextIssueRecommendation        marker for next issue recommendation
 * @param workflowFailureNotification    marker for workflow failure notification
 */
public record MarkersConfig(String unassignPrefix,
                            String gfiReminder,
                            String beginnerReminder,
                            String beginnerGfiGuard,
                            String mentorAssignment,
                            String intermediateGuard,
                            String advancedGuard,
                            String codeRabbitPlanTrigger,
                            String missingLinkedIssue,
                            String verifiedCommits,
                            String mergeConflict,
                            String nextIssueRecommendation,
                            String workflowFailureNotification) {

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
                "<!-- CodeRabbit Plan Trigger -->",
                "<!-- LinkBot Missing Issue -->",
                "<!-- commit-verification-bot -->",
                "<!-- MergeConflictBotSignature-v1 -->",
                "<!-- next-issue-bot-marker -->",
                "<!-- workflowbot:workflow-failure-notifier -->"
        );
    }
}
