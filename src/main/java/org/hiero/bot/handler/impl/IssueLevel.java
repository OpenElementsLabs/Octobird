package org.hiero.bot.handler.impl;

import org.hiero.bot.config.RepoConfig;
import org.kohsuke.github.GHLabel;

import java.util.Collection;

/**
 * Issue difficulty levels ordered by complexity. Each level has a numeric complexity value
 * that increases from {@link #GOOD_FIRST_ISSUE} (1) to {@link #ADVANCED} (4).
 */
enum IssueLevel {

    GOOD_FIRST_ISSUE(1),
    BEGINNER(2),
    INTERMEDIATE(3),
    ADVANCED(4);

    private final int complexity;

    IssueLevel(final int complexity) {
        this.complexity = complexity;
    }

    /**
     * Returns the numeric complexity value of this level.
     *
     * @return the complexity (1 = easiest, 4 = hardest)
     */
    int complexity() {
        return complexity;
    }

    /**
     * Determines the difficulty level of an issue based on its labels and the repository
     * configuration. Returns the highest-precedence level found, or {@code null} if no
     * difficulty label is present.
     *
     * @param labels     the labels on the issue
     * @param repoConfig the repository configuration providing label names
     * @return the matching {@code Level}, or {@code null} if none matches
     */
    static IssueLevel determineLevel(final Collection<GHLabel> labels, final RepoConfig repoConfig) {
        for (final GHLabel label : labels) {
            if (label.getName().equalsIgnoreCase(repoConfig.labels().advanced())) {
                return ADVANCED;
            }
        }
        for (final GHLabel label : labels) {
            if (label.getName().equalsIgnoreCase(repoConfig.labels().intermediate())) {
                return INTERMEDIATE;
            }
        }
        for (final GHLabel label : labels) {
            if (label.getName().equalsIgnoreCase(repoConfig.labels().beginner())) {
                return BEGINNER;
            }
        }
        for (final GHLabel label : labels) {
            if (label.getName().equalsIgnoreCase(repoConfig.labels().goodFirstIssue())) {
                return GOOD_FIRST_ISSUE;
            }
        }
        return null;
    }
}
