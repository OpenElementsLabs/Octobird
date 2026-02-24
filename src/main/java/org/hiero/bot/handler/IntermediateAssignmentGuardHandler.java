package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssuesEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class IntermediateAssignmentGuardHandler extends AbstractEventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(IntermediateAssignmentGuardHandler.class);

    public IntermediateAssignmentGuardHandler() {
        super(IssuesEvent.class, (event, action) -> event == GitHubEventType.ISSUES && action == GitHubAction.ASSIGNED);
    }

    @Override
    public void handle(final IssuesEvent issuesEvent, final GitHub gitHub,
                       final RepoConfig repoConfig) throws IOException {

        if (!repoConfig.features().intermediateGuard()) {
            return;
        }

        final int requiredBeginnerCount = repoConfig.guards().requiredBeginnerCountForIntermediate();

        // Guard deactivated when requiredBeginnerCount == 0
        if (requiredBeginnerCount == 0) {
            return;
        }

        final var assignee = issuesEvent.assignee();
        if (assignee == null || assignee.login().isEmpty()) {
            return;
        }

        final String assigneeLogin = assignee.login();

        // Skip bots
        if ("Bot".equals(assignee.type())) {
            return;
        }

        final String repoFullName = issuesEvent.repository().fullName();
        final int issueNumber = issuesEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        // Only intermediate issues
        final String intermediateLabel = repoConfig.labels().intermediate();
        final boolean hasIntermediateLabel = issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(intermediateLabel::equalsIgnoreCase);
        if (!hasIntermediateLabel) {
            return;
        }

        // Skip exempt users (ADMIN/WRITE)
        if (PermissionChecker.isExemptFromGuard(repo, assigneeLogin)) {
            LOG.debug("{} is exempt from intermediate guard", assigneeLogin);
            return;
        }

        // Check per-user marker
        final String markerPrefix = repoConfig.markers().intermediateGuard();
        final String userMarker = markerPrefix + " @" + assigneeLogin;
        if (CommentMarkerChecker.hasMarker(issue, userMarker)) {
            LOG.debug("Intermediate guard already checked for {} on {}#{}", assigneeLogin, repoFullName, issueNumber);
            return;
        }

        // Check qualification
        final String beginnerLabel = repoConfig.labels().beginner();
        final int closedBeginner = IssueSearchHelper.countClosedIssuesByLabel(
                gitHub, repoFullName, assigneeLogin, beginnerLabel);

        if (closedBeginner < requiredBeginnerCount) {
            issue.removeAssignees(gitHub.getUser(assigneeLogin));
            issue.comment(userMarker + "\n\n" +
                    "Hi @" + assigneeLogin + ", this is the Assignment Bot.\n\n" +
                    "This is an **intermediate** issue that requires at least **" +
                    requiredBeginnerCount + "** completed beginner issue(s).\n\n" +
                    "You currently have **" + closedBeginner + "** completed beginner issue(s). " +
                    "Please complete the required beginner issues first.");
            LOG.info("Removed unqualified user {} from intermediate issue {}#{}", assigneeLogin, repoFullName, issueNumber);
        }
    }
}
