package org.hiero.bot.config;

/**
 * Default implementation of {@link RepoConfig} backed by immutable config records.
 *
 * @param labels           label configuration
 * @param assignmentLimits assignment limit configuration
 * @param guards           guard threshold configuration
 * @param features         feature flag configuration
 * @param markers          marker configuration
 * @param commands         command pattern configuration
 * @param paths            file path configuration
 * @param codeRabbit       CodeRabbit trigger configuration
 */
public record DefaultRepoConfig(LabelsConfig labels,
                                AssignmentLimitsConfig assignmentLimits,
                                GuardsConfig guards,
                                FeaturesConfig features,
                                MarkersConfig markers,
                                CommandsConfig commands,
                                PathsConfig paths,
                                CodeRabbitConfig codeRabbit) implements RepoConfig {

    /**
     * Creates a {@code DefaultRepoConfig} with all default values.
     *
     * @return a fully-defaulted configuration
     */
    public static DefaultRepoConfig allDefaults() {
        return new DefaultRepoConfig(
                LabelsConfig.defaults(),
                AssignmentLimitsConfig.defaults(),
                GuardsConfig.defaults(),
                FeaturesConfig.defaults(),
                MarkersConfig.defaults(),
                CommandsConfig.defaults(),
                PathsConfig.defaults(),
                CodeRabbitConfig.defaults()
        );
    }
}
