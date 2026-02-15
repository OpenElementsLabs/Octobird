package org.hiero.bot.model.parse;

import org.hiero.bot.model.event.IssueCommentEvent;
import org.hiero.bot.model.event.IssuesEvent;
import org.hiero.bot.model.event.PullRequestEvent;
import org.hiero.bot.model.event.WebhookEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JacksonWebhookParserTest {

    private JacksonWebhookParser parser;

    @BeforeEach
    void setUp() {
        parser = new JacksonWebhookParser();
    }

    @Test
    void parsesIssueCommentEvent() {
        String json = """
                {
                  "action": "created",
                  "comment": {
                    "id": 123,
                    "body": "/assign",
                    "user": {
                      "id": 1,
                      "login": "alice",
                      "type": "User",
                      "avatar_url": "https://example.com/avatar",
                      "html_url": "https://github.com/alice",
                      "site_admin": false
                    },
                    "author_association": "CONTRIBUTOR",
                    "html_url": "https://github.com/owner/repo/issues/1#comment-123",
                    "created_at": "2024-01-01T00:00:00Z",
                    "updated_at": "2024-01-01T00:00:00Z"
                  },
                  "issue": {
                    "id": 456,
                    "number": 42,
                    "title": "Test issue",
                    "body": "Issue body",
                    "state": "open",
                    "user": {
                      "id": 2,
                      "login": "bob",
                      "type": "User"
                    },
                    "assignees": [],
                    "labels": [
                      { "id": 10, "name": "bug", "color": "ff0000", "description": "Bug report" }
                    ],
                    "locked": false,
                    "html_url": "https://github.com/owner/repo/issues/42"
                  },
                  "repository": {
                    "id": 789,
                    "name": "repo",
                    "full_name": "owner/repo",
                    "owner": {
                      "id": 3,
                      "login": "owner",
                      "type": "Organization"
                    },
                    "private": false,
                    "html_url": "https://github.com/owner/repo",
                    "default_branch": "main"
                  },
                  "sender": {
                    "id": 1,
                    "login": "alice",
                    "type": "User"
                  },
                  "installation": {
                    "id": 100,
                    "app_id": 200
                  }
                }
                """;

        WebhookEvent event = parser.parse("issue_comment", json);

        assertInstanceOf(IssueCommentEvent.class, event);
        IssueCommentEvent ice = (IssueCommentEvent) event;

        assertEquals("created", ice.action());
        assertEquals(123, ice.comment().id());
        assertEquals("/assign", ice.comment().body());
        assertEquals("alice", ice.comment().user().login());
        assertEquals("User", ice.comment().user().type());
        assertEquals("CONTRIBUTOR", ice.comment().authorAssociation());

        assertEquals(42, ice.issue().number());
        assertEquals("Test issue", ice.issue().title());
        assertEquals("open", ice.issue().state());
        assertEquals("bob", ice.issue().user().login());
        assertFalse(ice.issue().hasPullRequest());
        assertEquals(1, ice.issue().labels().size());
        assertEquals("bug", ice.issue().labels().get(0).name());

        assertEquals("owner/repo", ice.repository().fullName());
        assertEquals("main", ice.repository().defaultBranch());
        assertEquals("owner", ice.repository().owner().login());

        assertEquals("alice", ice.sender().login());
        assertEquals(100, ice.installation().id());
        assertEquals(200, ice.installation().appId());
    }

    @Test
    void parsesIssueCommentWithPullRequest() {
        String json = """
                {
                  "action": "created",
                  "comment": {
                    "id": 1,
                    "body": "test",
                    "user": { "id": 1, "login": "alice", "type": "User" }
                  },
                  "issue": {
                    "id": 1,
                    "number": 10,
                    "title": "PR title",
                    "state": "open",
                    "user": { "id": 1, "login": "alice", "type": "User" },
                    "pull_request": { "url": "https://api.github.com/repos/owner/repo/pulls/10" }
                  },
                  "repository": { "id": 1, "name": "repo", "full_name": "owner/repo" },
                  "sender": { "id": 1, "login": "alice", "type": "User" },
                  "installation": { "id": 1, "app_id": 1 }
                }
                """;

        IssueCommentEvent event = (IssueCommentEvent) parser.parse("issue_comment", json);
        assertTrue(event.issue().hasPullRequest());
    }

    @Test
    void parsesIssuesAssignedEvent() {
        String json = """
                {
                  "action": "assigned",
                  "issue": {
                    "id": 1,
                    "number": 5,
                    "title": "Fix bug",
                    "state": "open",
                    "user": { "id": 1, "login": "bob", "type": "User" },
                    "assignees": [
                      { "id": 2, "login": "alice", "type": "User" }
                    ],
                    "labels": []
                  },
                  "assignee": {
                    "id": 2,
                    "login": "alice",
                    "type": "User"
                  },
                  "repository": { "id": 1, "name": "repo", "full_name": "owner/repo" },
                  "sender": { "id": 1, "login": "bob", "type": "User" },
                  "installation": { "id": 1, "app_id": 1 }
                }
                """;

        WebhookEvent event = parser.parse("issues", json);

        assertInstanceOf(IssuesEvent.class, event);
        IssuesEvent ie = (IssuesEvent) event;

        assertEquals("assigned", ie.action());
        assertEquals(5, ie.issue().number());
        assertNotNull(ie.assignee());
        assertEquals("alice", ie.assignee().login());
        assertNull(ie.label());
    }

    @Test
    void parsesIssuesLabeledEvent() {
        String json = """
                {
                  "action": "labeled",
                  "issue": {
                    "id": 1,
                    "number": 5,
                    "title": "Fix bug",
                    "state": "open",
                    "user": { "id": 1, "login": "bob", "type": "User" },
                    "labels": [
                      { "id": 10, "name": "enhancement", "color": "84b6eb" }
                    ]
                  },
                  "label": {
                    "id": 10,
                    "name": "enhancement",
                    "color": "84b6eb"
                  },
                  "repository": { "id": 1, "name": "repo", "full_name": "owner/repo" },
                  "sender": { "id": 1, "login": "bob", "type": "User" },
                  "installation": { "id": 1, "app_id": 1 }
                }
                """;

        IssuesEvent event = (IssuesEvent) parser.parse("issues", json);

        assertEquals("labeled", event.action());
        assertNotNull(event.label());
        assertEquals("enhancement", event.label().name());
        assertNull(event.assignee());
    }

    @Test
    void parsesPullRequestEvent() {
        String json = """
                {
                  "action": "opened",
                  "number": 7,
                  "pull_request": {
                    "id": 100,
                    "number": 7,
                    "title": "Add feature",
                    "body": "PR body",
                    "state": "open",
                    "draft": false,
                    "merged": false,
                    "user": { "id": 1, "login": "alice", "type": "User" },
                    "assignees": [],
                    "requested_reviewers": [
                      { "id": 2, "login": "bob", "type": "User" }
                    ],
                    "labels": [],
                    "head": {
                      "label": "alice:feature",
                      "ref": "feature",
                      "sha": "abc123",
                      "user": { "id": 1, "login": "alice", "type": "User" },
                      "repo": { "id": 1, "name": "repo", "full_name": "alice/repo" }
                    },
                    "base": {
                      "label": "owner:main",
                      "ref": "main",
                      "sha": "def456",
                      "user": { "id": 3, "login": "owner", "type": "Organization" },
                      "repo": { "id": 2, "name": "repo", "full_name": "owner/repo" }
                    },
                    "html_url": "https://github.com/owner/repo/pull/7",
                    "commits": 3,
                    "additions": 50,
                    "deletions": 10,
                    "changed_files": 2
                  },
                  "repository": { "id": 2, "name": "repo", "full_name": "owner/repo" },
                  "sender": { "id": 1, "login": "alice", "type": "User" },
                  "installation": { "id": 1, "app_id": 1 }
                }
                """;

        WebhookEvent event = parser.parse("pull_request", json);

        assertInstanceOf(PullRequestEvent.class, event);
        PullRequestEvent pre = (PullRequestEvent) event;

        assertEquals("opened", pre.action());
        assertEquals(7, pre.number());
        assertEquals("Add feature", pre.pullRequest().title());
        assertFalse(pre.pullRequest().draft());
        assertFalse(pre.pullRequest().merged());
        assertEquals("alice", pre.pullRequest().user().login());

        assertEquals("feature", pre.pullRequest().head().ref());
        assertEquals("abc123", pre.pullRequest().head().sha());
        assertEquals("main", pre.pullRequest().base().ref());

        assertEquals(1, pre.pullRequest().requestedReviewers().size());
        assertEquals("bob", pre.pullRequest().requestedReviewers().get(0).login());

        assertEquals(3, pre.pullRequest().commits());
        assertEquals(50, pre.pullRequest().additions());
        assertEquals(10, pre.pullRequest().deletions());
        assertEquals(2, pre.pullRequest().changedFiles());
    }

    @Test
    void throwsOnUnsupportedEventType() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("push", "{}"));
    }

    @Test
    void throwsOnInvalidJson() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("issue_comment", "not json"));
    }

    @Test
    void handlesNullableFieldsGracefully() {
        String json = """
                {
                  "action": "opened",
                  "issue": {
                    "id": 1,
                    "number": 1,
                    "title": "Test",
                    "state": "open",
                    "user": { "id": 1, "login": "alice", "type": "User" }
                  },
                  "repository": { "id": 1, "name": "repo", "full_name": "owner/repo" },
                  "sender": { "id": 1, "login": "alice", "type": "User" },
                  "installation": { "id": 1, "app_id": 1 }
                }
                """;

        IssuesEvent event = (IssuesEvent) parser.parse("issues", json);

        assertNull(event.assignee());
        assertNull(event.label());
        assertNull(event.issue().body());
        assertNull(event.issue().assignee());
        assertEquals(0, event.issue().assignees().size());
        assertEquals(0, event.issue().labels().size());
        assertFalse(event.issue().hasPullRequest());
    }
}
