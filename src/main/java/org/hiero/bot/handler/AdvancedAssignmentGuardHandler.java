package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssuesEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class AdvancedAssignmentGuardHandler implements EventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AdvancedAssignmentGuardHandler.class);
    private static final String ADVANCED_LABEL = "advanced";
    private static final String INTERMEDIATE_LABEL = "intermediate";
    private static final String MARKER_PREFIX = "<!-- advanced-check:unqualified -->";
    private static final int REQUIRED_INTERMEDIATE_COUNT = 1;

    private final PermissionChecker permissionChecker;
    private final IssueSearchHelper searchHelper;
    private final CommentMarkerChecker markerChecker;

    public AdvancedAssignmentGuardHandler(final PermissionChecker permissionChecker,
                                          final IssueSearchHelper searchHelper,
                                          final CommentMarkerChecker markerChecker) {
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker must not be null");
        this.searchHelper = Objects.requireNonNull(searchHelper, "searchHelper must not be null");
        this.markerChecker = Objects.requireNonNull(markerChecker, "markerChecker must not be null");
    }

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
                       final Map<String, Object> repoConfig) throws IOException {

        final String repoFullName = issuesEvent.repository().fullName();
        final int issueNumber = issuesEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        if (issuesEvent.action() == GitHubAction.LABELED) {
            handleLabeled(issuesEvent, gitHub, repo, issue, repoFullName, issueNumber);
        } else {
            handleAssigned(issuesEvent, gitHub, repo, issue, repoFullName, issueNumber);
        }
    }

    private void handleLabeled(final IssuesEvent issuesEvent, final GitHub gitHub,
                                final GHRepository repo, final GHIssue issue,
                                final String repoFullName, final int issueNumber) throws IOException {
        final var label = issuesEvent.label();
        if (label == null || !ADVANCED_LABEL.equalsIgnoreCase(label.name())) {
            return;
        }

        // Check all current assignees
        for (final GHUser assignee : issue.getAssignees()) {
            checkAndRemoveIfUnqualified(gitHub, repo, issue, assignee.getLogin(), repoFullName, issueNumber);
        }
    }

    private void handleAssigned(final IssuesEvent issuesEvent, final GitHub gitHub,
                                 final GHRepository repo, final GHIssue issue,
                                 final String repoFullName, final int issueNumber) throws IOException {
        final var assignee = issuesEvent.assignee();
        if (assignee == null || assignee.login().isEmpty()) {
            return;
        }

        // Skip bots
        if ("Bot".equals(assignee.type())) {
            return;
        }

        // Only advanced issues
        final boolean hasAdvancedLabel = issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(ADVANCED_LABEL::equalsIgnoreCase);
        if (!hasAdvancedLabel) {
            return;
        }

        checkAndRemoveIfUnqualified(gitHub, repo, issue, assignee.login(), repoFullName, issueNumber);
    }

    private void checkAndRemoveIfUnqualified(final GitHub gitHub, final GHRepository repo,
                                              final GHIssue issue, final String username,
                                              final String repoFullName, final int issueNumber) throws IOException {
        // Skip exempt users (ADMIN/WRITE)
        if (permissionChecker.isExemptFromGuard(repo, username)) {
            LOG.debug("{} is exempt from advanced guard", username);
            return;
        }

        // Check per-user marker
        final String userMarker = MARKER_PREFIX + " @" + username;
        if (markerChecker.hasMarker(issue, userMarker)) {
            LOG.debug("Advanced guard already checked for {} on {}#{}", username, repoFullName, issueNumber);
            return;
        }

        // Check qualification
        final int closedIntermediate = searchHelper.countClosedIssuesByLabel(
                gitHub, repoFullName, username, INTERMEDIATE_LABEL);

        if (closedIntermediate < REQUIRED_INTERMEDIATE_COUNT) {
            issue.removeAssignees(gitHub.getUser(username));
            issue.comment(userMarker + "\n\n" +
                    "Hi @" + username + ", this is the Assignment Bot.\n\n" +
                    "This is an **advanced** issue that requires at least **" +
                    REQUIRED_INTERMEDIATE_COUNT + "** completed intermediate issue(s).\n\n" +
                    "You currently have **" + closedIntermediate + "** completed intermediate issue(s). " +
                    "Please complete the required intermediate issues first.");
            LOG.info("Removed unqualified user {} from advanced issue {}#{}", username, repoFullName, issueNumber);
        }
    }
}
