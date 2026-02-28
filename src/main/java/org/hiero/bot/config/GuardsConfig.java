package org.hiero.bot.config;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Prerequisite counts required before a user can be assigned to higher-difficulty issues.
 * The key is the target issue level; the value is the number of completed issues at the
 * previous level required before assignment.
 *
 * @param requiredCounts immutable map from target level to required completed count at previous level
 */
public record GuardsConfig(Map<IssueLevel, Integer> requiredCounts) {

    /**
     * Creates a {@code GuardsConfig} storing an immutable copy of the given counts.
     */
    public GuardsConfig {
        requiredCounts = Collections.unmodifiableMap(new EnumMap<>(requiredCounts));
    }

    /**
     * Returns the required number of completed issues at the previous level before a user
     * can be assigned to an issue at the given level.
     *
     * @param level the target issue level (must be {@link IssueLevel#BEGINNER} or higher)
     * @return the required count of completed issues at the previous level
     * @throws IllegalArgumentException if level is {@link IssueLevel#GOOD_FIRST_ISSUE}
     */
    public int requiredCountFor(final IssueLevel level) {
        if (level == IssueLevel.GOOD_FIRST_ISSUE) {
            throw new IllegalArgumentException("GOOD_FIRST_ISSUE has no prerequisite level");
        }
        return requiredCounts.get(level);
    }

    /**
     * Returns the default guard thresholds.
     *
     * @return a {@code GuardsConfig} with standard thresholds
     */
    public static GuardsConfig defaults() {
        final Map<IssueLevel, Integer> counts = new EnumMap<>(IssueLevel.class);
        counts.put(IssueLevel.GOOD_FIRST_ISSUE, 0);
        counts.put(IssueLevel.BEGINNER, 1);
        counts.put(IssueLevel.INTERMEDIATE, 0);
        counts.put(IssueLevel.ADVANCED, 1);
        return new GuardsConfig(counts);
    }
}
