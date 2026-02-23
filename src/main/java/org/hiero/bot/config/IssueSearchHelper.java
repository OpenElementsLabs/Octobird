package org.hiero.bot.config;

import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Objects;

public class IssueSearchHelper {

    public int countOpenAssignments(final GitHub gitHub, final String repoFullName,
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

    public int countClosedIssuesByLabel(final GitHub gitHub, final String repoFullName,
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

    public boolean hasNoMergedPullRequests(final GitHub gitHub, final String repoFullName,
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
