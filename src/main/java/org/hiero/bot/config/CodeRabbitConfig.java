package org.hiero.bot.config;

import java.util.Set;

/**
 * Configuration for the CodeRabbit plan trigger.
 *
 * @param triggerLabels label names that trigger a CodeRabbit plan comment
 */
public record CodeRabbitConfig(Set<String> triggerLabels) {

    /**
     * Returns the default CodeRabbit configuration.
     *
     * @return a {@code CodeRabbitConfig} with standard trigger labels
     */
    public static CodeRabbitConfig defaults() {
        return new CodeRabbitConfig(Set.of("beginner", "intermediate", "advanced"));
    }
}
