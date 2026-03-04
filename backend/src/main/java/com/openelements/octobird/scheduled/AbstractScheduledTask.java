package com.openelements.octobird.scheduled;

import com.openelements.octobird.config.RepoConfig;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Base class for scheduled tasks. Handles {@link #isActive(RepoConfig)} via a
 * {@link Predicate} supplied at construction time, so subclasses only need to implement
 * {@link #run}.
 */
public abstract class AbstractScheduledTask implements ScheduledTask {

    private final Predicate<RepoConfig> featureCheck;

    /**
     * Creates an {@code AbstractScheduledTask} with the given feature-flag predicate.
     *
     * @param featureCheck predicate that returns {@code true} when the task is enabled for
     *                     a given repository configuration
     */
    protected AbstractScheduledTask(final Predicate<RepoConfig> featureCheck) {
        this.featureCheck = Objects.requireNonNull(featureCheck, "featureCheck must not be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the predicate supplied at construction time.
     */
    @Override
    public final boolean isActive(final RepoConfig repoConfig) {
        return featureCheck.test(repoConfig);
    }
}
