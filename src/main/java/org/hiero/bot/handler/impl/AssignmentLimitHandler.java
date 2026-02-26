package org.hiero.bot.handler.impl;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.AbstractEventHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssuesEvent;
import org.hiero.bot.util.IssueSearchHelper;
import org.hiero.bot.util.MessageFormatter;
import org.hiero.bot.util.PermissionChecker;
import org.hiero.bot.util.SpamListLoader;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Enforces per-user open assignment limits when an issue is assigned. Spam-listed users have a
 * tighter limit and are further restricted to Good First Issues only. Maintainers (ADMIN/WRITE)
 * are exempt.
 *
 * <p>If the limit is exceeded the handler unassigns the user and posts an explanatory comment.
 * Enabled via {@link org.hiero.bot.config.FeaturesConfig#assignmentLimit()}.
 */
public final class AssignmentLimitHandler extends AbstractEventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AssignmentLimitHandler.class);

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.ISSUES && action == GitHubAction.ASSIGNED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().assignmentLimit();

    public AssignmentLimitHandler() {
        super(IssuesEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final IssuesEvent issuesEvent, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();

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
            issue.comment(MessageFormatter.format(
                    "Hi @{}, this is the Assignment Bot.\n\n" +
                            "Your account currently has limited assignment privileges. " +
                            "You may only be assigned to issues labeled **Good First Issue**.\n\n" +
                            "Please complete and merge your assigned Good First Issue " +
                            "to have restrictions lifted.", assignee));
            return;
        }

        final int spamMax = repoConfig.assignmentLimits().spamUserMax();

        // Spam users have a limit of open assignments
        final int count = IssueSearchHelper.countOpenAssignments(gitHub, repoFullName, assignee);
        if (count > spamMax) {
            LOG.info("Spam user {} exceeds limit: {} assignments", assignee, count);
            issue.removeAssignees(gitHub.getUser(assignee));
            issue.comment(MessageFormatter.format(
                    "Hi @{}, this is the Assignment Bot.\n\n" +
                            "Your account currently has limited assignment privileges with a maximum of **{} open assignment** at a time.\n\n" +
                            "You currently have {} open issue(s) assigned. " +
                            "Please complete and merge your existing assignment before requesting a new one.",
                    assignee, spamMax, count));
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
            issue.comment(MessageFormatter.format(
                    "Hi @{}, this is the Assignment Bot.\n\n" +
                            "Assigning you to this issue would exceed the limit of {} open assignments.\n\n" +
                            "Please resolve and merge your existing assigned issues before requesting new ones.",
                    assignee, normalMax));
        }
    }
}
