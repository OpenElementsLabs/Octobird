package com.openelements.octobird.util;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for executing GitHub search queries related to issue assignments and pull requests.
 * Used by handlers and guards to check a user's assignment history.
 */
public final class IssueSearchHelper {

    private static final Pattern LINKED_ISSUE_PATTERN =
            Pattern.compile("(?i)(fixes|closes|resolves|fix|close|resolve)\\s+(?:[\\w-]+/[\\w-]+)?#(\\d+)");

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

    /**
     * Finds all open pull requests in the repository whose body contains a closing keyword
     * referencing the given issue number (e.g. {@code "closes #42"}).
     *
     * @param gitHub       authenticated GitHub client
     * @param repoFullName full repository name in {@code owner/repo} format
     * @param issueNumber  the issue number to search for
     * @return list of open PRs that link to the issue; empty if none found
     * @throws IOException if the search query fails
     */
    public static List<GHIssue> findOpenPrsLinkingToIssue(final GitHub gitHub,
                                                            final String repoFullName,
                                                            final int issueNumber) throws IOException {
        Objects.requireNonNull(gitHub, "gitHub must not be null");
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        final List<GHIssue> candidates = gitHub.searchIssues()
                .q("repo:" + repoFullName + " is:pr is:open \"#" + issueNumber + "\"")
                .list()
                .toList();
        final List<GHIssue> result = new ArrayList<>();
        for (final GHIssue pr : candidates) {
            final String body = pr.getBody();
            if (body == null) {
                continue;
            }
            final Matcher m = LINKED_ISSUE_PATTERN.matcher(body);
            while (m.find()) {
                if (Integer.parseInt(m.group(2)) == issueNumber) {
                    result.add(pr);
                    break;
                }
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns {@code true} if the given pull request body contains a closing keyword that
     * references any issue number.
     *
     * @param prBody the pull request body text, may be {@code null}
     * @return {@code true} if a closing reference is found
     */
    public static boolean hasLinkedIssueInBody(final String prBody) {
        if (prBody == null || prBody.isBlank()) {
            return false;
        }
        return LINKED_ISSUE_PATTERN.matcher(prBody).find();
    }

    /**
     * Extracts the first issue number referenced by a closing keyword in the PR body.
     *
     * @param prBody the pull request body text, may be {@code null}
     * @return the first referenced issue number, or {@code -1} if none found
     */
    public static int extractLinkedIssueNumber(final String prBody) {
        if (prBody == null || prBody.isBlank()) {
            return -1;
        }
        final Matcher m = LINKED_ISSUE_PATTERN.matcher(prBody);
        if (m.find()) {
            return Integer.parseInt(m.group(2));
        }
        return -1;
    }
}
