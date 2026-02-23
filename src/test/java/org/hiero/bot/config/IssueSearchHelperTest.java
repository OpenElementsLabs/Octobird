package org.hiero.bot.config;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueSearchHelperTest {

    private IssueSearchHelper helper;

    @Mock private GitHub gitHub;
    @Mock private GHIssueSearchBuilder searchBuilder;
    @Mock private PagedSearchIterable<GHIssue> searchResult;

    @BeforeEach
    void setUp() {
        helper = new IssueSearchHelper();
    }

    @Test
    @SuppressWarnings("unchecked")
    void countOpenAssignmentsReturnsCorrectCount() throws IOException {
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class), mock(GHIssue.class)));

        final int count = helper.countOpenAssignments(gitHub, "owner/repo", "alice");

        assertEquals(2, count);
    }

    @Test
    @SuppressWarnings("unchecked")
    void countClosedIssuesByLabelReturnsCorrectCount() throws IOException {
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class)));

        final int count = helper.countClosedIssuesByLabel(gitHub, "owner/repo", "alice", "beginner");

        assertEquals(1, count);
    }

    @Test
    @SuppressWarnings("unchecked")
    void hasNoMergedPullRequestsReturnsTrueWhenNone() throws IOException {
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of());

        assertTrue(helper.hasNoMergedPullRequests(gitHub, "owner/repo", "alice"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void hasNoMergedPullRequestsReturnsFalseWhenSomeExist() throws IOException {
        when(gitHub.searchIssues()).thenReturn(searchBuilder);
        when(searchBuilder.q(any())).thenReturn(searchBuilder);
        when(searchBuilder.list()).thenReturn(searchResult);
        when(searchResult.toList()).thenReturn(List.of(mock(GHIssue.class)));

        assertFalse(helper.hasNoMergedPullRequests(gitHub, "owner/repo", "alice"));
    }
}
