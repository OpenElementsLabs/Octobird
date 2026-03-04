package com.openelements.octobird.scheduled.impl;

import com.openelements.octobird.config.RepoConfig;
import com.openelements.octobird.handler.ServiceRegistry;
import com.openelements.octobird.scheduled.AbstractScheduledTask;
import com.openelements.octobird.util.IssueSearchHelper;
import com.openelements.octobird.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueEvent;
import org.kohsuke.github.GHPullRequest;
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

/**
 * Daily scheduled task that unassigns contributors who have been inactive for too long.
 *
 * <p><b>Phase A – no linked PR:</b> If an issue has been assigned for at least
 * {@link com.openelements.octobird.config.ScheduledConfig#inactivityDays()} days and no open PR links to it,
 * the assignee is unassigned and a comment is posted explaining the action.
 *
 * <p><b>Phase B – stale PR:</b> If a linked open PR has had no commits for at least
 * {@code inactivityDays} days, the PR is closed, a comment is posted, and the assignee is
 * removed from the issue.
 *
 * <p>A recent {@code /working} comment (within the inactivity window) grants immunity and resets
 * the timer. Enabled via {@link com.openelements.octobird.config.FeaturesConfig#inactivityUnassign()}.
 */
public final class InactivityUnassignTask extends AbstractScheduledTask {

    private static final Logger LOG = LoggerFactory.getLogger(InactivityUnassignTask.class);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().inactivityUnassign();

    public InactivityUnassignTask() {
        super(FEATURE_CHECK);
    }

    @Override
    public void run(final ServiceRegistry registry, final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();
        final int inactivityDays = repoConfig.scheduled().inactivityDays();
        final String workingPattern = repoConfig.commands().workingPattern();
        final GHRepository repo = gitHub.getRepository(repoConfig.repoFullName());

        for (final GHIssue issue : repo.queryIssues().state(GHIssueState.OPEN).list()) {
            if (issue.isPullRequest()) {
                continue;
            }
            final Collection<GHUser> assignees = issue.getAssignees();
            if (assignees.isEmpty()) {
                continue;
            }

            for (final GHUser assignee : assignees) {
                final String login = assignee.getLogin();
                final Date assignedAt = findAssignmentDate(issue, login);
                if (assignedAt == null) {
                    LOG.debug("No assignment event found for {} on {}#{}, skipping",
                            login, repoConfig.repoFullName(), issue.getNumber());
                    continue;
                }

                final long daysSinceAssignment = ChronoUnit.DAYS.between(
                        assignedAt.toInstant(), Instant.now());

                if (daysSinceAssignment < inactivityDays) {
                    continue;
                }

                if (hasRecentWorkingComment(issue, login, inactivityDays, workingPattern)) {
                    LOG.debug("User @{} posted /working recently on {}#{}, skipping",
                            login, repoConfig.repoFullName(), issue.getNumber());
                    continue;
                }

                final List<GHIssue> linkedPrs = IssueSearchHelper.findOpenPrsLinkingToIssue(
                        gitHub, repoConfig.repoFullName(), issue.getNumber());

                if (linkedPrs.isEmpty()) {
                    // Phase A: no PR
                    phaseAUnassign(repo, issue, assignee, login, daysSinceAssignment, repoConfig);
                } else {
                    // Phase B: check PR staleness
                    phaseBHandlePrs(repo, issue, assignee, login, linkedPrs, inactivityDays, repoConfig);
                }
            }
        }
    }

    private void phaseAUnassign(final GHRepository repo, final GHIssue issue,
                                 final GHUser assignee, final String login,
                                 final long daysSinceAssignment,
                                 final RepoConfig repoConfig) throws IOException {
        final String marker = repoConfig.markers().inactivityUnassign();
        final String comment = MessageFormatter.format(
                "{}\nHi @{}, this is InactivityBot :wave:\n\n"
                        + "You were assigned to this issue **{} days** ago, and there is currently no "
                        + "open pull request linked to it. To keep the backlog available for active "
                        + "contributors, I'm unassigning you for now.\n\n"
                        + "If you'd like to continue working on this later, feel free to comment "
                        + "`/assign` on the issue to get re-assigned, and open a new PR when you're "
                        + "ready. :rocket:",
                marker, login, daysSinceAssignment);
        issue.comment(comment);
        issue.removeAssignees(List.of(assignee));
        LOG.info("Phase A: unassigned @{} from {}#{} ({} days inactive)",
                login, repo.getFullName(), issue.getNumber(), daysSinceAssignment);
    }

    private void phaseBHandlePrs(final GHRepository repo, final GHIssue issue,
                                  final GHUser assignee, final String login,
                                  final List<GHIssue> linkedPrs,
                                  final int inactivityDays,
                                  final RepoConfig repoConfig) throws IOException {
        for (final GHIssue linkedPrAsIssue : linkedPrs) {
            final GHPullRequest pr = repo.getPullRequest(linkedPrAsIssue.getNumber());
            if (pr.getState() != GHIssueState.OPEN) {
                continue;
            }

            final long prInactiveDays = daysSinceLastCommit(repo, pr);
            if (prInactiveDays < inactivityDays) {
                continue;
            }

            final String marker = repoConfig.markers().inactivityUnassign();
            final String comment = MessageFormatter.format(
                    "{}\nHi @{}, this is InactivityBot :wave:\n\n"
                            + "This pull request has had no new commits for **{} days**, so I'm closing "
                            + "it and unassigning you from the linked issue to keep the backlog healthy.\n\n"
                            + "If you'd like to continue working on this later, feel free to comment "
                            + "`/assign` on the issue to get re-assigned, and open a new PR when you're "
                            + "ready. :rocket:",
                    marker, login, prInactiveDays);
            pr.comment(comment);
            pr.close();
            issue.removeAssignees(List.of(assignee));
            LOG.info("Phase B: closed PR #{}  and unassigned @{} from {}#{} ({} days inactive)",
                    pr.getNumber(), login, repo.getFullName(), issue.getNumber(), prInactiveDays);
        }
    }

    private static Date findAssignmentDate(final GHIssue issue, final String login) throws IOException {
        Date latest = null;
        for (final GHIssueEvent event : issue.listEvents()) {
            if ("assigned".equals(event.getEvent())) {
                final GHUser eventAssignee = event.getAssignee();
                if (eventAssignee != null && login.equals(eventAssignee.getLogin())) {
                    final Date created = event.getCreatedAt();
                    if (latest == null || created.after(latest)) {
                        latest = created;
                    }
                }
            }
        }
        return latest;
    }

    private static boolean hasRecentWorkingComment(final GHIssue issue, final String login,
                                                    final int days, final String workingPattern)
            throws IOException {
        final Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        final Pattern pattern = Pattern.compile(workingPattern);
        for (final GHIssueComment comment : issue.listComments()) {
            if (!login.equals(comment.getUser().getLogin())) {
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
