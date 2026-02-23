package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.PermissionChecker;
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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class GfiAssignCommandHandler implements EventHandler<IssueCommentEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(GfiAssignCommandHandler.class);
    private static final Pattern ASSIGN_PATTERN = Pattern.compile("/assign\\b");
    private static final String GOOD_FIRST_ISSUE_LABEL = "Good First Issue";
    private static final String REMINDER_MARKER = "<!-- GFI assign reminder -->";
    private static final int SPAM_USER_MAX_ASSIGNMENTS = 1;
    private static final int NORMAL_USER_MAX_ASSIGNMENTS = 2;

    private final SpamListLoader spamListLoader;
    private final PermissionChecker permissionChecker;
    private final IssueSearchHelper searchHelper;
    private final CommentMarkerChecker markerChecker;

    public GfiAssignCommandHandler(final SpamListLoader spamListLoader,
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
                       final Map<String, Object> repoConfig) throws IOException {

        // Skip bots
        if ("Bot".equals(commentEvent.comment().user().type())) {
            return;
        }

        final String repoFullName = commentEvent.repository().fullName();
        final int issueNumber = commentEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        // Only GFI issues
        final boolean hasGfiLabel = issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(GOOD_FIRST_ISSUE_LABEL::equalsIgnoreCase);
        if (!hasGfiLabel) {
            return;
        }

        final String body = commentEvent.comment().body();
        final boolean hasAssignCommand = body != null && ASSIGN_PATTERN.matcher(body).find();
        final String commenter = commentEvent.comment().user().login();

        if (hasAssignCommand) {
            handleAssignCommand(gitHub, repo, issue, commenter, repoFullName, issueNumber);
        } else {
            handleReminder(repo, issue, commenter, repoFullName, issueNumber);
        }
    }

    private void handleAssignCommand(final GitHub gitHub, final GHRepository repo, final GHIssue issue,
                                      final String commenter, final String repoFullName,
                                      final int issueNumber) throws IOException {
        // Already assigned?
        final boolean alreadyAssigned = issue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(commenter));
        if (alreadyAssigned) {
            issue.comment("@" + commenter + " you are already assigned to this issue.");
            return;
        }

        final boolean isSpam = spamListLoader.isSpamUser(gitHub, repoFullName, commenter);

        if (isSpam) {
            final int count = searchHelper.countOpenAssignments(gitHub, repoFullName, commenter);
            if (count >= SPAM_USER_MAX_ASSIGNMENTS) {
                issue.comment("Hi @" + commenter + ", this is the Assignment Bot.\n\n" +
                        "Your account currently has limited assignment privileges with a maximum of **" +
                        SPAM_USER_MAX_ASSIGNMENTS + " open assignment** at a time.\n\n" +
                        "You currently have " + count + " open issue(s) assigned. " +
                        "Please complete and merge your existing assignment before requesting a new one.");
                return;
            }
        } else {
            final int count = searchHelper.countOpenAssignments(gitHub, repoFullName, commenter);
            if (count >= NORMAL_USER_MAX_ASSIGNMENTS) {
                issue.comment("Hi @" + commenter + ", this is the Assignment Bot.\n\n" +
                        "Assigning you to this issue would exceed the limit of " +
                        NORMAL_USER_MAX_ASSIGNMENTS + " open assignments.\n\n" +
                        "Please resolve and merge your existing assigned issues before requesting new ones.");
                return;
            }
        }

        issue.addAssignees(gitHub.getUser(commenter));
        issue.comment("@" + commenter + " has been assigned to this issue.");
        LOG.info("Assigned {} to GFI {}#{}", commenter, repoFullName, issueNumber);
    }

    private void handleReminder(final GHRepository repo, final GHIssue issue, final String commenter,
                                 final String repoFullName, final int issueNumber) throws IOException {
        // Only post reminder if issue is unassigned
        if (!issue.getAssignees().isEmpty()) {
            return;
        }

        // Only for non-collaborators
        if (permissionChecker.isCollaborator(repo, commenter)) {
            return;
        }

        // Check duplicate marker
        if (markerChecker.hasMarker(issue, REMINDER_MARKER)) {
            return;
        }

        issue.comment(REMINDER_MARKER + "\n\n" +
                "Hi @" + commenter + ", thanks for your interest in this issue!\n\n" +
                "This is a **Good First Issue** \u2014 if you'd like to work on it, " +
                "please comment `/assign` to get assigned.");
        LOG.info("Posted GFI assign reminder on {}#{}", repoFullName, issueNumber);
    }
}
