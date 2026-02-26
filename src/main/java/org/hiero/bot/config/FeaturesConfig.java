package org.hiero.bot.config;

/**
 * Per-handler feature flags that control which handlers are active.
 *
 * @param unassignCommand               enable the /unassign command handler
 * @param workingCommand                enable the /working command handler
 * @param assignmentLimit               enable the assignment limit handler
 * @param gfiAssignCommand              enable the GFI /assign command handler
 * @param beginnerAssignCommand         enable the beginner /assign command handler
 * @param mentorAssignment              enable the mentor assignment handler
 * @param intermediateGuard             enable the intermediate assignment guard
 * @param advancedGuard                 enable the advanced assignment guard
 * @param codeRabbitPlanTrigger         enable the CodeRabbit plan trigger handler
 * @param missingLinkedIssue            enable the missing linked issue reminder handler
 * @param verifiedCommits               enable the verified commits check handler
 * @param mergeConflict                 enable the merge conflict detection handler
 * @param nextIssueRecommendation       enable the next issue recommendation handler
 * @param workflowFailureNotification   enable the workflow failure notification handler
 * @param p0IssueAlarm                  enable the P0 issue team alarm handler
 * @param gfiCandidateNotification      enable the GFI candidate team notification handler
 */
public record FeaturesConfig(boolean unassignCommand,
                             boolean workingCommand,
                             boolean assignmentLimit,
                             boolean gfiAssignCommand,
                             boolean beginnerAssignCommand,
                             boolean mentorAssignment,
                             boolean intermediateGuard,
                             boolean advancedGuard,
                             boolean codeRabbitPlanTrigger,
                             boolean missingLinkedIssue,
                             boolean verifiedCommits,
                             boolean mergeConflict,
                             boolean nextIssueRecommendation,
                             boolean workflowFailureNotification,
                             boolean p0IssueAlarm,
                             boolean gfiCandidateNotification) {

    /**
     * Returns the default feature configuration with all handlers enabled.
     *
     * @return a {@code FeaturesConfig} with all features enabled
     */
    public static FeaturesConfig defaults() {
        return new FeaturesConfig(true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true);
    }
}
