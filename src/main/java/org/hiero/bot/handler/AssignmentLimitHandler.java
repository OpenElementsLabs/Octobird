package org.hiero.bot.handler;

import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.config.SpamListLoader;
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
import java.util.Map;
import java.util.Objects;

public class AssignmentLimitHandler implements EventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AssignmentLimitHandler.class);
    private static final String GOOD_FIRST_ISSUE_LABEL = "Good First Issue";
    private static final int SPAM_USER_MAX_ASSIGNMENTS = 1;
    private static final int NORMAL_USER_MAX_ASSIGNMENTS = 2;

    private final SpamListLoader spamListLoader;
    private final PermissionChecker permissionChecker;

    public AssignmentLimitHandler(final SpamListLoader spamListLoader, final PermissionChecker permissionChecker) {
        this.spamListLoader = Objects.requireNonNull(spamListLoader, "spamListLoader must not be null");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker must not be null");
    }

    @Override
    public Class<IssuesEvent> eventType() {
        return IssuesEvent.class;
    }

    @Override
    public boolean matches(final GitHubEventType event, final GitHubAction action) {
        return event == GitHubEventType.ISSUES && action == GitHubAction.ASSIGNED;
    }

    @Override
    public void handle(final IssuesEvent issuesEvent, final GitHub gitHub,
                       final Map<String, Object> repoConfig) throws IOException {

        final String assignee = issuesEvent.assignee() != null ? issuesEvent.assignee().login() : "";
        if (assignee.isEmpty()) {
            return;
        }

        final String repoFullName = issuesEvent.repository().fullName();
        final int issueNumber = issuesEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        // Maintainers have no limit
        if (permissionChecker.isMaintainer(repo, assignee)) {
            LOG.debug("{} is a maintainer, no limit applies", assignee);
            return;
        }

        final boolean isSpam = spamListLoader.isSpamUser(gitHub, repoFullName, assignee);

        if (isSpam) {
            handleSpamUser(gitHub, repo, issue, assignee, repoFullName, issueNumber);
        } else {
            handleNormalUser(gitHub, repo, issue, assignee, repoFullName, issueNumber);
        }
    }

    private void handleSpamUser(final GitHub gitHub, final GHRepository repo, final GHIssue issue,
                                final String assignee, final String repoFullName,
                                final int issueNumber) throws IOException {
        // Spam users can only be assigned to Good First Issues
        final boolean hasGfiLabel = issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(GOOD_FIRST_ISSUE_LABEL::equals);

        if (!hasGfiLabel) {
            LOG.info("Spam user {} attempted non-GFI issue #{}", assignee, issueNumber);
            issue.removeAssignees(gitHub.getUser(assignee));
            issue.comment("Hi @" + assignee + ", this is the Assignment Bot.\n\n" +
                    "Your account currently has limited assignment privileges. " +
                    "You may only be assigned to issues labeled **Good First Issue**.\n\n" +
                    "Please complete and merge your assigned Good First Issue " +
                    "to have restrictions lifted.");
            return;
        }

        // Spam users have a limit of 1 open assignment
        final int count = countOpenAssignments(gitHub, repoFullName, assignee);
        if (count > SPAM_USER_MAX_ASSIGNMENTS) {
            LOG.info("Spam user {} exceeds limit: {} assignments", assignee, count);
            issue.removeAssignees(gitHub.getUser(assignee));
            issue.comment("Hi @" + assignee + ", this is the Assignment Bot.\n\n" +
                    "Your account currently has limited assignment privileges with a maximum of **" +
                    SPAM_USER_MAX_ASSIGNMENTS + " open assignment** at a time.\n\n" +
                    "You currently have " + count + " open issue(s) assigned. " +
                    "Please complete and merge your existing assignment before requesting a new one.");
        }
    }

    private void handleNormalUser(final GitHub gitHub, final GHRepository repo, final GHIssue issue,
                                  final String assignee, final String repoFullName,
                                  final int issueNumber) throws IOException {
        final int count = countOpenAssignments(gitHub, repoFullName, assignee);
        if (count > NORMAL_USER_MAX_ASSIGNMENTS) {
            LOG.info("User {} exceeds limit: {} assignments", assignee, count);
            issue.removeAssignees(gitHub.getUser(assignee));
            issue.comment("Hi @" + assignee + ", this is the Assignment Bot.\n\n" +
                    "Assigning you to this issue would exceed the limit of " +
                    NORMAL_USER_MAX_ASSIGNMENTS + " open assignments.\n\n" +
                    "Please resolve and merge your existing assigned issues before requesting new ones.");
        }
    }

    private int countOpenAssignments(final GitHub gitHub, final String repoFullName,
                                     final String assignee) throws IOException {
        return gitHub.searchIssues()
                .q("repo:" + repoFullName + " is:issue is:open assignee:" + assignee)
                .list()
                .toList()
                .size();
    }
}
