package com.openelements.octobird.handler.impl;

import com.openelements.octobird.config.RepoConfig;
import com.openelements.octobird.handler.AbstractEventHandler;
import com.openelements.octobird.handler.ServiceRegistry;
import com.openelements.octobird.model.GitHubAction;
import com.openelements.octobird.model.GitHubEventType;
import com.openelements.octobird.model.event.WorkflowRunEvent;
import com.openelements.octobird.util.CommentMarkerChecker;
import com.openelements.octobird.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Notifies contributors when a GitHub Actions workflow run fails on their pull request. Posts a
 * one-time help comment with documentation links. Looks up associated PRs from the workflow run
 * payload and falls back to a head-branch search if none are listed.
 *
 * <p>Enabled via {@link com.openelements.octobird.config.FeaturesConfig#workflowFailureNotification()}.
 */
public final class WorkflowFailureNotificationHandler extends AbstractEventHandler<WorkflowRunEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowFailureNotificationHandler.class);

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.WORKFLOW_RUN && action == GitHubAction.COMPLETED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().workflowFailureNotification();

    public WorkflowFailureNotificationHandler() {
        super(WorkflowRunEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final WorkflowRunEvent event, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        // Only act on failed workflow runs
        if (!"failure".equals(event.workflowRun().conclusion())) {
            return;
        }

        if (event.repository() == null) {
            return;
        }

        final String repoFullName = event.repository().fullName();

        // Collect PR numbers from the payload first (no API call required)
        final List<Integer> prNumbers = new ArrayList<>(event.workflowRun().pullRequestNumbers());

        // Fall back to head-branch search only if payload contains no PR numbers
        final String headBranch = event.workflowRun().headBranch();
        if (prNumbers.isEmpty() && headBranch == null) {
            LOG.debug("No PRs found for failed workflow run {} in {}",
                    event.workflowRun().id(), repoFullName);
            return;
        }

        final GitHub gitHub = registry.getGitHub();
        final GHRepository repo = gitHub.getRepository(repoFullName);

        if (prNumbers.isEmpty()) {
            for (final GHPullRequest pr : repo.queryPullRequests()
                    .state(GHIssueState.OPEN)
                    .head(headBranch)
                    .list()) {
                prNumbers.add(pr.getNumber());
            }
        }

        if (prNumbers.isEmpty()) {
            LOG.debug("No PRs found for failed workflow run {} in {}",
                    event.workflowRun().id(), repoFullName);
            return;
        }

        final String marker = repoConfig.markers().workflowFailureNotification();

        for (final int prNumber : prNumbers) {
            // PRs are addressable as issues in GitHub's API
            final GHIssue prAsIssue = repo.getIssue(prNumber);
            if (CommentMarkerChecker.hasMarker(prAsIssue, marker)) {
                LOG.debug("Workflow failure comment already posted on {}#{}", repoFullName, prNumber);
                continue;
            }

            prAsIssue.comment(MessageFormatter.format(
                    "{}\nHi, this is WorkflowBot.\n" +
                            "Your pull request cannot be merged as it is not passing all workflow checks.\n\n" +
                            "Please click on each failing check to review the logs and resolve issues " +
                            "so all checks pass.\n\n" +
                            "To help you:\n" +
                            "- Review the workflow run logs for details on what failed\n" +
                            "- Ensure your commits are GPG-signed\n" +
                            "- Ensure your CHANGELOG entry is correct\n" +
                            "- Resolve any merge conflicts\n\n" +
                            "Thank you for contributing!",
                    marker));
            LOG.info("Posted workflow failure notification on {}#{}", repoFullName, prNumber);
        }
    }
}
