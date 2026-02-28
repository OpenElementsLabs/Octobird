package org.hiero.bot.handler.impl;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.AbstractEventHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.PullRequestEvent;
import org.hiero.bot.util.MessageFormatter;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueSearchBuilder;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * When a PR that fixes a Good First Issue or beginner issue is merged, posts a recommendation
 * comment listing up to 5 open unassigned beginner (or GFI fallback) issues for the contributor
 * to tackle next.
 *
 * <p>Skips bots, non-merged closes, intermediate/advanced issues, and issues with no matching
 * difficulty label. Enabled via
 * {@link org.hiero.bot.config.FeaturesConfig#nextIssueRecommendation()}.
 */
public final class NextIssueRecommendationHandler extends AbstractEventHandler<PullRequestEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(NextIssueRecommendationHandler.class);

    private static final Pattern LINKED_ISSUE_PATTERN =
            Pattern.compile("(?i)(fixes|closes|resolves|fix|close|resolve)\\s+(?:[\\w-]+/[\\w-]+)?#(\\d+)");

    private static final int MAX_RECOMMENDATIONS = 5;
    private static final int SEARCH_LIMIT = 6; // fetch one extra to allow filtering the solved issue

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.PULL_REQUEST && action == GitHubAction.CLOSED;

    private static final Predicate<RepoConfig> FEATURE_CHECK =
            repoConfig -> repoConfig.features().nextIssueRecommendation();

    public NextIssueRecommendationHandler() {
        super(PullRequestEvent.class, MATCHER, FEATURE_CHECK);
    }

    @Override
    public void handle(final PullRequestEvent event, final ServiceRegistry registry,
                       final RepoConfig repoConfig) throws IOException {
        final GitHub gitHub = registry.getGitHub();

        // Only act on merged PRs
        if (!event.pullRequest().merged()) {
            return;
        }

        // Skip bots
        if (event.sender().isBot()) {
            return;
        }

        final String body = event.pullRequest().body();
        if (body == null) {
            return;
        }

        // Parse the first linked issue number from the PR body
        final Matcher matcher = LINKED_ISSUE_PATTERN.matcher(body);
        if (!matcher.find()) {
            LOG.debug("No linked issue found in merged PR #{}", event.number());
            return;
        }
        final int linkedIssueNumber = Integer.parseInt(matcher.group(2));

        final String repoFullName = event.repository().fullName();
        final int prNumber = event.number();
        final GHRepository repo = gitHub.getRepository(repoFullName);

        // Fetch the linked issue
        final GHIssue linkedIssue = repo.getIssue(linkedIssueNumber);
        final List<String> labelNames = linkedIssue.getLabels().stream()
                .map(GHLabel::getName)
                .map(String::toLowerCase)
                .toList();

        final String intermediateLabel = repoConfig.labels().intermediate().toLowerCase();
        final String advancedLabel = repoConfig.labels().advanced().toLowerCase();
        final String gfiLabel = repoConfig.labels().goodFirstIssue().toLowerCase();
        final String beginnerLabel = repoConfig.labels().beginner().toLowerCase();

        // Skip intermediate/advanced issues
        if (labelNames.contains(intermediateLabel) || labelNames.contains(advancedLabel)) {
            LOG.debug("Linked issue #{} is intermediate/advanced, skipping recommendation", linkedIssueNumber);
            return;
        }

        // Only proceed for beginner or GFI issues
        final boolean isBeginner = labelNames.contains(beginnerLabel);
        final boolean isGfi = labelNames.contains(gfiLabel);
        if (!isBeginner && !isGfi) {
            LOG.debug("Linked issue #{} has no beginner/GFI label, skipping recommendation", linkedIssueNumber);
            return;
        }

        // Search open unassigned beginner issues first, fall back to GFI
        List<GHIssue> recommendations = searchOpenUnassignedIssues(gitHub, repoFullName, beginnerLabel);
        boolean isFallback = false;
        if (recommendations.isEmpty()) {
            recommendations = searchOpenUnassignedIssues(gitHub, repoFullName, gfiLabel);
            isFallback = true;
        }

        // Remove the just-solved issue
        recommendations = recommendations.stream()
                .filter(i -> i.getNumber() != linkedIssueNumber)
                .limit(MAX_RECOMMENDATIONS)
                .toList();

        // Build and post recommendation comment
        final String completedLabelText = isBeginner ? "Beginner issue" : "Good First Issue";
        final String recommendedLabel = isFallback ? "Good First Issue" : "Beginner";
        final String marker = repoConfig.markers().nextIssueRecommendation();
        final String comment = buildRecommendationComment(marker, completedLabelText,
                recommendedLabel, isFallback, recommendations);

        final GHIssue prAsIssue = repo.getIssue(prNumber);
        prAsIssue.comment(comment);
        LOG.info("Posted next issue recommendation on {}#{} (linked issue: #{})",
                repoFullName, prNumber, linkedIssueNumber);
    }

    private List<GHIssue> searchOpenUnassignedIssues(final GitHub gitHub, final String repoFullName,
                                                     final String label) throws IOException {
        final List<GHIssue> results = new ArrayList<>();
        int count = 0;
        for (final GHIssue issue : gitHub.searchIssues()
                .q("repo:" + repoFullName + " is:issue is:open label:\"" + label + "\" no:assignee")
                .list()) {
            if (count >= SEARCH_LIMIT) {
                break;
            }
            results.add(issue);
            count++;
        }
        return results;
    }

    private static String buildRecommendationComment(final String marker,
                                                     final String completedLabelText,
                                                     final String recommendedLabel,
                                                     final boolean isFallback,
                                                     final List<GHIssue> recommendations) {
        final StringBuilder comment = new StringBuilder();
        comment.append(marker).append("\n\n");
        comment.append(MessageFormatter.format(
                "Nice work completing a {}!\n\nThank you for your contribution! "
                        + "We are excited to have you as part of our community.\n\n",
                completedLabelText));

        if (!recommendations.isEmpty()) {
            if (isFallback) {
                comment.append(MessageFormatter.format(
                        "Here are some **{}** issues at a similar level you might be interested in:\n\n",
                        recommendedLabel));
            } else {
                comment.append(MessageFormatter.format(
                        "Here are some issues labeled **{}** you might be interested in next:\n\n",
                        recommendedLabel));
            }
            int index = 1;
            for (final GHIssue issue : recommendations) {
                final String title = issue.getTitle()
                        .replace("[", "\\[").replace("]", "\\]")
                        .replace("(", "\\(").replace(")", "\\)");
                comment.append(MessageFormatter.format(
                        "{}. [{}]({})\n", index, title, issue.getHtmlUrl()));
                index++;
            }
        } else {
            comment.append(MessageFormatter.format(
                    "There are currently no open issues available at the {} level.\n\n"
                            + "Check back later for new issues to work on!",
                    completedLabelText));
        }

        comment.append("\n\nWe look forward to seeing more contributions from you!");
        return comment.toString();
    }
}
