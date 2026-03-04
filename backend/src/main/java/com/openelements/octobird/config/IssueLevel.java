package com.openelements.octobird.config;

import org.kohsuke.github.GHLabel;

import java.util.Collection;

/**
 * Issue difficulty levels ordered from easiest ({@link #GOOD_FIRST_ISSUE}) to
 * hardest ({@link #ADVANCED}).
 */
public enum IssueLevel {

    GOOD_FIRST_ISSUE,
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;

    /**
     * Returns the previous (lower-complexity) level, or {@code null} if this is
     * {@link #GOOD_FIRST_ISSUE}.
     *
     * @return the previous level, or {@code null}
     */
    public IssueLevel previousLevel() {
        final IssueLevel[] levels = values();
        final int index = ordinal() - 1;
        return index >= 0 ? levels[index] : null;
    }

    /**
     * Determines the difficulty level of an issue based on its labels and the configured label
     * names. Returns the highest-complexity level found, or {@code null} if no difficulty label
     * is present.
     *
     * @param labels       the labels on the issue
     * @param labelsConfig the label configuration providing label names
     * @return the matching {@code IssueLevel}, or {@code null} if none matches
     */
    public static IssueLevel determineLevel(final Collection<GHLabel> labels, final LabelsConfig labelsConfig) {
        final IssueLevel[] levels = {ADVANCED, INTERMEDIATE, BEGINNER, GOOD_FIRST_ISSUE};
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
