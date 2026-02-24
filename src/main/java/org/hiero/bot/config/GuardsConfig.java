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
     * Returns the default guard thresholds.
     *
     * @return a {@code GuardsConfig} with standard thresholds
     */
    public static GuardsConfig defaults() {
        return new GuardsConfig(1, 0, 1);
    }
}
