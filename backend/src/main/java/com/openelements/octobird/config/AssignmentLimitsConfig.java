package com.openelements.octobird.config;

/**
 * Maximum number of open issue assignments allowed per user type.
 *
 * @param normalUserMax maximum open assignments for normal users
 * @param spamUserMax   maximum open assignments for spam-listed users
 */
public record AssignmentLimitsConfig(int normalUserMax, int spamUserMax) {

    /**
     * Returns the default assignment limits.
     *
     * @return a {@code AssignmentLimitsConfig} with standard limits
     */
    public static AssignmentLimitsConfig defaults() {
        return new AssignmentLimitsConfig(2, 1);
    }
}
