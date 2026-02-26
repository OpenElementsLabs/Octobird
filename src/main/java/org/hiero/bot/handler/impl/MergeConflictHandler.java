package org.hiero.bot.handler.impl;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.AbstractEventHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.PullRequestEvent;
import org.hiero.bot.util.CommentMarkerChecker;
import org.hiero.bot.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Detects merge conflicts in pull requests and posts a one-time help comment. Because GitHub
 * computes mergeability asynchronously, this handler retries up to 10 times with a 2-second
 * delay while the state is {@code "unknown"}.
 *
 * <p>Skips bots and draft PRs. Enabled via
 * {@link org.hiero.bot.config.FeaturesConfig#mergeConflict()}.
 */
public final class MergeConflictHandler extends AbstractEventHandler<PullRequestEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MergeConflictHandler.class);

    private static final int MAX_RETRIES = 10;
    private static final long RETRY_SLEEP_MS = 2_000L;
    private static final String STATE_UNKNOWN = "unknown";
    private static final String STATE_DIRTY = "dirty";

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.PULL_REQUEST
                    && (action == GitHubAction.OPENED || action == GitHubAction.SYNCHRONIZE
                            || action == GitHubAction.REOPENED);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().mergeConflict();

    public MergeConflictHandler() {
        super(PullRequestEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final PullRequestEvent event, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();

        // Skip bots
        if ("Bot".equals(event.sender().type())) {
            return;
        }

        // Skip draft PRs
        if (event.pullRequest().draft()) {
            return;
        }

        final String repoFullName = event.repository().fullName();
        final int prNumber = event.number();

        final GHRepository repo = gitHub.getRepository(repoFullName);

        // Retry until mergeableState is determined (not "unknown")
        GHPullRequest ghPR = repo.getPullRequest(prNumber);
        String mergeableState = ghPR.getMergeableState();

        for (int attempt = 0; attempt < MAX_RETRIES && STATE_UNKNOWN.equals(mergeableState); attempt++) {
            LOG.debug("PR #{} mergeableState is 'unknown', retrying ({}/{})...",
                    prNumber, attempt + 1, MAX_RETRIES);
            try {
                Thread.sleep(RETRY_SLEEP_MS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            ghPR = repo.getPullRequest(prNumber);
            mergeableState = ghPR.getMergeableState();
        }

        if (STATE_UNKNOWN.equals(mergeableState)) {
            LOG.warn("PR #{} mergeableState still 'unknown' after {} retries, skipping",
                    prNumber, MAX_RETRIES);
            return;
        }

        if (!STATE_DIRTY.equals(mergeableState)) {
            return;
        }

        LOG.debug("Merge conflict detected in {}#{}", repoFullName, prNumber);

        // PRs are addressable as issues in GitHub's API
        final GHIssue prAsIssue = repo.getIssue(prNumber);
        final String marker = repoConfig.markers().mergeConflict();
        if (CommentMarkerChecker.hasMarker(prAsIssue, marker)) {
            LOG.debug("Merge conflict comment already posted on {}#{}", repoFullName, prNumber);
            return;
        }

        prAsIssue.comment(MessageFormatter.format(
                "{}\nHi, this is MergeConflictBot.\n" +
                        "Your pull request cannot be merged because it contains **merge conflicts**.\n\n" +
                        "Please resolve these conflicts locally and push the changes.\n\n" +
                        "### Quick Fix for CHANGELOG.md Conflicts\n" +
                        "If your conflict is only in **CHANGELOG.md**, you can resolve it easily " +
                        "using the GitHub web editor:\n" +
                        "1. Click on the \"Resolve conflicts\" button in the PR\n" +
                        "2. Accept both changes (keep both changelog entries)\n" +
                        "3. Click \"Mark as resolved\"\n" +
                        "4. Commit the merge\n\n" +
                        "Thank you for contributing!",
                marker));
        LOG.info("Posted merge conflict comment on {}#{}", repoFullName, prNumber);
    }
}
