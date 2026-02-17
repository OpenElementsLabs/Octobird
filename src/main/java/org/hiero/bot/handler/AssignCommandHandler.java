package org.hiero.bot.handler;

import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class AssignCommandHandler implements EventHandler<IssueCommentEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AssignCommandHandler.class);
    private static final Pattern ASSIGN_PATTERN = Pattern.compile("/assign\\b");

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

        final String body = commentEvent.comment().body();
        if (body == null || !ASSIGN_PATTERN.matcher(body).find()) {
            return;
        }

        final String commenter = commentEvent.comment().user().login();
        final String repoFullName = commentEvent.repository().fullName();
        final int issueNumber = commentEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        final boolean alreadyAssigned = issue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(commenter));

        if (alreadyAssigned) {
            issue.comment("@" + commenter + " you are already assigned to this issue.");
            return;
        }

        issue.addAssignees(gitHub.getUser(commenter));
        issue.comment("@" + commenter + " has been assigned to this issue.");
        LOG.info("Assigned {} to {}#{}", commenter, repoFullName, issueNumber);
    }
}
