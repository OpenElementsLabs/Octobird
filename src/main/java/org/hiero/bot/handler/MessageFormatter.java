package org.hiero.bot.handler;

/**
 * Utility for formatting bot comment messages using SLF4J-style {@code {}} placeholders.
 *
 * <p>Each occurrence of {@code {}} in the template is replaced in order by the string
 * representation of the corresponding parameter. If there are more placeholders than
 * parameters the remaining placeholders are left as-is.
 *
 * <p>Example:
 * <pre>
 *   MessageFormatter.format("Hi @{}, you have {} open issues.", username, count)
 *   // → "Hi @alice, you have 3 open issues."
 * </pre>
 */
public final class MessageFormatter {

    private MessageFormatter() {
    }

    /**
     * Formats {@code template} by substituting each {@code {}} placeholder with the
     * corresponding entry from {@code params}.
     *
     * @param template the message template, may contain {@code {}} placeholders
     * @param params   the values to substitute; {@link Object#toString()} is called on each
     * @return the formatted string
     */
    public static String format(final String template, final Object... params) {
        if (params == null || params.length == 0) {
            return template;
        }
        final StringBuilder result = new StringBuilder(template.length() + 32);
        int paramIndex = 0;
        int start = 0;
        int pos;
        while (paramIndex < params.length && (pos = template.indexOf("{}", start)) >= 0) {
            result.append(template, start, pos);
            result.append(params[paramIndex++]);
            start = pos + 2;
        }
        result.append(template, start, template.length());
        return result.toString();
    }
}