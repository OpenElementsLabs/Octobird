package com.openelements.octobird.config;

/**
 * GitHub team mention handles used for label-based notifications.
 *
 * @param gfiCandidateTeam GitHub team handle to @mention when a GFI candidate issue is labeled
 *                         (e.g. {@code "@org/gfi-support"})
 */
public record TeamsConfig(String gfiCandidateTeam) {

    /**
     * Returns the default team configuration with no team mentions configured.
     *
     * @return a {@code TeamsConfig} with empty defaults
     */
    public static TeamsConfig defaults() {
        return new TeamsConfig("");
    }
}
