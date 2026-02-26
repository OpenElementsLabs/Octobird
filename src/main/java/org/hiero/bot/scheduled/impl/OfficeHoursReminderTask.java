package org.hiero.bot.scheduled.impl;

import org.hiero.bot.config.OfficeHoursConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.scheduled.AbstractScheduledTask;
import org.hiero.bot.util.CommentMarkerChecker;
import org.hiero.bot.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Daily scheduled task that posts office-hours reminders on open pull requests, bi-weekly.
 *
 * <p>One reminder is posted per external (non-excluded) PR author, on their most-recently
 * created open PR. The bi-weekly schedule is derived from the configured anchor date; the
 * task also checks that today's day of week matches the anchor's day of week. If a PR already
 * carries the reminder marker, it is skipped. Enabled via
 * {@link org.hiero.bot.config.FeaturesConfig#officeHoursReminder()}.
 */
public final class OfficeHoursReminderTask extends AbstractScheduledTask {

    private static final Logger LOG = LoggerFactory.getLogger(OfficeHoursReminderTask.class);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().officeHoursReminder();

    public OfficeHoursReminderTask() {
        super(FEATURE_CHECK);
    }

    @Override
    public void run(final ServiceRegistry registry, final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();
        final OfficeHoursConfig ohConfig = repoConfig.scheduled().officeHours();
        if (!isMeetingDay(ohConfig.anchorDate(), ohConfig.cancelledDates())) {
            return;
        }

        final String marker = repoConfig.markers().officeHoursReminder();
        final GHRepository repo = gitHub.getRepository(repoConfig.repoFullName());

        // Collect newest open PR per author
        final Map<String, GHPullRequest> newestPrByAuthor = new HashMap<>();
        for (final GHPullRequest pr : repo.queryPullRequests().state(GHIssueState.OPEN).list()) {
            final String author = pr.getUser().getLogin();
            if ("Bot".equals(pr.getUser().getType())
                    || author.endsWith("[bot]")
                    || ohConfig.excludedAuthors().contains(author)) {
                continue;
            }
            final GHPullRequest existing = newestPrByAuthor.get(author);
            if (existing == null || pr.getCreatedAt().after(existing.getCreatedAt())) {
                newestPrByAuthor.put(author, pr);
            }
        }

        for (final Map.Entry<String, GHPullRequest> entry : newestPrByAuthor.entrySet()) {
            final GHPullRequest pr = entry.getValue();
            final GHIssue prAsIssue = repo.getIssue(pr.getNumber());
            if (CommentMarkerChecker.hasMarker(prAsIssue, marker)) {
                LOG.debug("Office hours reminder already posted on {}#{}", repoConfig.repoFullName(), pr.getNumber());
                continue;
            }

            final String comment = buildComment(marker, ohConfig);
            prAsIssue.comment(comment);
            LOG.info("Posted office hours reminder on {}#{} for @{}",
                    repoConfig.repoFullName(), pr.getNumber(), entry.getKey());
        }
    }

    private static String buildComment(final String marker, final OfficeHoursConfig cfg) {
        final StringBuilder sb = new StringBuilder(marker).append("\n");
        sb.append("Hello, this is the OfficeHourBot.\n\n");
        sb.append("This is a reminder that office hours are scheduled in approximately 4 hours (14:00 UTC).\n\n");
        sb.append("This session provides an opportunity to ask questions regarding this Pull Request.\n\n");
        if (!cfg.meetingLink().isBlank()) {
            sb.append(MessageFormatter.format("- Join Link: [Zoom Meeting]({})\n", cfg.meetingLink()));
        }
        if (!cfg.calendarLink().isBlank()) {
            sb.append(MessageFormatter.format(
                    "\nDisclaimer: This is an automated reminder. Please verify the schedule [here]({}) "
                            + "for any changes.", cfg.calendarLink()));
        }
        return sb.toString();
    }

    static boolean isMeetingDay(final String anchorDateStr, final List<String> cancelledDates) {
        if (anchorDateStr == null || anchorDateStr.isBlank()) {
            return false;
        }
        final LocalDate anchor;
        try {
            anchor = LocalDate.parse(anchorDateStr);
        } catch (final DateTimeParseException e) {
            LOG.warn("Invalid office hours anchor date: {}", anchorDateStr);
            return false;
        }
        final LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (today.getDayOfWeek() != anchor.getDayOfWeek()) {
            return false;
        }
        final long daysBetween = ChronoUnit.DAYS.between(anchor, today);
        if (daysBetween < 0 || daysBetween % 14 != 0) {
            return false;
        }
        final String todayStr = today.toString();
        return !cancelledDates.contains(todayStr);
    }
}
