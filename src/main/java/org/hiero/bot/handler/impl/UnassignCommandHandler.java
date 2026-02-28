package org.hiero.bot.handler.impl;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.IssueCommandTriggerHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.hiero.bot.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Handles the {@code /unassign} command posted in an issue comment. When a user who is currently
 * assigned to an open issue posts {@code /unassign}, this handler removes them and posts a
 * confirmation comment with an HTML marker to prevent duplicate processing.
 *
 * <p>Skips pull requests, closed issues, and bot comments. Enabled via
 * {@link org.hiero.bot.config.FeaturesConfig#unassignCommand()}.
 */
public final class UnassignCommandHandler extends IssueCommandTriggerHandler {

    private static final Logger LOG = LoggerFactory.getLogger(UnassignCommandHandler.class);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().unassignCommand();

    public UnassignCommandHandler() {
        super(FEATURE_CHECK);
    }

    @Override
    protected Pattern commandPattern(final RepoConfig repoConfig) {
        return repoConfig.commands().compiledUnassignPattern();
    }

    @Override
    protected void handleCommand(final IssueCommentEvent commentEvent, final ServiceRegistry registry,
                                 final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();

        // Skip PRs
        if (commentEvent.issue().hasPullRequest()) {
            return;
        }

        // Skip if issue is not open
        if (!"open".equals(commentEvent.issue().state())) {
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
        ghIssue.comment(MessageFormatter.format(
                "{}\n\n@{}, you've been unassigned from this issue.\n\n" +
                        "Thanks for letting us know! If you'd like to work on something else, " +
                        "feel free to browse our open issues.",
                marker, username));

        LOG.info("Unassigned {} from {}#{}", username, repoFullName, issueNumber);
    }
}
