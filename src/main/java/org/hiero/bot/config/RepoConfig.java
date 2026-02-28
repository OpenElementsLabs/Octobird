package org.hiero.bot.config;

/**
 * Per-repository configuration contract. Implementations provide typed access to all bot settings.
 *
 * <p>This interface allows future replacement of the file-based implementation with a
 * database-backed one without changing handler code.
 *
 * @see DefaultRepoConfig
 * @see RepoConfigMapper
 */
public interface RepoConfig {

    /** Full repository name in {@code owner/repo} format. */
    String repoFullName();

    /** Label names for issue difficulty levels. */
    LabelsConfig labels();

    /** Maximum open assignment counts per user type. */
    AssignmentLimitsConfig assignmentLimits();

    /** Prerequisite thresholds for higher-difficulty issues. */
    GuardsConfig guards();

    /** Per-handler enable/disable flags. */
    FeaturesConfig features();

    /** HTML comment markers for duplicate prevention. */
    MarkersConfig markers();

    /** Regex patterns for bot commands. */
    CommandsConfig commands();

    /** File paths for per-repo data files. */
    PathsConfig paths();

    /** GitHub team mentions for label-based notifications. */
    TeamsConfig teams();

    /** Thresholds and settings for scheduled (cron-style) tasks. */
    ScheduledConfig scheduled();
}
