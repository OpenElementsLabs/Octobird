package org.hiero.bot.config;

import java.util.List;

/**
 * GitHub team mention handles used for label-based notifications.
 *
 * @param p0Teams          list of GitHub team handles to @mention when a P0 issue is labeled
 *                         (e.g. {@code "@org/maintainers"})
 * @param gfiCandidateTeam GitHub team handle to @mention when a GFI candidate issue is labeled
 *                         (e.g. {@code "@org/gfi-support"})
 */
public record TeamsConfig(List<String> p0Teams, String gfiCandidateTeam) {

    /**
     * Returns the default team configuration with no team mentions configured.
     *
     * @return a {@code TeamsConfig} with empty defaults
     */
    public static TeamsConfig defaults() {
        return new TeamsConfig(List.of(), "");
    }
}
