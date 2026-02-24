package org.hiero.bot.handler;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Pattern;

public class UnassignCommandHandler implements EventHandler<IssueCommentEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(UnassignCommandHandler.class);

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

        if (!repoConfig.features().unassignCommand()) {
            return;
        }

        // Skip PRs
        if (commentEvent.issue().hasPullRequest()) {
            return;
        }

        // Skip if issue is not open
        if (!"open".equals(commentEvent.issue().state())) {
            return;
        }

        // Skip bots
        if ("Bot".equals(commentEvent.comment().user().type())) {
            return;
        }

        final Pattern unassignPattern = repoConfig.commands().compiledUnassignPattern();
        final String body = commentEvent.comment().body();
        if (body == null || !unassignPattern.matcher(body).find()) {
            return;
        }

        final String username = commentEvent.comment().user().login();
        final String repoFullName = commentEvent.repository().fullName();
        final int issueNumber = commentEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue ghIssue = repo.getIssue(issueNumber);

        // Check if commenter is currently assigned
        final boolean isAssignee = ghIssue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(username));
        if (!isAssignee) {
            LOG.debug("{} is not an assignee of #{}", username, issueNumber);
            return;
        }

        // Check for duplicate unassign marker
        final String marker = repoConfig.markers().unassignPrefix() + username + " -->";
        for (final GHIssueComment c : ghIssue.listComments()) {
            if (c.getBody() != null && c.getBody().contains(marker)) {
                LOG.debug("Already unassigned previously: {} on #{}", username, issueNumber);
                return;
            }
        }

        // Remove assignee
        ghIssue.removeAssignees(gitHub.getUser(username));

        // Post confirmation with marker
        final String confirmation = marker + "\n\n" +
                "@" + username + ", you've been unassigned from this issue.\n\n" +
                "Thanks for letting us know! If you'd like to work on something else, " +
                "feel free to browse our open issues.";
        ghIssue.comment(confirmation);

        LOG.info("Unassigned {} from {}#{}", username, repoFullName, issueNumber);
    }
}
