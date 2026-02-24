package org.hiero.bot.config;

/**
 * Label names used to identify issue difficulty levels.
 *
 * @param goodFirstIssue label for good-first-issue issues
 * @param beginner       label for beginner issues
 * @param intermediate   label for intermediate issues
 * @param advanced       label for advanced issues
 */
public record LabelsConfig(String goodFirstIssue, String beginner, String intermediate, String advanced) {

    /**
     * Returns the default label configuration.
     *
     * @return a {@code LabelsConfig} with standard label names
     */
    public static LabelsConfig defaults() {
        return new LabelsConfig("Good First Issue", "beginner", "intermediate", "advanced");
    }
}
