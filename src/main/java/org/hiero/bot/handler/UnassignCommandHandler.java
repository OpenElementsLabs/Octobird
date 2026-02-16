package org.hiero.bot.handler;

import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class UnassignCommandHandler implements EventHandler<IssueCommentEvent> {

    private static final Pattern UNASSIGN_PATTERN = Pattern.compile("(^|\\s)/unassign(\\s|$)", Pattern.CASE_INSENSITIVE);

    @Override
    public Class<IssueCommentEvent> eventType() {
        return IssueCommentEvent.class;
    }

    @Override
    public boolean matches(GitHubEventType event, GitHubAction action) {
        return event == GitHubEventType.ISSUE_COMMENT && action == GitHubAction.CREATED;
    }

    @Override
    public void handle(IssueCommentEvent commentEvent, GitHub gitHub, Map<String, Object> repoConfig) throws IOException {

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

        String body = commentEvent.comment().body();
        if (body == null || !UNASSIGN_PATTERN.matcher(body).find()) {
            return;
        }

        String username = commentEvent.comment().user().login();
        String repoFullName = commentEvent.repository().fullName();
        int issueNumber = commentEvent.issue().number();

        GHRepository repo = gitHub.getRepository(repoFullName);
        GHIssue ghIssue = repo.getIssue(issueNumber);

        // Check if commenter is currently assigned
        boolean isAssignee = ghIssue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(username));
        if (!isAssignee) {
            System.out.println("[unassign] " + username + " is not an assignee of #" + issueNumber);
            return;
        }

        // Check for duplicate unassign marker
        String marker = "<!-- unassign-requested:" + username + " -->";
        for (GHIssueComment c : ghIssue.listComments()) {
            if (c.getBody() != null && c.getBody().contains(marker)) {
                System.out.println("[unassign] Already unassigned previously: " + username + " on #" + issueNumber);
                return;
            }
        }

        // Remove assignee
        ghIssue.removeAssignees(gitHub.getUser(username));

        // Post confirmation with marker
        String confirmation = marker + "\n\n" +
                "@" + username + ", you've been unassigned from this issue.\n\n" +
                "Thanks for letting us know! If you'd like to work on something else, " +
                "feel free to browse our open issues.";
        ghIssue.comment(confirmation);

        System.out.println("[unassign] Unassigned " + username + " from " + repoFullName + "#" + issueNumber);
    }
}
