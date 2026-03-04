package com.openelements.octobird.scheduled.impl;

import com.openelements.octobird.config.CommunityCallConfig;
import com.openelements.octobird.config.RepoConfig;
import com.openelements.octobird.handler.ServiceRegistry;
import com.openelements.octobird.scheduled.AbstractScheduledTask;
import com.openelements.octobird.util.CommentMarkerChecker;
import com.openelements.octobird.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Daily scheduled task that posts community-call reminders on open issues, bi-weekly.
 *
 * <p>One reminder is posted per external (non-excluded) issue author, on their most-recently
 * created open issue. The bi-weekly schedule is derived from the configured anchor date; the
 * task also checks that today's day of week matches the anchor's day of week. If an issue
 * already carries the reminder marker, it is skipped. Enabled via
 * {@link com.openelements.octobird.config.FeaturesConfig#communityCallReminder()}.
 */
public final class CommunityCallReminderTask extends AbstractScheduledTask {

    private static final Logger LOG = LoggerFactory.getLogger(CommunityCallReminderTask.class);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().communityCallReminder();

    public CommunityCallReminderTask() {
        super(FEATURE_CHECK);
    }

    @Override
    public void run(final ServiceRegistry registry, final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();
        final CommunityCallConfig callConfig = repoConfig.scheduled().communityCall();
        if (!isMeetingDay(callConfig.anchorDate(), callConfig.cancelledDates())) {
            return;
        }

        final String marker = repoConfig.markers().communityCallReminder();
        final GHRepository repo = gitHub.getRepository(repoConfig.repoFullName());

        // Collect newest open issue per author
        final Map<String, GHIssue> newestIssueByAuthor = new HashMap<>();
        for (final GHIssue issue : repo.queryIssues().state(GHIssueState.OPEN).list()) {
            if (issue.isPullRequest()) {
                continue;
            }
            final String author = issue.getUser().getLogin();
            if ("Bot".equals(issue.getUser().getType())) {
                continue;
            }
            if (callConfig.excludedAuthors().contains(author)) {
                continue;
            }
            final GHIssue existing = newestIssueByAuthor.get(author);
            if (existing == null || issue.getCreatedAt().after(existing.getCreatedAt())) {
                newestIssueByAuthor.put(author, issue);
            }
        }

        for (final Map.Entry<String, GHIssue> entry : newestIssueByAuthor.entrySet()) {
            final GHIssue issue = entry.getValue();
            if (CommentMarkerChecker.hasMarker(issue, marker)) {
                LOG.debug("Community call reminder already posted on {}#{}", repoConfig.repoFullName(), issue.getNumber());
                continue;
            }

            final String comment = buildComment(marker, callConfig);
            issue.comment(comment);
            LOG.info("Posted community call reminder on {}#{} for @{}",
                    repoConfig.repoFullName(), issue.getNumber(), entry.getKey());
        }
    }

    private static String buildComment(final String marker, final CommunityCallConfig cfg) {
        final StringBuilder sb = new StringBuilder(marker).append("\n");
        sb.append("Hello, this is CommunityCallBot.\n\n");
        sb.append("This is a reminder that the community call will begin in approximately 4 hours (14:00 UTC).\n\n");
        sb.append("The call is an open forum where contributors and users can discuss topics, raise issues, ");
        sb.append("and influence the direction of the project.\n\n");
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
            LOG.warn("Invalid community call anchor date: {}", anchorDateStr);
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

    static boolean isDayOfWeek(final DayOfWeek expected) {
        return LocalDate.now(ZoneOffset.UTC).getDayOfWeek() == expected;
    }
}
