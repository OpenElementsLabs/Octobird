package org.hiero.bot.config;

/**
 * File paths for per-repository data files loaded by the bot.
 *
 * @param spamList     path to the spam user list file
 * @param mentorRoster path to the mentor roster JSON file
 */
public record PathsConfig(String spamList, String mentorRoster) {

    /**
     * Returns the default file paths.
     *
     * @return a {@code PathsConfig} with standard paths
     */
    public static PathsConfig defaults() {
        return new PathsConfig(".github/spam-list.txt", ".github/mentor_roster.json");
    }
}
