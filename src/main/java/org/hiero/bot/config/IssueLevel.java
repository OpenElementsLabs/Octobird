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
}
