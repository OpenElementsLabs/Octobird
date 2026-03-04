package com.openelements.octobird.model.event;

import com.openelements.octobird.model.GitHubAction;
import com.openelements.octobird.model.Installation;
import com.openelements.octobird.model.Repository;
import com.openelements.octobird.model.User;
import com.openelements.octobird.model.WorkflowRun;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Payload for the {@code workflow_run} webhook event, triggered when a GitHub Actions workflow
 * run is requested, in progress, or completed.
 *
 * @param action      the action performed (e.g. {@code COMPLETED})
 * @param workflowRun the workflow run that triggered the event
 * @param repository  the repository where the event occurred, may be {@code null}
 * @param sender      the user that triggered the event
 * @param installation the GitHub App installation, may be {@code null}
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#workflow_run">GitHub Webhooks &ndash; workflow_run event</a>
 */
public record WorkflowRunEvent(GitHubAction action,
                                WorkflowRun workflowRun,
                                @Nullable Repository repository,
                                User sender,
                                @Nullable Installation installation) implements WebhookEvent {

    public WorkflowRunEvent {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(workflowRun, "workflowRun must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
    }
}
