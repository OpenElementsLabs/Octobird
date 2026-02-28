package org.hiero.bot.config;

/**
 * Prerequisite counts required before a user can be assigned to higher-difficulty issues.
 *
 * @param requiredGfiCountForBeginner              completed Good First Issues needed for beginner issues
 * @param requiredBeginnerCountForIntermediate     completed beginner issues needed for intermediate issues
 * @param requiredIntermediateCountForAdvanced     completed intermediate issues needed for advanced issues
 */
public record GuardsConfig(int requiredGfiCountForBeginner,
                           int requiredBeginnerCountForIntermediate,
                           int requiredIntermediateCountForAdvanced) {

    /**
     * Returns the required number of completed issues at the previous level before a user
     * can be assigned to an issue at the given level.
     *
     * @param level the target issue level (must be {@link IssueLevel#BEGINNER} or higher)
     * @return the required count of completed issues at the previous level
     * @throws IllegalArgumentException if level is {@link IssueLevel#GOOD_FIRST_ISSUE}
     */
    public int requiredCountFor(final IssueLevel level) {
        return switch (level) {
            case BEGINNER -> requiredGfiCountForBeginner;
            case INTERMEDIATE -> requiredBeginnerCountForIntermediate;
            case ADVANCED -> requiredIntermediateCountForAdvanced;
            case GOOD_FIRST_ISSUE -> throw new IllegalArgumentException(
                    "GOOD_FIRST_ISSUE has no prerequisite level");
        };
    }

    /**
     * Returns the default guard thresholds.
     *
     * @return a {@code GuardsConfig} with standard thresholds
     */
    public static GuardsConfig defaults() {
        return new GuardsConfig(1, 0, 1);
    }
}
