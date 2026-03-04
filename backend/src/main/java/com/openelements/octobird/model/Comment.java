package com.openelements.octobird.model;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a comment on a GitHub issue or pull request.
 *
 * @param id                unique identifier of the comment
 * @param body              comment body text (Markdown), may be {@code null}
 * @param user              the user who authored the comment
 * @param authorAssociation author's association with the repository (e.g. {@code "OWNER"}, {@code "CONTRIBUTOR"}), may be {@code null}
 * @param htmlUrl           URL of the comment on GitHub, may be {@code null}
 * @param createdAt         ISO 8601 creation timestamp, may be {@code null}
 * @param updatedAt         ISO 8601 last-updated timestamp, may be {@code null}
 * @see <a href="https://docs.github.com/en/rest/issues/comments#get-an-issue-comment">GitHub REST API &ndash; Issue Comments</a>
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#issue_comment">GitHub Webhooks &ndash; issue_comment event</a>
 */
public record Comment(long id, @Nullable String body, User user, @Nullable String authorAssociation,
                       @Nullable String htmlUrl, @Nullable String createdAt, @Nullable String updatedAt) {

    public Comment {
        Objects.requireNonNull(user, "user must not be null");
    }
}
