package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.config.SpamListLoader;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssueCommentEvent;
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

public class GfiAssignCommandHandler extends AbstractEventHandler<IssueCommentEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(GfiAssignCommandHandler.class);

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.ISSUE_COMMENT && action == GitHubAction.CREATED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().gfiAssignCommand();

    public GfiAssignCommandHandler() {
        super(IssueCommentEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final IssueCommentEvent commentEvent, final GitHub gitHub,
                       final RepoConfig repoConfig) throws IOException {

        // Skip bots
        if ("Bot".equals(commentEvent.comment().user().type())) {
            return;
        }

        final String repoFullName = commentEvent.repository().fullName();
        final int issueNumber = commentEvent.issue().number();

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
        // Already assigned?
        final boolean alreadyAssigned = issue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(commenter));
        if (alreadyAssigned) {
            issue.comment("@" + commenter + " you are already assigned to this issue.");
            return;
        }

        final String spamListPath = repoConfig.paths().spamList();
        final boolean isSpam = SpamListLoader.isSpamUser(gitHub, repoFullName, commenter, spamListPath);
        final int spamMax = repoConfig.assignmentLimits().spamUserMax();
        final int normalMax = repoConfig.assignmentLimits().normalUserMax();

        if (isSpam) {
            final int count = IssueSearchHelper.countOpenAssignments(gitHub, repoFullName, commenter);
            if (count >= spamMax) {
                issue.comment("Hi @" + commenter + ", this is the Assignment Bot.\n\n" +
                        "Your account currently has limited assignment privileges with a maximum of **" +
                        spamMax + " open assignment** at a time.\n\n" +
                        "You currently have " + count + " open issue(s) assigned. " +
                        "Please complete and merge your existing assignment before requesting a new one.");
                return;
            }
        } else {
            final int count = IssueSearchHelper.countOpenAssignments(gitHub, repoFullName, commenter);
            if (count >= normalMax) {
                issue.comment("Hi @" + commenter + ", this is the Assignment Bot.\n\n" +
                        "Assigning you to this issue would exceed the limit of " +
                        normalMax + " open assignments.\n\n" +
                        "Please resolve and merge your existing assigned issues before requesting new ones.");
                return;
            }
        }

        issue.addAssignees(gitHub.getUser(commenter));
        issue.comment("@" + commenter + " has been assigned to this issue.");
        LOG.info("Assigned {} to GFI {}#{}", commenter, repoFullName, issueNumber);
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

        final String reminderMarker = repoConfig.markers().gfiReminder();

        // Check duplicate marker
        if (CommentMarkerChecker.hasMarker(issue, reminderMarker)) {
            return;
        }

        issue.comment(reminderMarker + "\n\n" +
                "Hi @" + commenter + ", thanks for your interest in this issue!\n\n" +
                "This is a **Good First Issue** \u2014 if you'd like to work on it, " +
                "please comment `/assign` to get assigned.");
        LOG.info("Posted GFI assign reminder on {}#{}", repoFullName, issueNumber);
    }
}
