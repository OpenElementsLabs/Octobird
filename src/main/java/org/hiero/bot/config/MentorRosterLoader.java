package org.hiero.bot.config;

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

public class MentorRosterLoader {

    private static final Logger LOG = LoggerFactory.getLogger(MentorRosterLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConcurrentHashMap<String, List<String>> cache = new ConcurrentHashMap<>();

    /**
     * Loads the mentor roster for the given repository from the specified path.
     *
     * @param gitHub       authenticated GitHub client
     * @param repoFullName full repository name (owner/repo)
     * @param rosterPath   path to the mentor roster JSON file in the repository
     * @return an unmodifiable list of mentor usernames
     */
    public List<String> loadRoster(final GitHub gitHub, final String repoFullName, final String rosterPath) {
        final String cacheKey = repoFullName + ":" + rosterPath;
        return cache.computeIfAbsent(cacheKey, key -> doLoadRoster(gitHub, repoFullName, rosterPath));
    }

    public String selectMentor(final List<String> roster) {
        if (roster.isEmpty()) {
            return null;
        }
        final long dayNumber = Instant.now().truncatedTo(ChronoUnit.DAYS).getEpochSecond() / 86400;
        final int index = (int) (dayNumber % roster.size());
        return roster.get(index);
    }

    private List<String> doLoadRoster(final GitHub gitHub, final String repoFullName, final String rosterPath) {
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

    public void invalidateCache(final String repoFullName) {
        cache.keySet().removeIf(key -> key.startsWith(repoFullName + ":"));
    }
}
