package org.hiero.bot.scheduled.impl;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.scheduled.AbstractScheduledTask;
import org.hiero.bot.util.CommentMarkerChecker;
import org.hiero.bot.util.IssueSearchHelper;
import org.hiero.bot.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Daily scheduled task that posts a reminder on issues that have been assigned for at least
 * {@link org.hiero.bot.config.ScheduledConfig#issueReminderDays()} days with no open PR linked.
 *
 * <p>Skips issues where any assignee has posted a {@code /working} comment within the reminder
 * window, and skips issues that already carry the reminder marker. Enabled via
 * {@link org.hiero.bot.config.FeaturesConfig#issueReminderNoPr()}.
 */
public final class IssueReminderNoPrTask extends AbstractScheduledTask {

    private static final Logger LOG = LoggerFactory.getLogger(IssueReminderNoPrTask.class);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().issueReminderNoPr();

    public IssueReminderNoPrTask() {
        super(FEATURE_CHECK);
    }

    @Override
    public void run(final ServiceRegistry registry, final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();
        final int reminderDays = repoConfig.scheduled().issueReminderDays();
        final String workingPattern = repoConfig.commands().workingPattern();
        final String marker = repoConfig.markers().issueReminderNoPr();
        final GHRepository repo = gitHub.getRepository(repoConfig.repoFullName());

        for (final GHIssue issue : repo.queryIssues().state(GHIssueState.OPEN).list()) {
            if (issue.isPullRequest()) {
                continue;
            }
            final Collection<GHUser> assignees = issue.getAssignees();
            if (assignees.isEmpty()) {
                continue;
            }

            if (CommentMarkerChecker.hasMarker(issue, marker)) {
                continue;
            }

            if (hasRecentWorkingComment(issue, assignees, reminderDays, workingPattern)) {
                continue;
            }

            final Date assignedAt = findLastAssignmentDate(issue);
            if (assignedAt == null) {
                continue;
            }

            final long daysSinceAssignment = ChronoUnit.DAYS.between(
                    assignedAt.toInstant(), Instant.now());
            if (daysSinceAssignment < reminderDays) {
                continue;
            }

            final List<GHIssue> linkedPrs = IssueSearchHelper.findOpenPrsLinkingToIssue(
                    gitHub, repoConfig.repoFullName(), issue.getNumber());
            if (!linkedPrs.isEmpty()) {
                continue;
            }

            final String mentions = assignees.stream()
                    .map(u -> "@" + u.getLogin())
                    .collect(Collectors.joining(" "));
            final String comment = MessageFormatter.format(
                    "{}\nHi {} :wave:\n\n"
                            + "This issue has been assigned but no pull request has been created yet.\n"
                            + "Are you still planning on working on it?\n\n"
                            + "If you are, please create a draft PR linked to this issue or comment "
                            + "`/working` to let us know.\n"
                            + "If you're no longer able to work on this issue, you can comment "
                            + "`/unassign` to release it.",
                    marker, mentions);
            issue.comment(comment);
            LOG.info("Posted no-PR reminder on {}#{} ({} days, {} assignee(s))",
                    repoConfig.repoFullName(), issue.getNumber(), daysSinceAssignment, assignees.size());
        }
    }

    private static Date findLastAssignmentDate(final GHIssue issue) throws IOException {
        Date latest = null;
        for (final GHIssueEvent event : issue.listEvents()) {
            if ("assigned".equals(event.getEvent())) {
                final Date created = event.getCreatedAt();
                if (latest == null || created.after(latest)) {
                    latest = created;
                }
            }
        }
        return latest;
    }

    private static boolean hasRecentWorkingComment(final GHIssue issue,
                                                    final Collection<GHUser> assignees,
                                                    final int days,
                                                    final String workingPattern) throws IOException {
        final Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        final Pattern pattern = Pattern.compile(workingPattern);
        final List<String> assigneeLogins = assignees.stream()
                .map(GHUser::getLogin)
                .toList();
        for (final GHIssueComment comment : issue.listComments()) {
            if (!assigneeLogins.contains(comment.getUser().getLogin())) {
                continue;
            }
            if (comment.getCreatedAt().toInstant().isBefore(cutoff)) {
                continue;
            }
            final String body = comment.getBody();
            if (body != null && pattern.matcher(body).find()) {
                return true;
            }
        }
        return false;
    }
}
