package org.hiero.bot.model.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hiero.bot.model.Comment;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.Installation;
import org.hiero.bot.model.Issue;
import org.hiero.bot.model.Label;
import org.hiero.bot.model.PullRequest;
import org.hiero.bot.model.PullRequestRef;
import org.hiero.bot.model.Repository;
import org.hiero.bot.model.User;
import org.hiero.bot.model.event.IssueCommentEvent;
import org.hiero.bot.model.event.IssuesEvent;
import org.hiero.bot.model.event.PullRequestEvent;
import org.hiero.bot.model.event.WebhookEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Jackson-based implementation of {@link WebhookParser}. This is the only class in the
 * {@code model} package that depends on Jackson; all parsed data is returned as plain Java records.
 *
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads">GitHub Webhooks &ndash; Webhook events and payloads</a>
 */
public class JacksonWebhookParser implements WebhookParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public WebhookEvent parse(GitHubEventType eventType, String payload) {
        try {
            JsonNode root = MAPPER.readTree(payload);
            GitHubAction action = GitHubAction.fromWebhookName(text(root, "action"));
            return switch (eventType) {
                case ISSUE_COMMENT -> parseIssueCommentEvent(root, action);
                case ISSUES -> parseIssuesEvent(root, action);
                case PULL_REQUEST -> parsePullRequestEvent(root, action);
            };
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse webhook payload for event: " + eventType, e);
        }
    }

    private IssueCommentEvent parseIssueCommentEvent(JsonNode root, GitHubAction action) {
        return new IssueCommentEvent(
                action,
                parseComment(root.path("comment")),
                parseIssue(root.path("issue")),
                parseRepository(root.path("repository")),
                parseUser(root.path("sender")),
                parseInstallation(root.path("installation"))
        );
    }

    private IssuesEvent parseIssuesEvent(JsonNode root, GitHubAction action) {
        JsonNode assigneeNode = root.path("assignee");
        JsonNode labelNode = root.path("label");
        return new IssuesEvent(
                action,
                parseIssue(root.path("issue")),
                assigneeNode.isMissingNode() || assigneeNode.isNull() ? null : parseUser(assigneeNode),
                labelNode.isMissingNode() || labelNode.isNull() ? null : parseLabel(labelNode),
                parseRepository(root.path("repository")),
                parseUser(root.path("sender")),
                parseInstallation(root.path("installation"))
        );
    }

    private PullRequestEvent parsePullRequestEvent(JsonNode root, GitHubAction action) {
        return new PullRequestEvent(
                action,
                root.path("number").asInt(),
                parsePullRequest(root.path("pull_request")),
                parseRepository(root.path("repository")),
                parseUser(root.path("sender")),
                parseInstallation(root.path("installation"))
        );
    }

    private @Nullable User parseUser(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new User(
                node.path("id").asLong(),
                text(node, "login"),
                text(node, "type"),
                text(node, "avatar_url"),
                text(node, "html_url"),
                node.path("site_admin").asBoolean()
        );
    }

    private @Nullable Repository parseRepository(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new Repository(
                node.path("id").asLong(),
                text(node, "name"),
                text(node, "full_name"),
                parseUser(node.path("owner")),
                node.path("private").asBoolean(),
                text(node, "html_url"),
                text(node, "description"),
                text(node, "default_branch")
        );
    }

    private @Nullable Label parseLabel(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new Label(
                node.path("id").asLong(),
                text(node, "name"),
                text(node, "color"),
                text(node, "description")
        );
    }

    private @Nullable Issue parseIssue(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new Issue(
                node.path("id").asLong(),
                node.path("number").asInt(),
                text(node, "title"),
                text(node, "body"),
                text(node, "state"),
                text(node, "state_reason"),
                parseUser(node.path("user")),
                parseUser(node.path("assignee")),
                parseUserList(node.path("assignees")),
                parseLabelList(node.path("labels")),
                node.path("locked").asBoolean(),
                text(node, "author_association"),
                !node.path("pull_request").isMissingNode() && !node.path("pull_request").isNull(),
                text(node, "html_url"),
                text(node, "created_at"),
                text(node, "updated_at"),
                text(node, "closed_at")
        );
    }

    private @Nullable Comment parseComment(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new Comment(
                node.path("id").asLong(),
                text(node, "body"),
                parseUser(node.path("user")),
                text(node, "author_association"),
                text(node, "html_url"),
                text(node, "created_at"),
                text(node, "updated_at")
        );
    }

    private @Nullable PullRequest parsePullRequest(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new PullRequest(
                node.path("id").asLong(),
                node.path("number").asInt(),
                text(node, "title"),
                text(node, "body"),
                text(node, "state"),
                node.path("draft").asBoolean(),
                node.path("merged").asBoolean(),
                parseUser(node.path("user")),
                parseUser(node.path("assignee")),
                parseUserList(node.path("assignees")),
                parseUserList(node.path("requested_reviewers")),
                parseLabelList(node.path("labels")),
                parsePullRequestRef(node.path("head")),
                parsePullRequestRef(node.path("base")),
                parseUser(node.path("merged_by")),
                text(node, "author_association"),
                text(node, "html_url"),
                text(node, "created_at"),
                text(node, "updated_at"),
                text(node, "closed_at"),
                text(node, "merged_at"),
                node.path("commits").asInt(),
                node.path("additions").asInt(),
                node.path("deletions").asInt(),
                node.path("changed_files").asInt()
        );
    }

    private @Nullable PullRequestRef parsePullRequestRef(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new PullRequestRef(
                text(node, "label"),
                text(node, "ref"),
                text(node, "sha"),
                parseUser(node.path("user")),
                parseRepository(node.path("repo"))
        );
    }

    private @Nullable Installation parseInstallation(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new Installation(
                node.path("id").asLong(),
                node.path("app_id").asLong()
        );
    }

    private List<User> parseUserList(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        List<User> users = new ArrayList<>();
        for (JsonNode item : node) {
            users.add(parseUser(item));
        }
        return List.copyOf(users);
    }

    private List<Label> parseLabelList(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        List<Label> labels = new ArrayList<>();
        for (JsonNode item : node) {
            labels.add(parseLabel(item));
        }
        return List.copyOf(labels);
    }

    private static @Nullable String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
