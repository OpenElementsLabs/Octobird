package org.hiero.bot.handler.impl;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.AbstractEventHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.hiero.bot.util.*;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Handles the {@code /assign} command on beginner-labeled issues, and posts a reminder when a
 * non-collaborator comments on an unassigned beginner issue without the command.
 *
 * <p>Two execution paths:
 * <ul>
 *   <li><b>Assign command</b> – checks the GFI prerequisite guard, blocks spam users, validates
 *       assignment limits, and assigns the commenter.</li>
 *   <li><b>Reminder</b> – posts a one-time reminder (guarded by HTML marker) telling the user
 *       to use {@code /assign}.</li>
 * </ul>
 *
 * <p>Enabled via {@link org.hiero.bot.config.FeaturesConfig#beginnerAssignCommand()}.
 */
public final class BeginnerAssignCommandHandler extends AbstractEventHandler<IssueCommentEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(BeginnerAssignCommandHandler.class);

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.ISSUE_COMMENT && action == GitHubAction.CREATED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().beginnerAssignCommand();

    public BeginnerAssignCommandHandler() {
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

        // Only beginner issues
        final String beginnerLabel = repoConfig.labels().beginner();
        final boolean hasBeginnerLabel = issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(beginnerLabel::equalsIgnoreCase);
        if (!hasBeginnerLabel) {
            return;
        }

        final Pattern assignPattern = repoConfig.commands().compiledAssignPattern();
        final String body = commentEvent.comment().body();
        final boolean hasAssignCommand = body != null && assignPattern.matcher(body).find();
        final String commenter = commentEvent.comment().user().login();

        if (hasAssignCommand) {
            handleAssignCommand(gitHub, repo, issue, commenter, repoFullName, issueNumber, repoConfig);
        } else {
            handleReminder(repo, issue, commenter, repoFullName, issueNumber, repoConfig);
        }
    }

    private void handleAssignCommand(final GitHub gitHub, final GHRepository repo, final GHIssue issue,
                                     final String commenter, final String repoFullName,
                                     final int issueNumber, final RepoConfig repoConfig) throws IOException {
        final String gfiLabel = repoConfig.labels().goodFirstIssue();
        final int requiredGfiCount = repoConfig.guards().requiredGfiCountForBeginner();
        final String gfiGuardMarker = repoConfig.markers().beginnerGfiGuard();

        // GFI prerequisite check
        if (!PermissionChecker.isExemptFromGuard(repo, commenter)) {
            final int closedGfi = IssueSearchHelper.countClosedIssuesByLabel(
                    gitHub, repoFullName, commenter, gfiLabel);
            if (closedGfi < requiredGfiCount) {
                final String userMarker = gfiGuardMarker + " @" + commenter;
                if (!CommentMarkerChecker.hasMarker(issue, userMarker)) {
                    issue.comment(MessageFormatter.format(
                            "{}\n\nHi @{}, this is the Assignment Bot.\n\n" +
                                    "This is a **beginner** issue that requires at least **{}** completed Good First Issue(s).\n\n" +
                                    "You currently have **{}** completed Good First Issue(s). " +
                                    "Please complete a Good First Issue before requesting a beginner issue.",
                            userMarker, commenter, requiredGfiCount, closedGfi));
                }
                return;
            }
        }

        // Spam users are completely blocked from beginner issues
        final String spamListPath = repoConfig.paths().spamList();
        final boolean isSpam = SpamListLoader.isSpamUser(gitHub, repoFullName, commenter, spamListPath);
        if (isSpam) {
            issue.comment(MessageFormatter.format(
                    "Hi @{}, this is the Assignment Bot.\n\n" +
                            "Your account currently has limited assignment privileges. " +
                            "You may only be assigned to issues labeled **Good First Issue**.\n\n" +
                            "Please complete and merge your assigned Good First Issue " +
                            "to have restrictions lifted.", commenter));
            return;
        }

        // Already assigned?
        final boolean alreadyAssigned = issue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(commenter));
        if (alreadyAssigned) {
            issue.comment(MessageFormatter.format("@{} you are already assigned to this issue.", commenter));
            return;
        }

        // Assignment limit check
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

        issue.addAssignees(gitHub.getUser(commenter));
        issue.comment(MessageFormatter.format("@{} has been assigned to this issue.", commenter));
        LOG.info("Assigned {} to beginner issue {}#{}", commenter, repoFullName, issueNumber);
    }

    private void handleReminder(final GHRepository repo, final GHIssue issue, final String commenter,
                                final String repoFullName, final int issueNumber,
                                final RepoConfig repoConfig) throws IOException {
        // Only post reminder if issue is unassigned
        if (!issue.getAssignees().isEmpty()) {
            return;
        }

        // Only for non-collaborators
        if (PermissionChecker.isCollaborator(repo, commenter)) {
            return;
        }

        final String reminderMarker = repoConfig.markers().beginnerReminder();

        // Check duplicate marker
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
