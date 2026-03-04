package com.openelements.octobird.config;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Label names used to identify issue difficulty levels and special notification triggers.
 *
 * @param levelLabels  immutable map from issue level to its configured label name
 * @param gfiCandidate label that triggers the GFI candidate team notification
 */
public record LabelsConfig(Map<IssueLevel, String> levelLabels, String gfiCandidate) {

    /**
     * Creates a {@code LabelsConfig} storing an immutable copy of the given level labels.
     */
    public LabelsConfig {
        levelLabels = Collections.unmodifiableMap(new EnumMap<>(levelLabels));
    }

    /**
     * Returns the label name for the given issue difficulty level.
     *
     * @param level the issue level
     * @return the configured label name for that level
     */
    public String labelFor(final IssueLevel level) {
        return levelLabels.get(level);
    }

    /**
     * Returns the default label configuration.
     *
     * @return a {@code LabelsConfig} with standard label names
     */
    public static LabelsConfig defaults() {
        final Map<IssueLevel, String> labels = new EnumMap<>(IssueLevel.class);
        labels.put(IssueLevel.GOOD_FIRST_ISSUE, "Good First Issue");
        labels.put(IssueLevel.BEGINNER, "beginner");
        labels.put(IssueLevel.INTERMEDIATE, "intermediate");
        labels.put(IssueLevel.ADVANCED, "advanced");
        return new LabelsConfig(labels, "good first issue candidate");
    }
}
