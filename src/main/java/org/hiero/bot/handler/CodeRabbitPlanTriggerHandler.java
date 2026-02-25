package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssuesEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class CodeRabbitPlanTriggerHandler extends AbstractEventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRabbitPlanTriggerHandler.class);

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.ISSUES && action == GitHubAction.LABELED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().codeRabbitPlanTrigger();

    public CodeRabbitPlanTriggerHandler() {
        super(IssuesEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final IssuesEvent issuesEvent, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();

        final var label = issuesEvent.label();
        if (label == null) {
            return;
        }

        final String labelName = label.name().toLowerCase();
        if (!repoConfig.codeRabbit().triggerLabels().contains(labelName)) {
            return;
        }

        final String repoFullName = issuesEvent.repository().fullName();
        final int issueNumber = issuesEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        final String marker = repoConfig.markers().codeRabbitPlanTrigger();
        if (CommentMarkerChecker.hasMarker(issue, marker)) {
            LOG.debug("CodeRabbit plan already triggered for {}#{}", repoFullName, issueNumber);
            return;
        }

        issue.comment(MessageFormatter.format("{}\n@coderabbitai plan", marker));
        LOG.info("Triggered CodeRabbit plan for {}#{} (label: {})", repoFullName, issueNumber, label.name());
    }
}
