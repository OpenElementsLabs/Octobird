package org.hiero.bot.handler.impl;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.AbstractEventHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssuesEvent;
import org.hiero.bot.util.CommentMarkerChecker;
import org.hiero.bot.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Notifies the configured GFI support team when an issue is labeled as a Good First Issue
 * candidate, prompting the team to review whether the issue should be promoted to a full
 * Good First Issue.
 *
 * <p>Posts a single @mention comment on the issue and uses an HTML marker to prevent duplicate
 * notifications. The candidate label name is configurable via
 * {@link org.hiero.bot.config.LabelsConfig#gfiCandidate()} and the team to notify is configurable
 * via {@link org.hiero.bot.config.TeamsConfig#gfiCandidateTeam()}. Enabled via
 * {@link org.hiero.bot.config.FeaturesConfig#gfiCandidateNotification()}.
 */
public final class GfiCandidateNotificationHandler extends AbstractEventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(GfiCandidateNotificationHandler.class);

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.ISSUES && action == GitHubAction.LABELED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().gfiCandidateNotification();

    public GfiCandidateNotificationHandler() {
        super(IssuesEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final IssuesEvent event, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        final var label = event.label();
        if (label == null) {
            return;
        }

        final String gfiCandidateLabel = repoConfig.labels().gfiCandidate();
        if (!label.name().equalsIgnoreCase(gfiCandidateLabel)) {
            return;
        }

        final String teamMention = repoConfig.teams().gfiCandidateTeam();
        if (teamMention.isBlank()) {
            LOG.debug("GFI candidate notification triggered for {}#{} but no team configured",
                    event.repository().fullName(), event.issue().number());
            return;
        }

        final GitHub gitHub = registry.getGitHub();
        final String repoFullName = event.repository().fullName();
        final int issueNumber = event.issue().number();
        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        final String marker = repoConfig.markers().gfiCandidateNotification();
        if (CommentMarkerChecker.hasMarker(issue, marker)) {
            LOG.debug("GFI candidate notification already posted for {}#{}", repoFullName, issueNumber);
            return;
        }

        final String comment = MessageFormatter.format(
                "{}\n:wave: Hello Team :wave:\n{}\n\n"
                        + "A new Good First Issue Candidate has been created. "
                        + "Please review it and confirm whether it should be labeled as a Good First Issue.\n\n"
                        + "Repository: {}\nIssue: #{} - {}\n\n"
                        + "Best Regards,\nAutomated Notification System",
                marker, teamMention, repoFullName, issueNumber, event.issue().title());

        issue.comment(comment);
        LOG.info("Posted GFI candidate notification on {}#{}", repoFullName, issueNumber);
    }
}
