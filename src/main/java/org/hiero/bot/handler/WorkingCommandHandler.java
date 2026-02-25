package org.hiero.bot.handler;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.ReactionContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class WorkingCommandHandler extends AbstractEventHandler<IssueCommentEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(WorkingCommandHandler.class);

    public WorkingCommandHandler() {
        super(IssueCommentEvent.class,
                (event, action) -> event == GitHubEventType.ISSUE_COMMENT && action == GitHubAction.CREATED,
                repoConfig -> repoConfig.features().workingCommand());
    }

    @Override
    public void handle(final IssueCommentEvent commentEvent, final GitHub gitHub,
                       final RepoConfig repoConfig) throws IOException {

        // Skip bots
        if ("Bot".equals(commentEvent.comment().user().type())) {
            return;
        }

        final Pattern workingPattern = repoConfig.commands().compiledWorkingPattern();
        final String body = commentEvent.comment().body();
        if (body == null || !workingPattern.matcher(body).find()) {
            return;
        }

        final String username = commentEvent.comment().user().login();
        final String repoFullName = commentEvent.repository().fullName();
        final int issueNumber = commentEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue ghIssue = repo.getIssue(issueNumber);

        // Authorization: assignee (issue) or author (PR)
        boolean authorized = false;

        // Check if PR and user is author
        if (commentEvent.issue().hasPullRequest()) {
            if (username.equals(commentEvent.issue().user().login())) {
                authorized = true;
            }
        }

        // Check if user is assignee
        if (!authorized) {
            authorized = ghIssue.getAssignees().stream()
                    .anyMatch(u -> u.getLogin().equals(username));
        }

        if (!authorized) {
            LOG.debug("{} is not authorized on #{}", username, issueNumber);
            return;
        }

        // React with eyes emoji on the triggering comment
        final List<GHIssueComment> comments = ghIssue.getComments();
        GHIssueComment targetComment = null;
        for (final GHIssueComment c : comments) {
            if (body.equals(c.getBody())) {
                targetComment = c;
            }
        }
        if (targetComment != null) {
            targetComment.createReaction(ReactionContent.EYES);
        }

        LOG.info("Acknowledged /working from {} on {}#{}", username, repoFullName, issueNumber);
    }
}
