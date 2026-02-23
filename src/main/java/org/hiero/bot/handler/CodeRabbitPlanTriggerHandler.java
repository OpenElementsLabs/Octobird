package org.hiero.bot.handler;

import org.hiero.bot.config.CommentMarkerChecker;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.IssuesEvent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CodeRabbitPlanTriggerHandler implements EventHandler<IssuesEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(CodeRabbitPlanTriggerHandler.class);
    private static final String MARKER = "<!-- CodeRabbit Plan Trigger -->";
    private static final Set<String> TRIGGER_LABELS = Set.of("beginner", "intermediate", "advanced");

    private final CommentMarkerChecker markerChecker;

    public CodeRabbitPlanTriggerHandler(final CommentMarkerChecker markerChecker) {
        this.markerChecker = Objects.requireNonNull(markerChecker, "markerChecker must not be null");
    }

    @Override
    public Class<IssuesEvent> eventType() {
        return IssuesEvent.class;
    }

    @Override
    public boolean matches(final GitHubEventType event, final GitHubAction action) {
        return event == GitHubEventType.ISSUES && action == GitHubAction.LABELED;
    }

    @Override
    public void handle(final IssuesEvent issuesEvent, final GitHub gitHub,
                       final Map<String, Object> repoConfig) throws IOException {

        final var label = issuesEvent.label();
        if (label == null) {
            return;
        }

        final String labelName = label.name().toLowerCase();
        if (!TRIGGER_LABELS.contains(labelName)) {
            return;
        }

        final String repoFullName = issuesEvent.repository().fullName();
        final int issueNumber = issuesEvent.issue().number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHIssue issue = repo.getIssue(issueNumber);

        if (markerChecker.hasMarker(issue, MARKER)) {
            LOG.debug("CodeRabbit plan already triggered for {}#{}", repoFullName, issueNumber);
            return;
        }

        issue.comment(MARKER + "\n@coderabbitai plan");
        LOG.info("Triggered CodeRabbit plan for {}#{} (label: {})", repoFullName, issueNumber, label.name());
    }
}
