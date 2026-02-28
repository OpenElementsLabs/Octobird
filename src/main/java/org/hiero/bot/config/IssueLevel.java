package org.hiero.bot.config;

import org.kohsuke.github.GHLabel;

import java.util.Collection;

/**
 * Issue difficulty levels ordered by complexity. Each level has a numeric complexity value
 * that increases from {@link #GOOD_FIRST_ISSUE} (1) to {@link #ADVANCED} (4).
 */
public enum IssueLevel {

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
    public int complexity() {
        return complexity;
    }

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
