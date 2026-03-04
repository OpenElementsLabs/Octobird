package com.openelements.octobird.config;

import java.util.List;

/**
 * Configuration for the bi-weekly office-hours reminder task.
 *
 * @param anchorDate      ISO-8601 date string ({@code YYYY-MM-DD}) of any past session used to
 *                        compute the bi-weekly schedule; empty string disables the task
 * @param meetingLink     hyperlink posted in the reminder comment (e.g. Zoom URL)
 * @param calendarLink    link to the public calendar for schedule verification
 * @param cancelledDates  ISO-8601 dates on which office hours are cancelled and no reminder is sent
 * @param excludedAuthors GitHub logins of team members who should not receive reminders
 */
public record OfficeHoursConfig(String anchorDate,
                                 String meetingLink,
                                 String calendarLink,
                                 List<String> cancelledDates,
                                 List<String> excludedAuthors) {

    /**
     * Returns the default office-hours configuration with all fields empty/disabled.
     *
     * @return an {@code OfficeHoursConfig} that produces no reminders until configured
     */
    public static OfficeHoursConfig defaults() {
        return new OfficeHoursConfig("", "", "", List.of(), List.of());
    }
}
