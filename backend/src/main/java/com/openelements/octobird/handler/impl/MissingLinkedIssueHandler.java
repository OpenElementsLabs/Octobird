package com.openelements.octobird.handler.impl;

import com.openelements.octobird.config.RepoConfig;
import com.openelements.octobird.handler.AbstractEventHandler;
import com.openelements.octobird.handler.ServiceRegistry;
import com.openelements.octobird.model.GitHubAction;
import com.openelements.octobird.model.GitHubEventType;
import com.openelements.octobird.model.event.PullRequestEvent;
import com.openelements.octobird.util.CommentMarkerChecker;
import com.openelements.octobird.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Posts a one-time reminder comment on pull requests that do not reference a linked issue.
 * Checks for {@code Fixes #N}, {@code Closes #N}, or {@code Resolves #N} in the PR body.
 * Skips bots, already-merged PRs, and PRs that already received the reminder (marker-based).
 *
 * <p>Enabled via {@link com.openelements.octobird.config.FeaturesConfig#missingLinkedIssue()}.
 */
public final class MissingLinkedIssueHandler extends AbstractEventHandler<PullRequestEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MissingLinkedIssueHandler.class);

    private static final Pattern LINKED_ISSUE_PATTERN =
            Pattern.compile("(?i)(fixes|closes|resolves)\\s*:?\\s*#\\d+");

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.PULL_REQUEST
                    && (action == GitHubAction.OPENED || action == GitHubAction.EDITED
                    || action == GitHubAction.REOPENED);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().missingLinkedIssue();

    public MissingLinkedIssueHandler() {
        super(PullRequestEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final PullRequestEvent event, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();

        // Skip bots
        if (event.sender().isBot()) {
            return;
        }

        // Skip already-merged PRs
        if (event.pullRequest().merged()) {
            return;
        }

        // Check for a linked issue reference in the PR body
        final String body = event.pullRequest().body();
        if (body != null && LINKED_ISSUE_PATTERN.matcher(body).find()) {
            return;
        }

        final String repoFullName = event.repository().fullName();
        final int prNumber = event.number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        // PRs are addressable as issues in GitHub's API
        final GHIssue prAsIssue = repo.getIssue(prNumber);

        final String marker = repoConfig.markers().missingLinkedIssue();
        if (CommentMarkerChecker.hasMarker(prAsIssue, marker)) {
            LOG.debug("Missing linked issue comment already posted on {}#{}", repoFullName, prNumber);
            return;
        }

        prAsIssue.comment(MessageFormatter.format(
                "{}\n\nHi! It looks like this pull request does not reference a linked issue.\n\n" +
                        "Please add a closing reference to the related issue in the PR description " +
                        "using one of these keywords:\n\n" +
                        "- `Fixes #<issue-number>`\n" +
                        "- `Closes #<issue-number>`\n" +
                        "- `Resolves #<issue-number>`\n\n" +
                        "This helps automatically close the issue when the PR is merged.",
                marker));
        LOG.info("Posted missing linked issue reminder on {}#{}", repoFullName, prNumber);
    }
}
