package org.hiero.bot.config;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

import java.io.IOException;

public final class CommentMarkerChecker {

    private CommentMarkerChecker() {
    }

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
