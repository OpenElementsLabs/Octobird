package com.openelements.octobird.handler;

import org.kohsuke.github.GitHub;

/**
 * Registry providing access to external services used by event handlers.
 *
 * <p>Centralises service access so that handlers remain decoupled from
 * concrete service clients. Additional services (e.g. Discord, Slack) can be
 * added here as the bot grows.
 */
public interface ServiceRegistry {

    /**
     * Returns an authenticated GitHub API client for the current installation.
     *
     * @return the {@link GitHub} client
     */
    GitHub getGitHub();
}