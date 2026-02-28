package org.hiero.bot.config;

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
}
