package org.hiero.bot.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class AssignCommandHandler implements EventHandler {

    private static final Pattern ASSIGN_PATTERN = Pattern.compile("/assign\\b");

    @Override
    public boolean matches(String event, String action) {
        return "issue_comment".equals(event) && "created".equals(action);
    }

    @Override
    public void handle(String event, String action, JsonNode payload, GitHub gitHub,
                       Map<String, Object> repoConfig) throws IOException {
        String body = payload.path("comment").path("body").asText("");
        if (!ASSIGN_PATTERN.matcher(body).find()) {
            return;
        }

        String commenter = payload.path("comment").path("user").path("login").asText();
        String repoFullName = payload.path("repository").path("full_name").asText();
        int issueNumber = payload.path("issue").path("number").asInt();

        GHRepository repo = gitHub.getRepository(repoFullName);
        GHIssue issue = repo.getIssue(issueNumber);

        boolean alreadyAssigned = issue.getAssignees().stream()
                .anyMatch(u -> u.getLogin().equals(commenter));

        if (alreadyAssigned) {
            issue.comment("@" + commenter + " you are already assigned to this issue.");
            return;
        }

        issue.addAssignees(gitHub.getUser(commenter));
        issue.comment("@" + commenter + " has been assigned to this issue.");
        System.out.println("Assigned " + commenter + " to " + repoFullName + "#" + issueNumber);
    }
}
