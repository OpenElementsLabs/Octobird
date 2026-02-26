package org.hiero.bot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and caches the mentor roster from a JSON file in the repository. The roster is a JSON
 * object with an {@code "order"} array of GitHub login strings. Mentor selection rotates by
 * day number to spread assignments evenly over time.
 *
 * <p>If the file is absent or cannot be read, an empty list is returned silently.
 */
public final class MentorRosterLoader {

    private static final Logger LOG = LoggerFactory.getLogger(MentorRosterLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ConcurrentHashMap<String, List<String>> CACHE = new ConcurrentHashMap<>();

    private MentorRosterLoader() {
    }

    /**
     * Loads the mentor roster for the given repository from the specified path.
     *
     * @param gitHub       authenticated GitHub client
     * @param repoFullName full repository name (owner/repo)
     * @param rosterPath   path to the mentor roster JSON file in the repository
     * @return an unmodifiable list of mentor usernames
     */
    public static List<String> loadRoster(final GitHub gitHub, final String repoFullName,
                                          final String rosterPath) {
        final String cacheKey = repoFullName + ":" + rosterPath;
        return CACHE.computeIfAbsent(cacheKey, key -> doLoadRoster(gitHub, repoFullName, rosterPath));
    }

    /**
     * Selects a mentor from the roster using a day-based rotation strategy.
     *
     * @param roster the list of mentor usernames to choose from
     * @return the selected mentor login, or {@code null} if the roster is empty
     */
    public static String selectMentor(final List<String> roster) {
        if (roster.isEmpty()) {
            return null;
        }
        final long dayNumber = Instant.now().truncatedTo(ChronoUnit.DAYS).getEpochSecond() / 86400;
        final int index = (int) (dayNumber % roster.size());
        return roster.get(index);
    }

    private static List<String> doLoadRoster(final GitHub gitHub, final String repoFullName,
                                             final String rosterPath) {
        try {
            final GHRepository repo = gitHub.getRepository(repoFullName);
            final GHContent content = repo.getFileContent(rosterPath);
            try (final InputStream is = content.read()) {
                final JsonNode root = MAPPER.readTree(is);
                final JsonNode order = root.get("order");
                if (order == null || !order.isArray()) {
                    LOG.warn("Invalid mentor roster format in {}", repoFullName);
                    return List.of();
                }
                final List<String> mentors = new ArrayList<>();
                for (final JsonNode node : order) {
                    mentors.add(node.asText());
                }
                return Collections.unmodifiableList(mentors);
            }
        } catch (final IOException e) {
            LOG.debug("No mentor roster found for {} at {}, treating as empty", repoFullName, rosterPath);
            return List.of();
        }
    }

    /**
     * Removes all cached roster entries for the given repository, forcing the next lookup to
     * re-read from GitHub.
     *
     * @param repoFullName full repository name in {@code owner/repo} format
     */
    public static void invalidateCache(final String repoFullName) {
        CACHE.keySet().removeIf(key -> key.startsWith(repoFullName + ":"));
    }
}
