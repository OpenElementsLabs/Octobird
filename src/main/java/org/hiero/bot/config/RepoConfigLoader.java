package org.hiero.bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RepoConfigLoader {

    private static final String CONFIG_PATH = ".github/hiero-bot.yml";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public Map<String, Object> loadConfig(GitHub gitHub, String repoFullName) {
        return cache.computeIfAbsent(repoFullName, name -> {
            try {
                GHRepository repo = gitHub.getRepository(name);
                GHContent content = repo.getFileContent(CONFIG_PATH);
                try (InputStream is = content.read()) {
                    return YAML_MAPPER.readValue(is, Map.class);
                }
            } catch (IOException e) {
                System.out.println("No config found for " + name + ", using defaults");
                return Map.of();
            }
        });
    }

    public void invalidateCache(String repoFullName) {
        cache.remove(repoFullName);
    }
}
