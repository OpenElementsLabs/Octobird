package org.hiero.bot.model;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a GitHub user or bot account.
 *
 * @param id        unique identifier of the user
 * @param login     username of the account
 * @param type      account type, e.g. {@code "User"}, {@code "Bot"}, or {@code "Organization"}
 * @param avatarUrl URL of the user's avatar image, may be {@code null}
 * @param htmlUrl   URL of the user's GitHub profile page, may be {@code null}
 * @param siteAdmin whether the user is a GitHub site administrator
 * @see <a href="https://docs.github.com/en/rest/users/users#get-a-user">GitHub REST API &ndash; Users</a>
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#user">GitHub Webhooks &ndash; user object</a>
 */
public record User(long id, String login, String type, @Nullable String avatarUrl, @Nullable String htmlUrl,
                   boolean siteAdmin) {

    public User {
        Objects.requireNonNull(login, "login must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }

    public boolean isBot() {
        return "Bot".equals(type);
    }
}
