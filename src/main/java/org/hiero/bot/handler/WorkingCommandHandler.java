package org.hiero.bot.handler;

import org.hiero.bot.model.event.IssueCommentEvent;
import org.hiero.bot.model.event.WebhookEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.ReactionContent;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WorkingCommandHandler implements EventHandler {

    private static final Pattern WORKING_PATTERN = Pattern.compile("(^|\\s)/working(\\s|$)", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean matches(String event, String action) {
        return "issue_comment".equals(event) && "created".equals(action);
    }

    @Override
    public void handle(WebhookEvent event, GitHub gitHub, Map<String, Object> repoConfig) throws IOException {
        IssueCommentEvent commentEvent = (IssueCommentEvent) event;

        // Skip bots
        if ("Bot".equals(commentEvent.comment().user().type())) {
            return;
        }

        String body = commentEvent.comment().body();
        if (body == null || !WORKING_PATTERN.matcher(body).find()) {
            return;
        }

        String username = commentEvent.comment().user().login();
        String repoFullName = commentEvent.repository().fullName();
        int issueNumber = commentEvent.issue().number();

        GHRepository repo = gitHub.getRepository(repoFullName);
        GHIssue ghIssue = repo.getIssue(issueNumber);

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
            System.out.println("[working] " + username + " is not authorized on #" + issueNumber);
            return;
        }

        // React with eyes emoji on the triggering comment
        List<GHIssueComment> comments = ghIssue.getComments();
        GHIssueComment targetComment = null;
        for (GHIssueComment c : comments) {
            if (body.equals(c.getBody())) {
                targetComment = c;
            }
        }
        if (targetComment != null) {
            targetComment.createReaction(ReactionContent.EYES);
        }

        System.out.println("[working] Acknowledged /working from " + username + " on " + repoFullName + "#" + issueNumber);
    }
}
