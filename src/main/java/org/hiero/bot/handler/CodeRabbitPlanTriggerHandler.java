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

public class CodeRabbitPlanTriggerHandler extends AbstractEventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRabbitPlanTriggerHandler.class);

    public CodeRabbitPlanTriggerHandler() {
        super(IssuesEvent.class, (event, action) -> event == GitHubEventType.ISSUES && action == GitHubAction.LABELED);
    }

    @Override
    public void handle(final IssuesEvent issuesEvent, final GitHub gitHub,
                       final RepoConfig repoConfig) throws IOException {

        if (!repoConfig.features().codeRabbitPlanTrigger()) {
            return;
        }

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

        issue.comment(marker + "\n@coderabbitai plan");
        LOG.info("Triggered CodeRabbit plan for {}#{} (label: {})", repoFullName, issueNumber, label.name());
    }
}
