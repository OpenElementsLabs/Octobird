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
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Notifies configured teams when an issue is labeled with the P0 label, indicating a critical
 * priority issue that requires immediate attention.
 *
 * <p>Posts a single @mention comment on the issue and uses an HTML marker to prevent duplicate
 * notifications. The P0 label name is configurable via
 * {@link org.hiero.bot.config.LabelsConfig#p0()} and the teams to notify are configurable via
 * {@link org.hiero.bot.config.TeamsConfig#p0Teams()}. Enabled via
 * {@link org.hiero.bot.config.FeaturesConfig#p0IssueAlarm()}.
 */
public final class P0IssueAlarmHandler extends AbstractEventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(P0IssueAlarmHandler.class);

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.ISSUES && action == GitHubAction.LABELED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().p0IssueAlarm();

    public P0IssueAlarmHandler() {
        super(IssuesEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final IssuesEvent event, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        final var label = event.label();
        if (label == null) {
            return;
        }

        final String p0Label = repoConfig.labels().p0();
        if (!label.name().equalsIgnoreCase(p0Label)) {
            return;
        }

        final List<String> teams = repoConfig.teams().p0Teams();
        if (teams.isEmpty()) {
            LOG.debug("P0 issue alarm triggered for {}#{} but no teams configured",
                    event.repository().fullName(), event.issue().number());
            return;
        }

        final GitHub gitHub = registry.getGitHub();
        final String repoFullName = event.repository().fullName();
        final int issueNumber = event.issue().number();
        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        final String marker = repoConfig.markers().p0IssueAlarm();
        if (CommentMarkerChecker.hasMarker(issue, marker)) {
            LOG.debug("P0 alarm already posted for {}#{}", repoFullName, issueNumber);
            return;
        }

        final String teamMentions = String.join(" ", teams);
        final String comment = MessageFormatter.format(
                "{}\n:rotating_light: Attention Team :rotating_light:\n{}\n\n"
                        + "A new P0 issue has been created: #{} - {}\n"
                        + "Please prioritize this issue accordingly.\n\n"
                        + "Best Regards,\nAutomated Notification System",
                marker, teamMentions, issueNumber, event.issue().title());

        issue.comment(comment);
        LOG.info("Posted P0 alarm on {}#{}", repoFullName, issueNumber);
    }
}
