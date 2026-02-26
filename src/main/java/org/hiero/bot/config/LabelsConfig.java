package org.hiero.bot.config;

/**
 * Label names used to identify issue difficulty levels and special notification triggers.
 *
 * @param goodFirstIssue label for good-first-issue issues
 * @param beginner       label for beginner issues
 * @param intermediate   label for intermediate issues
 * @param advanced       label for advanced issues
 * @param p0             label that triggers the P0 team alarm notification
 * @param gfiCandidate   label that triggers the GFI candidate team notification
 */
public record LabelsConfig(String goodFirstIssue, String beginner, String intermediate, String advanced,
                           String p0, String gfiCandidate) {

    /**
     * Returns the default label configuration.
     *
     * @return a {@code LabelsConfig} with standard label names
     */
    public static LabelsConfig defaults() {
        return new LabelsConfig("Good First Issue", "beginner", "intermediate", "advanced",
                "p0", "good first issue candidate");
    }
}
