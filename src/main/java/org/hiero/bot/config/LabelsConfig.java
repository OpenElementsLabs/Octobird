package org.hiero.bot.config;

/**
 * Label names used to identify issue difficulty levels and special notification triggers.
 *
 * @param goodFirstIssue label for good-first-issue issues
 * @param beginner       label for beginner issues
 * @param intermediate   label for intermediate issues
 * @param advanced       label for advanced issues
 * @param gfiCandidate   label that triggers the GFI candidate team notification
 */
public record LabelsConfig(String goodFirstIssue, String beginner, String intermediate, String advanced,
                           String gfiCandidate) {

    /**
     * Returns the label name for the given issue difficulty level.
     *
     * @param level the issue level
     * @return the configured label name for that level
     */
    public String labelFor(final IssueLevel level) {
        return switch (level) {
            case GOOD_FIRST_ISSUE -> goodFirstIssue;
            case BEGINNER -> beginner;
            case INTERMEDIATE -> intermediate;
            case ADVANCED -> advanced;
        };
    }

    /**
     * Returns the default label configuration.
     *
     * @return a {@code LabelsConfig} with standard label names
     */
    public static LabelsConfig defaults() {
        return new LabelsConfig("Good First Issue", "beginner", "intermediate", "advanced",
                "good first issue candidate");
    }
}
