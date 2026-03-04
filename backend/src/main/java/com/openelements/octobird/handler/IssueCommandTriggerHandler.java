package com.openelements.octobird.handler;

import com.openelements.octobird.config.RepoConfig;
import com.openelements.octobird.model.GitHubAction;
import com.openelements.octobird.model.GitHubEventType;
import com.openelements.octobird.model.event.IssueCommentEvent;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Intermediate base class for handlers that react to slash commands in issue comments
 * (e.g. {@code /assign}, {@code /unassign}, {@code /working}).
 *
 * <p>Provides a fixed matcher ({@code ISSUE_COMMENT + CREATED}), skips bot comments,
 * checks the command pattern against the comment body, and dispatches to
 * {@link #handleCommand} when matched.
 *
 * <p>Subclasses supply the command regex via {@link #commandPattern(RepoConfig)} and
 * implement {@link #handleCommand}.
 */
public abstract class IssueCommandTriggerHandler extends AbstractEventHandler<IssueCommentEvent> {

    private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER =
            (event, action) -> event == GitHubEventType.ISSUE_COMMENT && action == GitHubAction.CREATED;

    /**
     * Constructs a handler with the given feature-check predicate. The event type and matcher
     * are fixed to {@code IssueCommentEvent} and {@code ISSUE_COMMENT + CREATED}.
     *
     * @param featureCheck predicate that returns {@code true} when this handler is enabled
     */
    protected IssueCommandTriggerHandler(final Predicate<RepoConfig> featureCheck) {
        super(IssueCommentEvent.class, MATCHER, featureCheck);
    }

    /**
     * Returns the compiled regex pattern for the command this handler recognises.
     *
     * @param repoConfig the repository configuration
     * @return the compiled command pattern
     */
    protected abstract Pattern commandPattern(RepoConfig repoConfig);

    /**
     * Called when the comment body matches the {@linkplain #commandPattern command pattern}.
     *
     * @param event      the issue-comment event
     * @param registry   the service registry
     * @param repoConfig the repository configuration
     * @throws IOException if a GitHub API call fails
     */
    protected abstract void handleCommand(IssueCommentEvent event, ServiceRegistry registry,
                                          RepoConfig repoConfig) throws IOException;

    @Override
    public final void handle(final IssueCommentEvent event, final ServiceRegistry registry,
                             final RepoConfig repoConfig) throws IOException {
        // Skip bots
        if (event.comment().user().isBot()) {
            return;
        }

        final String body = event.comment().body();
        final Pattern pattern = commandPattern(repoConfig);
        if (body != null && pattern.matcher(body).find()) {
            handleCommand(event, registry, repoConfig);
        }
    }
}
