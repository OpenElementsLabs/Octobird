package org.hiero.bot.config;

/**
 * Thresholds and sub-configurations for the scheduled (cron-style) bot tasks.
 *
 * @param inactivityDays            days without activity before an issue assignment is revoked
 *                                  and (for issues with a PR) the PR is closed (default: 21)
 * @param issueReminderDays         days after assignment before posting a "no PR yet" reminder
 *                                  on an issue (default: 7)
 * @param prInactivityDays          days without a new commit before posting an inactivity reminder
 *                                  on a PR (default: 10)
 * @param linkedIssueEnforcerDays   minimum age in days of a PR before the linked-issue check is
 *                                  applied; newer PRs are skipped (default: 3)
 * @param requireAuthorAssigned     when {@code true} the linked-issue enforcer also verifies that
 *                                  the PR author is assigned to the linked issue (default: true)
 * @param communityCall             community-call reminder configuration
 * @param officeHours               office-hours reminder configuration
 */
public record ScheduledConfig(int inactivityDays,
                               int issueReminderDays,
                               int prInactivityDays,
                               int linkedIssueEnforcerDays,
                               boolean requireAuthorAssigned,
                               CommunityCallConfig communityCall,
                               OfficeHoursConfig officeHours) {

    /**
     * Returns the default scheduled configuration.
     *
     * @return a {@code ScheduledConfig} with standard thresholds and disabled call/hours reminders
     */
    public static ScheduledConfig defaults() {
        return new ScheduledConfig(21, 7, 10, 3, true,
                CommunityCallConfig.defaults(), OfficeHoursConfig.defaults());
    }
}
