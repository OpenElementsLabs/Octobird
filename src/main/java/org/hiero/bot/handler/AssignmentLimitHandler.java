package org.hiero.bot.handler;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.config.IssueSearchHelper;
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

public class AssignmentLimitHandler extends AbstractEventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AssignmentLimitHandler.class);

    public AssignmentLimitHandler() {
        super(IssuesEvent.class, (event, action) -> event == GitHubEventType.ISSUES && action == GitHubAction.ASSIGNED);
    }

    @Override
    public void handle(final IssuesEvent issuesEvent, final GitHub gitHub,
                       final RepoConfig repoConfig) throws IOException {

        if (!repoConfig.features().assignmentLimit()) {
            return;
        }

        final String assignee = issuesEvent.assignee() != null ? issuesEvent.assignee().login() : "";
        if (assignee.isEmpty()) {
            return;
        }

        final String repoFullName = issuesEvent.repository().fullName();
        final int issueNumber = issuesEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        // Maintainers have no limit
        if (PermissionChecker.isMaintainer(repo, assignee)) {
            LOG.debug("{} is a maintainer, no limit applies", assignee);
            return;
        }

        final String spamListPath = repoConfig.paths().spamList();
        final boolean isSpam = SpamListLoader.isSpamUser(gitHub, repoFullName, assignee, spamListPath);

        if (isSpam) {
            handleSpamUser(gitHub, issue, assignee, repoFullName, issueNumber, repoConfig);
        } else {
            handleNormalUser(gitHub, issue, assignee, repoFullName, repoConfig);
        }
    }

    private void handleSpamUser(final GitHub gitHub, final GHIssue issue,
                                final String assignee, final String repoFullName,
                                final int issueNumber, final RepoConfig repoConfig) throws IOException {
        final String gfiLabel = repoConfig.labels().goodFirstIssue();

        // Spam users can only be assigned to Good First Issues
        final boolean hasGfiLabel = issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(gfiLabel::equals);

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

        final int spamMax = repoConfig.assignmentLimits().spamUserMax();

        // Spam users have a limit of open assignments
        final int count = IssueSearchHelper.countOpenAssignments(gitHub, repoFullName, assignee);
        if (count > spamMax) {
            LOG.info("Spam user {} exceeds limit: {} assignments", assignee, count);
            issue.removeAssignees(gitHub.getUser(assignee));
            issue.comment("Hi @" + assignee + ", this is the Assignment Bot.\n\n" +
                    "Your account currently has limited assignment privileges with a maximum of **" +
                    spamMax + " open assignment** at a time.\n\n" +
                    "You currently have " + count + " open issue(s) assigned. " +
                    "Please complete and merge your existing assignment before requesting a new one.");
        }
    }

    private void handleNormalUser(final GitHub gitHub, final GHIssue issue,
                                  final String assignee, final String repoFullName,
                                  final RepoConfig repoConfig) throws IOException {
        final int normalMax = repoConfig.assignmentLimits().normalUserMax();
        final int count = IssueSearchHelper.countOpenAssignments(gitHub, repoFullName, assignee);
        if (count > normalMax) {
            LOG.info("User {} exceeds limit: {} assignments", assignee, count);
            issue.removeAssignees(gitHub.getUser(assignee));
            issue.comment("Hi @" + assignee + ", this is the Assignment Bot.\n\n" +
                    "Assigning you to this issue would exceed the limit of " +
                    normalMax + " open assignments.\n\n" +
                    "Please resolve and merge your existing assigned issues before requesting new ones.");
        }
    }
}
