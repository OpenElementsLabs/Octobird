package org.hiero.bot.model;

import org.jspecify.annotations.Nullable;

/**
 * Represents the head or base reference of a GitHub pull request.
 *
 * @param label combined label in the form {@code "owner:branch"} (e.g. {@code "alice:feature"}), may be {@code null}
 * @param ref   branch name (e.g. {@code "feature"} or {@code "main"}), may be {@code null}
 * @param sha   commit SHA at the tip of the branch, may be {@code null}
 * @param user  the owner of the fork or repository, may be {@code null}
 * @param repo  the repository that contains the branch, may be {@code null}
 * @see <a href="https://docs.github.com/en/rest/pulls/pulls#get-a-pull-request">GitHub REST API &ndash; Pull Requests (head/base objects)</a>
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#pull_request">GitHub Webhooks &ndash; pull_request event</a>
 */
public record PullRequestRef(@Nullable String label, @Nullable String ref, @Nullable String sha, @Nullable User user,
                              @Nullable Repository repo) {
}
