package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.IssueSearchHelper;
import org.hiero.bot.config.MentorRosterLoader;
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
import java.util.Map;
import java.util.Objects;

public class MentorAssignmentHandler implements EventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MentorAssignmentHandler.class);
    private static final String GOOD_FIRST_ISSUE_LABEL = "Good First Issue";
    private static final String MARKER = "<!-- Mentor Assignment Bot -->";

    private final MentorRosterLoader rosterLoader;
    private final IssueSearchHelper searchHelper;
    private final CommentMarkerChecker markerChecker;

    public MentorAssignmentHandler(final MentorRosterLoader rosterLoader,
                                   final IssueSearchHelper searchHelper,
                                   final CommentMarkerChecker markerChecker) {
        this.rosterLoader = Objects.requireNonNull(rosterLoader, "rosterLoader must not be null");
        this.searchHelper = Objects.requireNonNull(searchHelper, "searchHelper must not be null");
        this.markerChecker = Objects.requireNonNull(markerChecker, "markerChecker must not be null");
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
        final boolean hasGfiLabel = issue.getLabels().stream()
                .map(GHLabel::getName)
                .anyMatch(GOOD_FIRST_ISSUE_LABEL::equalsIgnoreCase);
        if (!hasGfiLabel) {
            return;
        }

        // Check duplicate marker
        if (markerChecker.hasMarker(issue, MARKER)) {
            LOG.debug("Mentor already assigned for {}#{}", repoFullName, issueNumber);
            return;
        }

        // Only for new contributors (no merged PRs)
        if (!searchHelper.hasNoMergedPullRequests(gitHub, repoFullName, assigneeLogin)) {
            LOG.debug("{} already has merged PRs, skipping mentor assignment", assigneeLogin);
            return;
        }

        // Select mentor from roster
        final List<String> roster = rosterLoader.loadRoster(gitHub, repoFullName);
        final String mentor = rosterLoader.selectMentor(roster);
        if (mentor == null) {
            LOG.debug("No mentors available for {}", repoFullName);
            return;
        }

        final String comment = MARKER + "\n\n" +
                "Welcome @" + assigneeLogin + "! \uD83D\uDC4B This is your first contribution — exciting!\n\n" +
                "@" + mentor + " has been assigned as your mentor for this issue. " +
                "Feel free to ask them any questions as you work through it.\n\n" +
                "Good luck and happy coding!";
        issue.comment(comment);
        LOG.info("Assigned mentor {} to newcomer {} on {}#{}", mentor, assigneeLogin, repoFullName, issueNumber);
    }
}
