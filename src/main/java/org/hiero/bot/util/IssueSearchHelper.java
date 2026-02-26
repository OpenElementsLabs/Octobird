package org.hiero.bot.util;

import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Objects;

/**
 * Utility for executing GitHub search queries related to issue assignments and pull requests.
 * Used by handlers and guards to check a user's assignment history.
 */
public final class IssueSearchHelper {

    private IssueSearchHelper() {
    }

    /**
     * Counts the number of open issues currently assigned to the given user in the repository.
     *
     * @param gitHub       authenticated GitHub client
     * @param repoFullName full repository name in {@code owner/repo} format
     * @param username     the GitHub login to query
     * @return the number of open assigned issues
     * @throws IOException if the search query fails
     */
    public static int countOpenAssignments(final GitHub gitHub, final String repoFullName,
                                           final String username) throws IOException {
        Objects.requireNonNull(gitHub, "gitHub must not be null");
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        Objects.requireNonNull(username, "username must not be null");
        return gitHub.searchIssues()
                .q("repo:" + repoFullName + " is:issue is:open assignee:" + username)
                .list()
                .toList()
                .size();
    }

    /**
     * Counts the closed issues with the specified label that were assigned to the given user.
     *
     * @param gitHub       authenticated GitHub client
     * @param repoFullName full repository name in {@code owner/repo} format
     * @param username     the GitHub login to query
     * @param label        the label name to filter by (e.g. {@code "Good First Issue"})
     * @return the number of matching closed issues
     * @throws IOException if the search query fails
     */
    public static int countClosedIssuesByLabel(final GitHub gitHub, final String repoFullName,
                                               final String username, final String label) throws IOException {
        Objects.requireNonNull(gitHub, "gitHub must not be null");
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(label, "label must not be null");
        return gitHub.searchIssues()
                .q("repo:" + repoFullName + " is:issue is:closed assignee:" + username
                        + " label:\"" + label + "\"")
                .list()
                .toList()
                .size();
    }

    /**
     * Returns {@code true} if the user has no merged pull requests in the repository, indicating
     * they are a new contributor.
     *
     * @param gitHub       authenticated GitHub client
     * @param repoFullName full repository name in {@code owner/repo} format
     * @param username     the GitHub login to query
     * @return {@code true} if no merged PRs exist for this user
     * @throws IOException if the search query fails
     */
    public static boolean hasNoMergedPullRequests(final GitHub gitHub, final String repoFullName,
                                                  final String username) throws IOException {
        Objects.requireNonNull(gitHub, "gitHub must not be null");
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        Objects.requireNonNull(username, "username must not be null");
        final int count = gitHub.searchIssues()
                .q("repo:" + repoFullName + " is:pr is:merged author:" + username)
                .list()
                .toList()
                .size();
        return count == 0;
    }
}
