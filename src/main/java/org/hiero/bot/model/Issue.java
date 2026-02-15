package org.hiero.bot.model;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents a GitHub issue. Also used as the {@code issue} object inside
 * {@code issue_comment} webhook payloads (where it may reference a pull request).
 *
 * @param id                unique identifier of the issue
 * @param number            issue number within the repository
 * @param title             title of the issue
 * @param body              body text (Markdown), may be {@code null}
 * @param state             current state, e.g. {@code "open"} or {@code "closed"}
 * @param stateReason       reason for the current state (e.g. {@code "completed"}, {@code "not_planned"}), may be {@code null}
 * @param user              the user who created the issue, may be {@code null}
 * @param assignee          primary assignee, may be {@code null}
 * @param assignees         all users assigned to the issue
 * @param labels            labels attached to the issue
 * @param locked            whether the issue conversation is locked
 * @param authorAssociation author's association with the repository (e.g. {@code "OWNER"}, {@code "CONTRIBUTOR"}), may be {@code null}
 * @param hasPullRequest    {@code true} if this issue is actually a pull request (the {@code pull_request} key is present)
 * @param htmlUrl           URL of the issue on GitHub, may be {@code null}
 * @param createdAt         ISO 8601 creation timestamp, may be {@code null}
 * @param updatedAt         ISO 8601 last-updated timestamp, may be {@code null}
 * @param closedAt          ISO 8601 close timestamp, may be {@code null}
 * @see <a href="https://docs.github.com/en/rest/issues/issues#get-an-issue">GitHub REST API &ndash; Issues</a>
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#issues">GitHub Webhooks &ndash; issues event</a>
 */
public record Issue(long id, int number, String title, @Nullable String body, String state,
                    @Nullable String stateReason, @Nullable User user, @Nullable User assignee,
                    List<User> assignees, List<Label> labels, boolean locked, @Nullable String authorAssociation,
                    boolean hasPullRequest, @Nullable String htmlUrl, @Nullable String createdAt,
                    @Nullable String updatedAt, @Nullable String closedAt) {

    public Issue {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(assignees, "assignees must not be null");
        Objects.requireNonNull(labels, "labels must not be null");
        assignees = List.copyOf(assignees);
        labels = List.copyOf(labels);
    }
}
