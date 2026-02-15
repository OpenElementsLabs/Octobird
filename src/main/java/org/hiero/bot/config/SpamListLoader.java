package org.hiero.bot.config;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SpamListLoader {

    private static final String SPAM_LIST_PATH = ".github/spam-list.txt";

    private final ConcurrentHashMap<String, Set<String>> cache = new ConcurrentHashMap<>();

    public boolean isSpamUser(GitHub gitHub, String repoFullName, String username) {
        Set<String> spamUsers = cache.computeIfAbsent(repoFullName, name -> loadSpamList(gitHub, name));
        return spamUsers.contains(username);
    }

    private Set<String> loadSpamList(GitHub gitHub, String repoFullName) {
        try {
            GHRepository repo = gitHub.getRepository(repoFullName);
            GHContent content = repo.getFileContent(SPAM_LIST_PATH);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(content.read(), StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toSet());
            }
        } catch (IOException e) {
            System.out.println("No spam list found for " + repoFullName + ", treating as empty");
            return Set.of();
        }
    }

    public void invalidateCache(String repoFullName) {
        cache.remove(repoFullName);
    }
}
