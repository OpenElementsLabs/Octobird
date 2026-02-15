package org.hiero.bot.model;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a GitHub repository.
 *
 * @param id            unique identifier of the repository
 * @param name          short name of the repository (e.g. {@code "repo"})
 * @param fullName      full name including the owner (e.g. {@code "owner/repo"})
 * @param owner         the user or organization that owns the repository, may be {@code null}
 * @param isPrivate     whether the repository is private
 * @param htmlUrl       URL of the repository on GitHub, may be {@code null}
 * @param description   short description of the repository, may be {@code null}
 * @param defaultBranch name of the default branch (e.g. {@code "main"}), may be {@code null}
 * @see <a href="https://docs.github.com/en/rest/repos/repos#get-a-repository">GitHub REST API &ndash; Repositories</a>
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#repository">GitHub Webhooks &ndash; repository object</a>
 */
public record Repository(long id, String name, String fullName, @Nullable User owner, boolean isPrivate,
                          @Nullable String htmlUrl, @Nullable String description, @Nullable String defaultBranch) {

    public Repository {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(fullName, "fullName must not be null");
    }
}
