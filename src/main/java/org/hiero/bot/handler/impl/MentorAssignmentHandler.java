package org.hiero.bot.handler.impl;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.MentorRosterLoader;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.AbstractEventHandler;
import org.hiero.bot.handler.MessageFormatter;
import org.hiero.bot.handler.ServiceRegistry;
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
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class MentorAssignmentHandler extends AbstractEventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MentorAssignmentHandler.class);

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.ISSUES && action == GitHubAction.ASSIGNED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().mentorAssignment();

    public MentorAssignmentHandler() {
        super(IssuesEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final IssuesEvent issuesEvent, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();

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

        // Only GFI issues
        final String gfiLabel = repoConfig.labels().goodFirstIssue();
        final boolean hasGfiLabel = issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(gfiLabel::equalsIgnoreCase);
        if (!hasGfiLabel) {
            return;
        }

        // Check duplicate marker
        final String marker = repoConfig.markers().mentorAssignment();
        if (CommentMarkerChecker.hasMarker(issue, marker)) {
            LOG.debug("Mentor already assigned for {}#{}", repoFullName, issueNumber);
            return;
        }

        // Only for new contributors (no merged PRs)
        if (!IssueSearchHelper.hasNoMergedPullRequests(gitHub, repoFullName, assigneeLogin)) {
            LOG.debug("{} already has merged PRs, skipping mentor assignment", assigneeLogin);
            return;
        }

        // Select mentor from roster
        final String rosterPath = repoConfig.paths().mentorRoster();
        final List<String> roster = MentorRosterLoader.loadRoster(gitHub, repoFullName, rosterPath);
        final String mentor = MentorRosterLoader.selectMentor(roster);
        if (mentor == null) {
            LOG.debug("No mentors available for {}", repoFullName);
            return;
        }

        issue.comment(MessageFormatter.format(
                "{}\n\nWelcome @{}! \uD83D\uDC4B This is your first contribution \u2014 exciting!\n\n" +
                        "@{} has been assigned as your mentor for this issue. " +
                        "Feel free to ask them any questions as you work through it.\n\n" +
                        "Good luck and happy coding!",
                marker, assigneeLogin, mentor));
        LOG.info("Assigned mentor {} to newcomer {} on {}#{}", mentor, assigneeLogin, repoFullName, issueNumber);
    }
}
