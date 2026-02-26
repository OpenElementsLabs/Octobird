package org.hiero.bot.config;

import java.util.List;

/**
 * Configuration for the bi-weekly community-call reminder task.
 *
 * @param anchorDate      ISO-8601 date string ({@code YYYY-MM-DD}) of any past meeting used to
 *                        compute the bi-weekly schedule; empty string disables the task
 * @param meetingLink     hyperlink posted in the reminder comment (e.g. Zoom URL)
 * @param calendarLink    link to the public calendar for schedule verification
 * @param cancelledDates  ISO-8601 dates on which the call is cancelled and no reminder is sent
 * @param excludedAuthors GitHub logins of team members who should not receive reminders
 */
public record CommunityCallConfig(String anchorDate,
                                   String meetingLink,
                                   String calendarLink,
                                   List<String> cancelledDates,
                                   List<String> excludedAuthors) {

    /**
     * Returns the default community-call configuration with all fields empty/disabled.
     *
     * @return a {@code CommunityCallConfig} that produces no reminders until configured
     */
    public static CommunityCallConfig defaults() {
        return new CommunityCallConfig("", "", "", List.of(), List.of());
    }
}
