package org.hiero.bot.util;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

import java.io.IOException;

/**
 * Utility for checking whether an HTML comment marker is already present on an issue.
 * Used to prevent duplicate bot comments (e.g. assignment reminders, guard notices).
 */
public final class CommentMarkerChecker {

    private CommentMarkerChecker() {
    }

    /**
     * Returns {@code true} if any existing comment on {@code issue} contains the given
     * {@code marker} string.
     *
     * @param issue  the GitHub issue whose comments are searched
     * @param marker the substring to look for in comment bodies
     * @return {@code true} if a comment containing {@code marker} exists
     * @throws IOException if the comment list cannot be fetched
     */
    public static boolean hasMarker(final GHIssue issue, final String marker) throws IOException {
        for (final GHIssueComment comment : issue.listComments()) {
            final String body = comment.getBody();
            if (body != null && body.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
