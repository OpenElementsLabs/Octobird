package org.hiero.bot.handler.impl;

import org.hiero.bot.config.IssueLevel;
import org.hiero.bot.config.LabelsConfig;
import org.kohsuke.github.GHLabel;

import java.util.Collection;

/**
 * Determines the {@link IssueLevel} of an issue based on its GitHub labels.
 */
final class IssueLevelDetector {

    private IssueLevelDetector() {
    }

    /**
     * Determines the difficulty level of an issue based on its labels and the configured label
     * names. Returns the highest-complexity level found, or {@code null} if no difficulty label
     * is present.
     *
     * @param labels the labels on the issue
     * @param labelsConfig the label configuration providing label names
     * @return the matching {@code IssueLevel}, or {@code null} if none matches
     */
    static IssueLevel determineLevel(final Collection<GHLabel> labels, final LabelsConfig labelsConfig) {
        // Check from highest complexity to lowest so the most specific level wins
        final IssueLevel[] levels = {IssueLevel.ADVANCED, IssueLevel.INTERMEDIATE,
                IssueLevel.BEGINNER, IssueLevel.GOOD_FIRST_ISSUE};
        for (final IssueLevel level : levels) {
            final String labelName = labelsConfig.labelFor(level);
            for (final GHLabel label : labels) {
                if (label.getName().equalsIgnoreCase(labelName)) {
                    return level;
                }
            }
        }
        return null;
    }
}
