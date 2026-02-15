package org.hiero.bot.model;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents a GitHub pull request.
 *
 * @param id                 unique identifier of the pull request
 * @param number             pull request number within the repository
 * @param title              title of the pull request
 * @param body               body text (Markdown), may be {@code null}
 * @param state              current state, e.g. {@code "open"} or {@code "closed"}
 * @param draft              whether the pull request is a draft
 * @param merged             whether the pull request has been merged
 * @param user               the user who created the pull request
 * @param assignee           primary assignee, may be {@code null}
 * @param assignees          all users assigned to the pull request
 * @param requestedReviewers users requested for review
 * @param labels             labels attached to the pull request
 * @param head               the head branch reference (source), may be {@code null}
 * @param base               the base branch reference (target), may be {@code null}
 * @param mergedBy           the user who merged the pull request, may be {@code null}
 * @param authorAssociation  author's association with the repository (e.g. {@code "OWNER"}, {@code "CONTRIBUTOR"}), may be {@code null}
 * @param htmlUrl            URL of the pull request on GitHub, may be {@code null}
 * @param createdAt          ISO 8601 creation timestamp, may be {@code null}
 * @param updatedAt          ISO 8601 last-updated timestamp, may be {@code null}
 * @param closedAt           ISO 8601 close timestamp, may be {@code null}
 * @param mergedAt           ISO 8601 merge timestamp, may be {@code null}
 * @param commits            number of commits in the pull request
 * @param additions          number of added lines
 * @param deletions          number of deleted lines
 * @param changedFiles       number of changed files
 * @see <a href="https://docs.github.com/en/rest/pulls/pulls#get-a-pull-request">GitHub REST API &ndash; Pull Requests</a>
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#pull_request">GitHub Webhooks &ndash; pull_request event</a>
 */
public record PullRequest(long id, int number, String title, @Nullable String body, String state, boolean draft,
                           boolean merged, User user, @Nullable User assignee, List<User> assignees,
                           List<User> requestedReviewers, List<Label> labels, @Nullable PullRequestRef head,
                           @Nullable PullRequestRef base, @Nullable User mergedBy,
                           @Nullable String authorAssociation, @Nullable String htmlUrl, @Nullable String createdAt,
                           @Nullable String updatedAt, @Nullable String closedAt, @Nullable String mergedAt,
                           int commits, int additions, int deletions, int changedFiles) {

    public PullRequest {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(assignees, "assignees must not be null");
        Objects.requireNonNull(requestedReviewers, "requestedReviewers must not be null");
        Objects.requireNonNull(labels, "labels must not be null");
        assignees = List.copyOf(assignees);
        requestedReviewers = List.copyOf(requestedReviewers);
        labels = List.copyOf(labels);
    }
}
