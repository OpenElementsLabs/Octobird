package org.hiero.bot.handler.impl;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.AbstractEventHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.hiero.bot.util.*;
import java.util.List;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Handles the {@code /assign} command for all difficulty levels (GFI, beginner, intermediate,
 * advanced), and posts an introductory reminder on unassigned GFI and beginner issues when a
 * non-collaborator comments without the assign command.
 *
 * <p>Validation rules per level:
 * <ul>
 *   <li><b>GFI</b> – spam users are limited by {@code spamUserMax}; normal users by
 *       {@code normalUserMax}.</li>
 *   <li><b>Beginner</b> – requires a minimum number of closed GFI issues; spam users are
 *       blocked entirely.</li>
 *   <li><b>Intermediate</b> – requires a minimum number of closed beginner issues; spam users
 *       are blocked entirely; exempt (ADMIN/WRITE) users bypass prerequisites.</li>
 *   <li><b>Advanced</b> – requires a minimum number of closed intermediate issues; spam users
 *       are blocked entirely; exempt (ADMIN/WRITE) users bypass prerequisites.</li>
 * </ul>
 *
 * <p>Enabled via {@link org.hiero.bot.config.FeaturesConfig#assignCommand()}.
 */
public final class AssignCommandHandler extends AbstractEventHandler<IssueCommentEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AssignCommandHandler.class);

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.ISSUE_COMMENT && action == GitHubAction.CREATED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().assignCommand();

    /**
     * Issue difficulty levels in order of precedence (highest first).
     */
    private enum Level {ADVANCED, INTERMEDIATE, BEGINNER, GFI}

    public AssignCommandHandler() {
        super(IssueCommentEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final IssueCommentEvent commentEvent, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();

        // Skip bots
        if ("Bot".equals(commentEvent.comment().user().type())) {
            return;
        }

        final String repoFullName = commentEvent.repository().fullName();
        final int issueNumber = commentEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        final Level level = determineLevel(issue.getLabels(), repoConfig);
        if (level == null) {
            return;
        }

        final Pattern assignPattern = repoConfig.commands().compiledAssignPattern();
        final String body = commentEvent.comment().body();
        final boolean hasAssignCommand = body != null && assignPattern.matcher(body).find();
        final String commenter = commentEvent.comment().user().login();

        if (hasAssignCommand) {
            handleAssignCommand(gitHub, repo, issue, commenter, repoFullName, issueNumber, level, repoConfig);
        } else {
            handleReminder(repo, issue, commenter, repoFullName, issueNumber, level, repoConfig);
        }
    }

    private Level determineLevel(final Collection<GHLabel> labels, final RepoConfig repoConfig) {
        for (final GHLabel label : labels) {
            if (label.getName().equalsIgnoreCase(repoConfig.labels().advanced())) {
                return Level.ADVANCED;
            }
        }
        for (final GHLabel label : labels) {
            if (label.getName().equalsIgnoreCase(repoConfig.labels().intermediate())) {
                return Level.INTERMEDIATE;
            }
        }
        for (final GHLabel label : labels) {
            if (label.getName().equalsIgnoreCase(repoConfig.labels().beginner())) {
                return Level.BEGINNER;
            }
        }
        for (final GHLabel label : labels) {
            if (label.getName().equalsIgnoreCase(repoConfig.labels().goodFirstIssue())) {
                return Level.GFI;
            }
        }
        return null;
    }

    private void handleAssignCommand(final GitHub gitHub, final GHRepository repo, final GHIssue issue,
                                     final String commenter, final String repoFullName,
                                     final int issueNumber, final Level level,
                                     final RepoConfig repoConfig) throws IOException {
        // Already assigned?
        final boolean alreadyAssigned = issue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(commenter));
        if (alreadyAssigned) {
            issue.comment(MessageFormatter.format("@{} you are already assigned to this issue.", commenter));
            return;
        }

        // Level-specific prerequisite check for Beginner and above (exempt users bypass)
        if (level != Level.GFI && !PermissionChecker.isExemptFromGuard(repo, commenter)) {
            if (!checkPrerequisite(gitHub, issue, commenter, repoFullName, level, repoConfig)) {
                return;
            }
        }

        // Spam check
        final String spamListPath = repoConfig.paths().spamList();
        final boolean isSpam = SpamListLoader.isSpamUser(gitHub, repoFullName, commenter, spamListPath);
        if (isSpam) {
            if (level == Level.GFI) {
                // Spam users can claim GFIs but with a lower limit
                final int spamMax = repoConfig.assignmentLimits().spamUserMax();
                final int count = IssueSearchHelper.countOpenAssignments(gitHub, repoFullName, commenter);
                if (count >= spamMax) {
                    issue.comment(MessageFormatter.format(
                            "Hi @{}, this is the Assignment Bot.\n\n" +
                                    "Your account currently has limited assignment privileges with a maximum of **{} open assignment** at a time.\n\n" +
                                    "You currently have {} open issue(s) assigned. " +
                                    "Please complete and merge your existing assignment before requesting a new one.",
                            commenter, spamMax, count));
                    return;
                }
            } else {
                // Spam users are completely blocked from beginner and above
                issue.comment(MessageFormatter.format(
                        "Hi @{}, this is the Assignment Bot.\n\n" +
                                "Your account currently has limited assignment privileges. " +
                                "You may only be assigned to issues labeled **Good First Issue**.\n\n" +
                                "Please complete and merge your assigned Good First Issue " +
                                "to have restrictions lifted.", commenter));
                return;
            }
        } else {
            // Assignment limit check for non-spam users
            final int normalMax = repoConfig.assignmentLimits().normalUserMax();
            final int count = IssueSearchHelper.countOpenAssignments(gitHub, repoFullName, commenter);
            if (count >= normalMax) {
                issue.comment(MessageFormatter.format(
                        "Hi @{}, this is the Assignment Bot.\n\n" +
                                "Assigning you to this issue would exceed the limit of {} open assignments.\n\n" +
                                "Please resolve and merge your existing assigned issues before requesting new ones.",
                        commenter, normalMax));
                return;
            }
        }

        issue.addAssignees(gitHub.getUser(commenter));
        issue.comment(MessageFormatter.format("@{} has been assigned to this issue.", commenter));
        LOG.info("Assigned {} to {} issue {}#{}", commenter, level.name().toLowerCase(), repoFullName, issueNumber);

        if (level == Level.GFI) {
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
     * Checks the level-specific prerequisite for the commenter. Returns {@code true} if qualified,
     * {@code false} if not (and posts an explanatory comment).
     */
    private boolean checkPrerequisite(final GitHub gitHub, final GHIssue issue, final String commenter,
                                      final String repoFullName, final Level level,
                                      final RepoConfig repoConfig) throws IOException {
        return switch (level) {
            case BEGINNER -> checkBeginnerPrerequisite(gitHub, issue, commenter, repoFullName, repoConfig);
            case INTERMEDIATE -> checkIntermediatePrerequisite(gitHub, issue, commenter, repoFullName, repoConfig);
            case ADVANCED -> checkAdvancedPrerequisite(gitHub, issue, commenter, repoFullName, repoConfig);
            default -> true;
        };
    }

    private boolean checkBeginnerPrerequisite(final GitHub gitHub, final GHIssue issue,
                                              final String commenter, final String repoFullName,
                                              final RepoConfig repoConfig) throws IOException {
        final String gfiLabel = repoConfig.labels().goodFirstIssue();
        final int required = repoConfig.guards().requiredGfiCountForBeginner();
        final String markerPrefix = repoConfig.markers().beginnerGfiGuard();
        final String userMarker = markerPrefix + " @" + commenter;

        final int closed = IssueSearchHelper.countClosedIssuesByLabel(gitHub, repoFullName, commenter, gfiLabel);
        if (closed < required) {
            if (!CommentMarkerChecker.hasMarker(issue, userMarker)) {
                issue.comment(MessageFormatter.format(
                        "{}\n\nHi @{}, this is the Assignment Bot.\n\n" +
                                "This is a **beginner** issue that requires at least **{}** completed Good First Issue(s).\n\n" +
                                "You currently have **{}** completed Good First Issue(s). " +
                                "Please complete a Good First Issue before requesting a beginner issue.",
                        userMarker, commenter, required, closed));
            }
            return false;
        }
        return true;
    }

    private boolean checkIntermediatePrerequisite(final GitHub gitHub, final GHIssue issue,
                                                  final String commenter, final String repoFullName,
                                                  final RepoConfig repoConfig) throws IOException {
        final String beginnerLabel = repoConfig.labels().beginner();
        final int required = repoConfig.guards().requiredBeginnerCountForIntermediate();
        final String markerPrefix = repoConfig.markers().intermediateGuard();
        final String userMarker = markerPrefix + " @" + commenter;

        final int closed = IssueSearchHelper.countClosedIssuesByLabel(gitHub, repoFullName, commenter, beginnerLabel);
        if (closed < required) {
            if (!CommentMarkerChecker.hasMarker(issue, userMarker)) {
                issue.comment(MessageFormatter.format(
                        "{}\n\nHi @{}, this is the Assignment Bot.\n\n" +
                                "This is an **intermediate** issue that requires at least **{}** completed beginner issue(s).\n\n" +
                                "You currently have **{}** completed beginner issue(s). " +
                                "Please complete the required beginner issues first.",
                        userMarker, commenter, required, closed));
            }
            return false;
        }
        return true;
    }

    private boolean checkAdvancedPrerequisite(final GitHub gitHub, final GHIssue issue,
                                              final String commenter, final String repoFullName,
                                              final RepoConfig repoConfig) throws IOException {
        final String intermediateLabel = repoConfig.labels().intermediate();
        final int required = repoConfig.guards().requiredIntermediateCountForAdvanced();
        final String markerPrefix = repoConfig.markers().advancedGuard();
        final String userMarker = markerPrefix + " @" + commenter;

        final int closed = IssueSearchHelper.countClosedIssuesByLabel(gitHub, repoFullName, commenter, intermediateLabel);
        if (closed < required) {
            if (!CommentMarkerChecker.hasMarker(issue, userMarker)) {
                issue.comment(MessageFormatter.format(
                        "{}\n\nHi @{}, this is the Assignment Bot.\n\n" +
                                "This is an **advanced** issue that requires at least **{}** completed intermediate issue(s).\n\n" +
                                "You currently have **{}** completed intermediate issue(s). " +
                                "Please complete the required intermediate issues first.",
                        userMarker, commenter, required, closed));
            }
            return false;
        }
        return true;
    }

    private void handleReminder(final GHRepository repo, final GHIssue issue, final String commenter,
                                final String repoFullName, final int issueNumber, final Level level,
                                final RepoConfig repoConfig) throws IOException {
        // Only post reminders for GFI and beginner issues
        if (level != Level.GFI && level != Level.BEGINNER) {
            return;
        }

        // Only post reminder if issue is unassigned
        if (!issue.getAssignees().isEmpty()) {
            return;
        }

        // Only for non-collaborators
        if (PermissionChecker.isCollaborator(repo, commenter)) {
            return;
        }

        if (level == Level.GFI) {
            final String reminderMarker = repoConfig.markers().gfiReminder();
            if (CommentMarkerChecker.hasMarker(issue, reminderMarker)) {
                return;
            }
            issue.comment(MessageFormatter.format(
                    "{}\n\nHi @{}, thanks for your interest in this issue!\n\n" +
                            "This is a **Good First Issue** \u2014 if you'd like to work on it, " +
                            "please comment `/assign` to get assigned.",
                    reminderMarker, commenter));
            LOG.info("Posted GFI assign reminder on {}#{}", repoFullName, issueNumber);
        } else {
            final String reminderMarker = repoConfig.markers().beginnerReminder();
            if (CommentMarkerChecker.hasMarker(issue, reminderMarker)) {
                return;
            }
            issue.comment(MessageFormatter.format(
                    "{}\n\nHi @{}, thanks for your interest in this issue!\n\n" +
                            "This is a **beginner** issue \u2014 if you'd like to work on it, " +
                            "please comment `/assign` to get assigned.",
                    reminderMarker, commenter));
            LOG.info("Posted beginner assign reminder on {}#{}", repoFullName, issueNumber);
        }
    }
}
