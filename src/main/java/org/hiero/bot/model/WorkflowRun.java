package org.hiero.bot.model;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents a GitHub Actions workflow run.
 *
 * @param id                  unique identifier of the workflow run
 * @param name                name of the workflow, may be {@code null}
 * @param headBranch          the branch the run was triggered on, may be {@code null}
 * @param headSha             the commit SHA the run was triggered on, may be {@code null}
 * @param conclusion          the outcome of the run (e.g. {@code "success"}, {@code "failure"},
 *                            {@code "cancelled"}), may be {@code null} while in progress
 * @param htmlUrl             URL of the workflow run on GitHub, may be {@code null}
 * @param pullRequestNumbers  pull request numbers associated with this run
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#workflow_run">GitHub Webhooks &ndash; workflow_run event</a>
 */
public record WorkflowRun(long id,
                          @Nullable String name,
                          @Nullable String headBranch,
                          @Nullable String headSha,
                          @Nullable String conclusion,
                          @Nullable String htmlUrl,
                          List<Integer> pullRequestNumbers) {

    public WorkflowRun {
        Objects.requireNonNull(pullRequestNumbers, "pullRequestNumbers must not be null");
        pullRequestNumbers = List.copyOf(pullRequestNumbers);
    }
}
