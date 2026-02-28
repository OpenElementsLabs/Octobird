package org.hiero.bot.config;

/**
 * Per-handler feature flags that control which handlers are active.
 *
 * @param unassignCommand               enable the /unassign command handler
 * @param workingCommand                enable the /working command handler
 * @param assignCommand                 enable the unified /assign command handler (all levels + mentor assignment)
 * @param codeRabbitPlanTrigger         enable the CodeRabbit plan trigger handler
 * @param missingLinkedIssue            enable the missing linked issue reminder handler
 * @param verifiedCommits               enable the verified commits check handler
 * @param mergeConflict                 enable the merge conflict detection handler
 * @param nextIssueRecommendation       enable the next issue recommendation handler
 * @param workflowFailureNotification   enable the workflow failure notification handler
 * @param p0IssueAlarm                  enable the P0 issue team alarm handler
 * @param gfiCandidateNotification      enable the GFI candidate team notification handler
 * @param inactivityUnassign            enable the inactivity unassign scheduled task
 * @param issueReminderNoPr             enable the issue reminder (no PR) scheduled task
 * @param prInactivityReminder          enable the PR inactivity reminder scheduled task
 * @param linkedIssueEnforcer           enable the linked-issue enforcer scheduled task
 * @param communityCallReminder         enable the community-call reminder scheduled task
 * @param officeHoursReminder           enable the office-hours reminder scheduled task
 */
public record FeaturesConfig(boolean unassignCommand,
                             boolean workingCommand,
                             boolean assignCommand,
                             boolean codeRabbitPlanTrigger,
                             boolean missingLinkedIssue,
                             boolean verifiedCommits,
                             boolean mergeConflict,
                             boolean nextIssueRecommendation,
                             boolean workflowFailureNotification,
                             boolean p0IssueAlarm,
                             boolean gfiCandidateNotification,
                             boolean inactivityUnassign,
                             boolean issueReminderNoPr,
                             boolean prInactivityReminder,
                             boolean linkedIssueEnforcer,
                             boolean communityCallReminder,
                             boolean officeHoursReminder) {

    /**
     * Returns the default feature configuration with all handlers enabled.
     *
     * @return a {@code FeaturesConfig} with all features enabled
     */
    public static FeaturesConfig defaults() {
        return new FeaturesConfig(true, true, true, true, true,
                true, true, true, true, true, true,
                true, true, true, true, true, true);
    }
}
