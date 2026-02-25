package org.hiero.bot.util;

import org.hiero.bot.util.IssueSearchHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueSearchBuilder;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueSearchHelperTest {

    @Mock
    private GitHub gitHub;
    @Mock
    private GHIssueSearchBuilder searchBuilder;
    @Mock
    private PagedSearchIterable<GHIssue> searchResult;

    @Test
    @SuppressWarnings("unchecked")
    void countOpenAssignmentsReturnsCorrectCount() throws IOException {
        // Given
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class), mock(GHIssue.class)));

        // When
        final int count = IssueSearchHelper.countOpenAssignments(gitHub, "owner/repo", "alice");

        // Then
        assertEquals(2, count);
    }

    @Test
    @SuppressWarnings("unchecked")
    void countClosedIssuesByLabelReturnsCorrectCount() throws IOException {
        // Given
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class)));

        // When
        final int count = IssueSearchHelper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "beginner");

        // Then
        assertEquals(1, count);
    }

    @Test
    @SuppressWarnings("unchecked")
    void hasNoMergedPullRequestsReturnsTrueWhenNone() throws IOException {
        // Given
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of());

        // When
        final boolean result = IssueSearchHelper.hasNoMergedPullRequests(gitHub, "owner/repo", "alice");

        // Then
        assertTrue(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void hasNoMergedPullRequestsReturnsFalseWhenSomeExist() throws IOException {
        // Given
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class)));

        // When
        final boolean result = IssueSearchHelper.hasNoMergedPullRequests(gitHub, "owner/repo", "alice");

        // Then
        assertFalse(result);
    }
}