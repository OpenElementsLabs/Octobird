package org.hiero.bot.util;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class SpamListLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SpamListLoader.class);

    private static final ConcurrentHashMap<String, Set<String>> CACHE = new ConcurrentHashMap<>();

    private SpamListLoader() {
    }

    /**
     * Checks whether the given user is on the spam list for the given repository.
     *
     * @param gitHub       authenticated GitHub client
     * @param repoFullName full repository name (owner/repo)
     * @param username     the user to check
     * @param spamListPath path to the spam list file in the repository
     * @return {@code true} if the user is on the spam list
     */
    public static boolean isSpamUser(final GitHub gitHub, final String repoFullName, final String username,
                                     final String spamListPath) {
        Objects.requireNonNull(gitHub, "gitHub must not be null");
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(spamListPath, "spamListPath must not be null");
        final String cacheKey = repoFullName + ":" + spamListPath;
        final Set<String> spamUsers = CACHE.computeIfAbsent(cacheKey,
                key -> loadSpamList(gitHub, repoFullName, spamListPath));
        return spamUsers.contains(username);
    }

    private static Set<String> loadSpamList(final GitHub gitHub, final String repoFullName,
                                            final String spamListPath) {
        try {
            final GHRepository repo = gitHub.getRepository(repoFullName);
            final GHContent content = repo.getFileContent(spamListPath);
            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(content.read(), StandardCharsets.UTF_8))) {
                return Set.copyOf(reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .collect(Collectors.toSet()));
            }
        } catch (final IOException e) {
            LOG.debug("No spam list found for {} at {}, treating as empty", repoFullName, spamListPath);
            return Set.of();
        }
    }

    public static void invalidateCache(final String repoFullName) {
        CACHE.keySet().removeIf(key -> key.startsWith(repoFullName + ":"));
    }
}
