package org.hiero.bot.rest;

/**
 * DTO representing a GitHub account with immutable user ID and mutable username.
 *
 * @param githubId the GitHub numeric user ID (immutable)
 * @param username the current GitHub username (may change over time)
 */
public record GitHubAccountDto(long githubId, String username) {
}
