package org.hiero.bot.scheduled.impl;

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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Predicate;

/**
 * Daily scheduled task that posts an inactivity reminder on open pull requests that have had no
 * new commits for at least {@link org.hiero.bot.config.ScheduledConfig#prInactivityDays()} days.
 *
 * <p>Bot-authored PRs are skipped. An HTML marker prevents duplicate reminders on the same PR.
 * Enabled via {@link org.hiero.bot.config.FeaturesConfig#prInactivityReminder()}.
 */
public final class PrInactivityReminderTask extends AbstractScheduledTask {

    private static final Logger LOG = LoggerFactory.getLogger(PrInactivityReminderTask.class);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().prInactivityReminder();

    public PrInactivityReminderTask() {
        super(FEATURE_CHECK);
    }

    @Override
    public void run(final ServiceRegistry registry, final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();
        final int inactivityDays = repoConfig.scheduled().prInactivityDays();
        final String marker = repoConfig.markers().prInactivityReminder();
        final GHRepository repo = gitHub.getRepository(repoConfig.repoFullName());

        for (final GHPullRequest pr : repo.queryPullRequests().state(GHIssueState.OPEN).list()) {
            if ("Bot".equals(pr.getUser().getType())) {
                continue;
            }

            final long inactiveDays = daysSinceLastCommit(repo, pr);
            if (inactiveDays < inactivityDays) {
                continue;
            }

            final GHIssue prAsIssue = repo.getIssue(pr.getNumber());
            if (CommentMarkerChecker.hasMarker(prAsIssue, marker)) {
                LOG.debug("PR inactivity reminder already posted for {}#{}", repoConfig.repoFullName(), pr.getNumber());
                continue;
            }

            final String login = pr.getUser().getLogin();
            final String comment = MessageFormatter.format(
                    "{}\nHi @{},\n\n"
                            + "This pull request has had no commit activity for {} days. "
                            + "Are you still working on it?\n\n"
                            + "To keep the PR active, you can:\n"
                            + "- Push a new commit.\n"
                            + "- Comment `/working` on the linked **issue** (not this PR).\n\n"
                            + "If you're no longer working on this, please comment `/unassign` on the "
                            + "linked issue to release it for others. Otherwise, this PR may be closed "
                            + "due to inactivity.",
                    marker, login, inactiveDays);
            prAsIssue.comment(comment);
            LOG.info("Posted PR inactivity reminder on {}#{} ({} days inactive)",
                    repoConfig.repoFullName(), pr.getNumber(), inactiveDays);
        }
    }

    private static long daysSinceLastCommit(final GHRepository repo,
                                             final GHPullRequest pr) throws IOException {
        final String sha = pr.getHead().getSha();
        final Date committed = repo.getCommit(sha).getCommitDate();
        if (committed == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(committed.toInstant(), Instant.now());
    }
}
