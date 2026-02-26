package org.hiero.bot.handler.impl;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.AbstractEventHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.PullRequestEvent;
import org.hiero.bot.util.CommentMarkerChecker;
import org.hiero.bot.util.MessageFormatter;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHVerification;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Checks that all commits in a pull request are GPG-signed (verified). Posts a one-time comment
 * listing unverified commits when unsigned commits are found. Uses a fail-closed policy: if
 * pagination is truncated (more than 500 commits) and no unverified commits were detected in the
 * scanned portion, at least one is assumed to be unverified.
 *
 * <p>Sanitizes commit messages against Markdown injection and breaks {@code @mentions} with a
 * zero-width space. Skips bots. Enabled via
 * {@link org.hiero.bot.config.FeaturesConfig#verifiedCommits()}.
 */
public final class VerifiedCommitsHandler extends AbstractEventHandler<PullRequestEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(VerifiedCommitsHandler.class);

    private static final int MAX_COMMITS = 500;
    private static final int MAX_DISPLAY = 10;
    private static final Pattern MARKDOWN_SPECIAL_CHARS = Pattern.compile("[`*_~\\[\\]()]");

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.PULL_REQUEST
                    && (action == GitHubAction.OPENED || action == GitHubAction.SYNCHRONIZE);

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().verifiedCommits();

    public VerifiedCommitsHandler() {
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

        final String repoFullName = event.repository().fullName();
        final int prNumber = event.number();

        final GHRepository repo = gitHub.getRepository(repoFullName);
        final GHPullRequest ghPR = repo.getPullRequest(prNumber);

        // Collect unverified commits up to MAX_COMMITS
        final List<String> unverifiedShas = new ArrayList<>();
        final List<String> unverifiedMessages = new ArrayList<>();
        int checkedCount = 0;
        boolean truncated = false;

        for (final GHPullRequestCommitDetail commitDetail : ghPR.listCommits()) {
            if (checkedCount >= MAX_COMMITS) {
                truncated = true;
                break;
            }
            checkedCount++;
            final String sha = commitDetail.getSha();
            final GHCommit ghCommit = repo.getCommit(sha);
            final GHVerification verification = ghCommit.getCommitShortInfo().getVerification();
            if (verification == null || !verification.isVerified()) {
                unverifiedShas.add(sha);
                unverifiedMessages.add(commitDetail.getCommit().getMessage());
            }
        }

        // Fail-closed: if truncated with zero unverified found, assume at least 1
        int unverifiedCount = unverifiedShas.size();
        if (truncated && unverifiedCount == 0) {
            unverifiedCount = 1;
        }

        if (unverifiedCount == 0) {
            return;
        }

        // Check for existing marker to avoid duplicate comments
        final GHIssue prAsIssue = repo.getIssue(prNumber);
        final String marker = repoConfig.markers().verifiedCommits();
        if (CommentMarkerChecker.hasMarker(prAsIssue, marker)) {
            LOG.debug("Verified commits comment already posted on {}#{}", repoFullName, prNumber);
            return;
        }

        // Build the commit list (show first MAX_DISPLAY unverified commits)
        final StringBuilder commitList = new StringBuilder();
        final int displayCount = Math.min(MAX_DISPLAY, unverifiedShas.size());
        for (int i = 0; i < displayCount; i++) {
            final String sha = unverifiedShas.get(i).substring(0, 7);
            final String rawMsg = unverifiedMessages.get(i);
            final String firstLine = rawMsg != null ? rawMsg.split("\n")[0] : "No message";
            final String sanitized = sanitizeMarkdown(
                    firstLine.substring(0, Math.min(50, firstLine.length())));
            commitList.append("- `").append(sha).append("` ").append(sanitized).append("\n");
        }
        if (unverifiedShas.size() > MAX_DISPLAY) {
            commitList.append("- ...and ").append(unverifiedShas.size() - MAX_DISPLAY)
                    .append(" more\n");
        }
        if (truncated && unverifiedShas.isEmpty()) {
            commitList.append("- Unable to enumerate commits due to pagination limit.\n");
        }

        final String countText = truncated ? "at least " + unverifiedCount : String.valueOf(unverifiedCount);
        prAsIssue.comment(MessageFormatter.format(
                "{}\nHi, this is VerificationBot.\n" +
                        "Your pull request cannot be merged as it has **{} unverified commit(s)**:\n\n" +
                        "{}\n" +
                        "Please ensure all commits are GPG-signed:\n" +
                        "`git commit -S -s -m \"Your message here\"`\n\n" +
                        "Thank you for contributing!",
                marker, countText, commitList));
        LOG.info("Posted unverified commits comment on {}#{} ({} unverified)",
                repoFullName, prNumber, unverifiedCount);
    }

    private static String sanitizeMarkdown(final String input) {
        return MARKDOWN_SPECIAL_CHARS.matcher(input).replaceAll("\\\\$0")
                .replace("@", "@\u200b");
    }
}
