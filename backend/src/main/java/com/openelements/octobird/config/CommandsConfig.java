package com.openelements.octobird.config;

import java.util.regex.Pattern;

/**
 * Regex patterns used to recognize bot commands in issue comments.
 *
 * @param assignPattern   regex for the /assign command
 * @param unassignPattern regex for the /unassign command
 * @param workingPattern  regex for the /working command
 */
public record CommandsConfig(String assignPattern, String unassignPattern, String workingPattern) {

    /**
     * Returns the default command patterns.
     *
     * @return a {@code CommandsConfig} with standard regex patterns
     */
    public static CommandsConfig defaults() {
        return new CommandsConfig("/assign\\b", "(^|\\s)/unassign(\\s|$)", "(^|\\s)/working(\\s|$)");
    }

    /**
     * Compiles the assign pattern.
     *
     * @return a compiled {@link Pattern} for detecting /assign commands
     */
    public Pattern compiledAssignPattern() {
        return Pattern.compile(assignPattern);
    }

    /**
     * Compiles the unassign pattern with case-insensitive matching.
     *
     * @return a compiled {@link Pattern} for detecting /unassign commands
     */
    public Pattern compiledUnassignPattern() {
        return Pattern.compile(unassignPattern, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Compiles the working pattern with case-insensitive matching.
     *
     * @return a compiled {@link Pattern} for detecting /working commands
     */
    public Pattern compiledWorkingPattern() {
        return Pattern.compile(workingPattern, Pattern.CASE_INSENSITIVE);
    }
}
