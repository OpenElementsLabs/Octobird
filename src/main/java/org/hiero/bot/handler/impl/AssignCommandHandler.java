package org.hiero.bot.handler.impl;

import org.hiero.bot.config.IssueLevel;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.IssueCommandTriggerHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.hiero.bot.util.*;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Handles the {@code /assign} command for all difficulty levels (GFI, beginner, intermediate,
 * advanced). On a successful GFI assignment, a mentor is assigned to first-time contributors.
 *
 * <p>Validation order:
 * <ol>
 *   <li>Issue must already be unassigned (no existing assignees).</li>
 *   <li>Committer (ADMIN/WRITE) users are told to self-assign.</li>
 *   <li>Open assignment count must not exceed {@code normalUserMax}.</li>
 *   <li>Spam-listed users are blocked entirely.</li>
 *   <li>Level prerequisite: for levels above GFI the user must have completed a minimum
 *       number of issues at the previous level (configurable via {@code guards}).</li>
 * </ol>
 *
 * <p>Enabled via {@link org.hiero.bot.config.FeaturesConfig#assignCommand()}.
 */
public final class AssignCommandHandler extends IssueCommandTriggerHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AssignCommandHandler.class);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().assignCommand();

    public AssignCommandHandler() {
        super(FEATURE_CHECK);
    }

    @Override
    protected Pattern commandPattern(final RepoConfig repoConfig) {
        return repoConfig.commands().compiledAssignPattern();
    }

    @Override
    protected void handleCommand(final IssueCommentEvent commentEvent, final ServiceRegistry registry,
                                 final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();
        final String repoFullName = commentEvent.repository().fullName();
        final int issueNumber = commentEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        final IssueLevel issueLevel = IssueLevelDetector.determineLevel(issue.getLabels(), repoConfig.labels());
        if (issueLevel == null) {
            return;
        }

        final String commenter = commentEvent.comment().user().login();
        handleAssignCommand(gitHub, repo, issue, commenter, repoFullName, issueNumber, issueLevel, repoConfig);
    }

    private void handleAssignCommand(final GitHub gitHub, final GHRepository repo, final GHIssue issue,
                                     final String commenter, final String repoFullName,
                                     final int issueNumber, final IssueLevel issueLevel,
                                     final RepoConfig repoConfig) throws IOException {
        if (!issue.getAssignees().isEmpty()) {
            issue.comment(MessageFormatter.format("Hi @{}, another account is already assigned to this issue."));
            return;
        }
        if (PermissionChecker.isCommitterOfRepo(repo, commenter)) {
            issue.comment(MessageFormatter.format("@{} you are already a committer of this repository and can assign you by yourself.", commenter));
            return;
        }
        if (issue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(commenter))) {
            issue.comment(MessageFormatter.format("@{} you are already assigned to this issue.", commenter));
            return;
        }
        final int normalMax = repoConfig.assignmentLimits().normalUserMax();
        final int count = IssueSearchHelper.countOpenAssignments(gitHub, repoFullName, commenter);
        if (count >= normalMax) {
            issue.comment(MessageFormatter.format(
                    "Hi @{}, assigning you to this issue would exceed the limit of {} open assignments.\n\n" +
                            "Please resolve and merge your existing assigned issues before requesting new ones.",
                    commenter, normalMax));
            return;
        }
        final String spamListPath = repoConfig.paths().spamList();
        final boolean isSpam = SpamListLoader.isSpamUser(gitHub, repoFullName, commenter, spamListPath);
        if (isSpam) {
            issue.comment(MessageFormatter.format(
                    "Hi @{}, your account has been flagged for spam activity in this repo and cannot be assigned to issues.\n\n" +
                            "If you believe this is a mistake, please contact the maintainers.",
                    commenter));
            return;
        }
        if (!checkPrerequisite(gitHub, issue, commenter, repoFullName, issueLevel, repoConfig)) {
            return;
        }

        issue.addAssignees(gitHub.getUser(commenter));
        issue.comment(MessageFormatter.format("@{} has been assigned to this issue.", commenter));
        LOG.info("Assigned {} to {} issue {}#{}", commenter, issueLevel.name().toLowerCase(), repoFullName, issueNumber);

        if (issueLevel == IssueLevel.GOOD_FIRST_ISSUE) {
            tryAssignMentor(gitHub, issue, commenter, repoFullName, issueNumber, repoConfig);
        }
    }

    private void tryAssignMentor(final GitHub gitHub, final GHIssue issue, final String assignee,
                                 final String repoFullName, final int issueNumber,
                                 final RepoConfig repoConfig) throws IOException {
        final String marker = repoConfig.markers().mentorAssignment();
        if (CommentMarkerChecker.hasMarker(issue, marker)) {
            LOG.debug("Mentor already assigned for {}#{}", repoFullName, issueNumber);
            return;
        }

        if (!IssueSearchHelper.hasNoMergedPullRequests(gitHub, repoFullName, assignee)) {
            LOG.debug("{} already has merged PRs, skipping mentor assignment", assignee);
            return;
        }

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
                marker, assignee, mentor));
        LOG.info("Assigned mentor {} to newcomer {} on {}#{}", mentor, assignee, repoFullName, issueNumber);
    }

    /**
     * Checks the level-specific prerequisite for the commenter. The user must have completed
     * a minimum number of issues at the previous level. Returns {@code true} if qualified,
     * {@code false} if not (and posts an explanatory comment).
     */
    private boolean checkPrerequisite(final GitHub gitHub, final GHIssue issue, final String commenter,
                                      final String repoFullName, final IssueLevel issueLevel,
                                      final RepoConfig repoConfig) throws IOException {
        final IssueLevel previousLevel = issueLevel.previousLevel();
        if (previousLevel == null) {
            return true;
        }

        final String previousLabel = repoConfig.labels().labelFor(previousLevel);
        final int required = repoConfig.guards().requiredCountFor(issueLevel);
        final String markerPrefix = repoConfig.markers().guardMarkerFor(issueLevel);
        final String userMarker = markerPrefix + " @" + commenter;

        final int closed = IssueSearchHelper.countClosedIssuesByLabel(gitHub, repoFullName, commenter, previousLabel);
        if (closed < required) {
            if (!CommentMarkerChecker.hasMarker(issue, userMarker)) {
                final String currentLevelLabel = repoConfig.labels().labelFor(issueLevel);
                issue.comment(MessageFormatter.format(
                        "Hi @{}, this is an issue labeled by {}. Working on it requires at least **{}** completed issue(s) labeled with {}.\n\n" +
                                "You currently have **{}** completed issue(s) with the {} label. ",
                        commenter, currentLevelLabel, required,
                        previousLabel, closed, previousLabel));
            }
            return false;
        }
        return true;
    }

}
