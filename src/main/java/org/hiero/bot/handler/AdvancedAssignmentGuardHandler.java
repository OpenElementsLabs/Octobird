package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssuesEvent;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AdvancedAssignmentGuardHandler implements EventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AdvancedAssignmentGuardHandler.class);

    @Override
    public Class<IssuesEvent> eventType() {
        return IssuesEvent.class;
    }

    @Override
    public boolean matches(final GitHubEventType event, final GitHubAction action) {
        return event == GitHubEventType.ISSUES
                && (action == GitHubAction.ASSIGNED || action == GitHubAction.LABELED);
    }

    @Override
    public void handle(final IssuesEvent issuesEvent, final GitHub gitHub,
                       final RepoConfig repoConfig) throws IOException {

        if (!repoConfig.features().advancedGuard()) {
            return;
        }

        final String repoFullName = issuesEvent.repository().fullName();
        final int issueNumber = issuesEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        if (issuesEvent.action() == GitHubAction.LABELED) {
            handleLabeled(issuesEvent, gitHub, repo, issue, repoFullName, issueNumber, repoConfig);
        } else {
            handleAssigned(issuesEvent, gitHub, repo, issue, repoFullName, issueNumber, repoConfig);
        }
    }

    private void handleLabeled(final IssuesEvent issuesEvent, final GitHub gitHub,
                               final GHRepository repo, final GHIssue issue,
                               final String repoFullName, final int issueNumber,
                               final RepoConfig repoConfig) throws IOException {
        final var label = issuesEvent.label();
        final String advancedLabel = repoConfig.labels().advanced();
        if (label == null || !advancedLabel.equalsIgnoreCase(label.name())) {
            return;
        }

        // Check all current assignees
        for (final GHUser assignee : issue.getAssignees()) {
            checkAndRemoveIfUnqualified(gitHub, repo, issue, assignee.getLogin(), repoFullName, issueNumber, repoConfig);
        }
    }

    private void handleAssigned(final IssuesEvent issuesEvent, final GitHub gitHub,
                                final GHRepository repo, final GHIssue issue,
                                final String repoFullName, final int issueNumber,
                                final RepoConfig repoConfig) throws IOException {
        final var assignee = issuesEvent.assignee();
        if (assignee == null || assignee.login().isEmpty()) {
            return;
        }

        // Skip bots
        if ("Bot".equals(assignee.type())) {
            return;
        }

        // Only advanced issues
        final String advancedLabel = repoConfig.labels().advanced();
        final boolean hasAdvancedLabel = issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(advancedLabel::equalsIgnoreCase);
        if (!hasAdvancedLabel) {
            return;
        }

        checkAndRemoveIfUnqualified(gitHub, repo, issue, assignee.login(), repoFullName, issueNumber, repoConfig);
    }

    private void checkAndRemoveIfUnqualified(final GitHub gitHub, final GHRepository repo,
                                             final GHIssue issue, final String username,
                                             final String repoFullName, final int issueNumber,
                                             final RepoConfig repoConfig) throws IOException {
        // Skip exempt users (ADMIN/WRITE)
        if (PermissionChecker.isExemptFromGuard(repo, username)) {
            LOG.debug("{} is exempt from advanced guard", username);
            return;
        }

        // Check per-user marker
        final String markerPrefix = repoConfig.markers().advancedGuard();
        final String userMarker = markerPrefix + " @" + username;
        if (CommentMarkerChecker.hasMarker(issue, userMarker)) {
            LOG.debug("Advanced guard already checked for {} on {}#{}", username, repoFullName, issueNumber);
            return;
        }

        // Check qualification
        final String intermediateLabel = repoConfig.labels().intermediate();
        final int requiredIntermediateCount = repoConfig.guards().requiredIntermediateCountForAdvanced();
        final int closedIntermediate = IssueSearchHelper.countClosedIssuesByLabel(
                gitHub, repoFullName, username, intermediateLabel);

        if (closedIntermediate < requiredIntermediateCount) {
            issue.removeAssignees(gitHub.getUser(username));
            issue.comment(userMarker + "\n\n" +
                    "Hi @" + username + ", this is the Assignment Bot.\n\n" +
                    "This is an **advanced** issue that requires at least **" +
                    requiredIntermediateCount + "** completed intermediate issue(s).\n\n" +
                    "You currently have **" + closedIntermediate + "** completed intermediate issue(s). " +
                    "Please complete the required intermediate issues first.");
            LOG.info("Removed unqualified user {} from advanced issue {}#{}", username, repoFullName, issueNumber);
        }
    }
}
