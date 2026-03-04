package com.openelements.octobird.scheduled.impl;

import com.openelements.octobird.config.RepoConfig;
import com.openelements.octobird.handler.ServiceRegistry;
import com.openelements.octobird.scheduled.AbstractScheduledTask;
import com.openelements.octobird.util.IssueSearchHelper;
import com.openelements.octobird.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;

/**
 * Twice-weekly scheduled task that closes open pull requests that violate the linked-issue policy.
 *
 * <p>A PR is closed if it is older than {@link com.openelements.octobird.config.ScheduledConfig#linkedIssueEnforcerDays()}
 * days and either:
 * <ul>
 *   <li>its body contains no closing reference to an issue, or</li>
 *   <li>{@link com.openelements.octobird.config.ScheduledConfig#requireAuthorAssigned()} is {@code true} and
 *       the PR author is not assigned to the linked issue.</li>
 * </ul>
 *
 * <p>Bot PRs are skipped. Enabled via
 * {@link com.openelements.octobird.config.FeaturesConfig#linkedIssueEnforcer()}.
 */
public class LinkedIssueEnforcerTask extends AbstractScheduledTask {

    private static final Logger LOG = LoggerFactory.getLogger(LinkedIssueEnforcerTask.class);

    private static final String REASON_NO_ISSUE = "no_issue";
    private static final String REASON_NOT_ASSIGNED = "not_assigned";

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().linkedIssueEnforcer();

    public LinkedIssueEnforcerTask() {
        super(FEATURE_CHECK);
    }

    @Override
    public void run(final ServiceRegistry registry, final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();
        final int minAgeDays = repoConfig.scheduled().linkedIssueEnforcerDays();
        final boolean requireAssigned = repoConfig.scheduled().requireAuthorAssigned();
        final String marker = repoConfig.markers().linkedIssueEnforcer();
        final GHRepository repo = gitHub.getRepository(repoConfig.repoFullName());

        for (final GHPullRequest pr : repo.queryPullRequests().state(GHIssueState.OPEN).list()) {
            if ("Bot".equals(pr.getUser().getType())) {
                continue;
            }

            if (prAgeInDays(pr) < minAgeDays) {
                continue;
            }

            final String body = pr.getBody();
            if (!IssueSearchHelper.hasLinkedIssueInBody(body)) {
                closePr(repo, pr, marker, REASON_NO_ISSUE);
                continue;
            }

            if (requireAssigned) {
                final int linkedIssueNumber = IssueSearchHelper.extractLinkedIssueNumber(body);
                if (linkedIssueNumber > 0) {
                    final GHIssue linkedIssue = repo.getIssue(linkedIssueNumber);
                    if (linkedIssue.getState() == GHIssueState.OPEN
                            && !isAuthorAssigned(linkedIssue, pr.getUser())) {
                        closePr(repo, pr, marker, REASON_NOT_ASSIGNED);
                    }
                }
            }
        }
    }

    long prAgeInDays(final GHPullRequest pr) throws IOException {
        return ChronoUnit.DAYS.between(pr.getCreatedAt().toInstant(), Instant.now());
    }

    private static boolean isAuthorAssigned(final GHIssue issue, final GHUser author) throws IOException {
        final String login = author.getLogin();
        return issue.getAssignees().stream().anyMatch(u -> login.equals(u.getLogin()));
    }

    private void closePr(final GHRepository repo, final GHPullRequest pr,
                          final String marker, final String reason) throws IOException {
        final String message;
        if (REASON_NO_ISSUE.equals(reason)) {
            message = MessageFormatter.format(
                    "{}\nHi there! I'm the LinkedIssueBot.\n\n"
                            + "This pull request has been automatically closed because it is not linked "
                            + "to any issue. Please link it to an issue and reopen the pull request if "
                            + "this is an error.\n\nThank you!",
                    marker);
        } else {
            message = MessageFormatter.format(
                    "{}\nHi there! I'm the LinkedIssueBot.\n\n"
                            + "This pull request has been automatically closed because you are not "
                            + "assigned to the linked issue. Please ensure you are assigned before "
                            + "reopening the pull request.\n\nThank you!",
                    marker);
        }
        final GHIssue prAsIssue = repo.getIssue(pr.getNumber());
        prAsIssue.comment(message);
        pr.close();
        LOG.info("Closed PR {}#{} (reason: {})", repo.getFullName(), pr.getNumber(), reason);
    }
}
