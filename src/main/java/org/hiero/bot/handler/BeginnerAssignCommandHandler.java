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
import java.util.Objects;
import java.util.regex.Pattern;

public class BeginnerAssignCommandHandler implements EventHandler<IssueCommentEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(BeginnerAssignCommandHandler.class);

    private final SpamListLoader spamListLoader;
    private final PermissionChecker permissionChecker;
    private final IssueSearchHelper searchHelper;
    private final CommentMarkerChecker markerChecker;

    public BeginnerAssignCommandHandler(final SpamListLoader spamListLoader,
                                        final PermissionChecker permissionChecker,
                                        final IssueSearchHelper searchHelper,
                                        final CommentMarkerChecker markerChecker) {
        this.spamListLoader = Objects.requireNonNull(spamListLoader, "spamListLoader must not be null");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker must not be null");
        this.searchHelper = Objects.requireNonNull(searchHelper, "searchHelper must not be null");
        this.markerChecker = Objects.requireNonNull(markerChecker, "markerChecker must not be null");
    }

    @Override
    public Class<IssueCommentEvent> eventType() {
        return IssueCommentEvent.class;
    }

    @Override
    public boolean matches(final GitHubEventType event, final GitHubAction action) {
        return event == GitHubEventType.ISSUE_COMMENT && action == GitHubAction.CREATED;
    }

    @Override
    public void handle(final IssueCommentEvent commentEvent, final GitHub gitHub,
                       final RepoConfig repoConfig) throws IOException {

        if (!repoConfig.features().beginnerAssignCommand()) {
            return;
        }

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
        if (!permissionChecker.isExemptFromGuard(repo, commenter)) {
            final int closedGfi = searchHelper.countClosedIssuesByLabel(
                    gitHub, repoFullName, commenter, gfiLabel);
            if (closedGfi < requiredGfiCount) {
                final String userMarker = gfiGuardMarker + " @" + commenter;
                if (!markerChecker.hasMarker(issue, userMarker)) {
                    issue.comment(userMarker + "\n\n" +
                            "Hi @" + commenter + ", this is the Assignment Bot.\n\n" +
                            "This is a **beginner** issue that requires at least **" +
                            requiredGfiCount + "** completed Good First Issue(s).\n\n" +
                            "You currently have **" + closedGfi + "** completed Good First Issue(s). " +
                            "Please complete a Good First Issue before requesting a beginner issue.");
                }
                return;
            }
        }

        // Spam users are completely blocked from beginner issues
        final String spamListPath = repoConfig.paths().spamList();
        final boolean isSpam = spamListLoader.isSpamUser(gitHub, repoFullName, commenter, spamListPath);
        if (isSpam) {
            issue.comment("Hi @" + commenter + ", this is the Assignment Bot.\n\n" +
                    "Your account currently has limited assignment privileges. " +
                    "You may only be assigned to issues labeled **Good First Issue**.\n\n" +
                    "Please complete and merge your assigned Good First Issue " +
                    "to have restrictions lifted.");
            return;
        }

        // Already assigned?
        final boolean alreadyAssigned = issue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(commenter));
        if (alreadyAssigned) {
            issue.comment("@" + commenter + " you are already assigned to this issue.");
            return;
        }

        // Assignment limit check
        final int normalMax = repoConfig.assignmentLimits().normalUserMax();
        final int count = searchHelper.countOpenAssignments(gitHub, repoFullName, commenter);
        if (count >= normalMax) {
            issue.comment("Hi @" + commenter + ", this is the Assignment Bot.\n\n" +
                    "Assigning you to this issue would exceed the limit of " +
                    normalMax + " open assignments.\n\n" +
                    "Please resolve and merge your existing assigned issues before requesting new ones.");
            return;
        }

        issue.addAssignees(gitHub.getUser(commenter));
        issue.comment("@" + commenter + " has been assigned to this issue.");
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
        if (permissionChecker.isCollaborator(repo, commenter)) {
            return;
        }

        final String reminderMarker = repoConfig.markers().beginnerReminder();

        // Check duplicate marker
        if (markerChecker.hasMarker(issue, reminderMarker)) {
            return;
        }

        issue.comment(reminderMarker + "\n\n" +
                "Hi @" + commenter + ", thanks for your interest in this issue!\n\n" +
                "This is a **beginner** issue \u2014 if you'd like to work on it, " +
                "please comment `/assign` to get assigned.");
        LOG.info("Posted beginner assign reminder on {}#{}", repoFullName, issueNumber);
    }
}
