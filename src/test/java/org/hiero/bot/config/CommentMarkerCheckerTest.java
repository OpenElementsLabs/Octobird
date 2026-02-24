package org.hiero.bot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.PagedIterator;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentMarkerCheckerTest {

    @Mock private GHIssue issue;

    @Test
    void returnsTrueWhenMarkerPresent() throws IOException {
        final GHIssueComment comment = mock(GHIssueComment.class);
        when(comment.getBody()).thenReturn("<!-- test-marker -->\nSome text");
        mockCommentList(List.of(comment));

        assertTrue(CommentMarkerChecker.hasMarker(issue, "<!-- test-marker -->"));
    }

    @Test
    void returnsFalseWhenNoMarker() throws IOException {
        final GHIssueComment comment = mock(GHIssueComment.class);
        when(comment.getBody()).thenReturn("Some regular comment");
        mockCommentList(List.of(comment));

        assertFalse(CommentMarkerChecker.hasMarker(issue, "<!-- test-marker -->"));
    }

    @Test
    void returnsFalseWhenNoComments() throws IOException {
        mockCommentList(List.of());

        assertFalse(CommentMarkerChecker.hasMarker(issue, "<!-- test-marker -->"));
    }

    @Test
    void handlesNullCommentBody() throws IOException {
        final GHIssueComment comment = mock(GHIssueComment.class);
        when(comment.getBody()).thenReturn(null);
        mockCommentList(List.of(comment));

        assertFalse(CommentMarkerChecker.hasMarker(issue, "<!-- test-marker -->"));
    }

    @SuppressWarnings("unchecked")
    private void mockCommentList(final List<GHIssueComment> comments) throws IOException {
        final PagedIterable<GHIssueComment> pagedIterable = mock(PagedIterable.class);
        final PagedIterator<GHIssueComment> pagedIterator = mock(PagedIterator.class);
        when(pagedIterable.iterator()).thenReturn(pagedIterator);

        final var iterator = comments.iterator();
        when(pagedIterator.hasNext()).thenAnswer(inv -> iterator.hasNext());
        lenient().when(pagedIterator.next()).thenAnswer(inv -> iterator.next());
        when(issue.listComments()).thenReturn(pagedIterable);
    }
}
