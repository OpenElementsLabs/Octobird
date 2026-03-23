package com.openelements.octobird.config;

import io.helidon.config.Config;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves configuration values from process environment variables, a local {@code .env} file,
 * or Helidon config defaults.
 */
public final class ConfigValueResolver {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("^\\$\\{([A-Za-z_][A-Za-z0-9_]*)(?::(.*))?}$");
    private static final Map<String, String> LOCAL_ENV = LocalEnvFile.load();

    private ConfigValueResolver() {
    }

    /**
     * Resolves a string value, using the real process environment first, then a nearby
     * {@code .env}, and finally the supplied config node.
     *
     * @param config        the config node to read from
     * @param key           the child key to resolve
     * @param envName       the environment variable name
     * @param defaultValue  the default value if nothing else is set
     * @return the resolved string value
     */
    public static String resolveString(final Config config, final String key, final String envName,
                                       final String defaultValue) {
        return resolveString(config, key, envName, defaultValue, System.getenv(), LOCAL_ENV);
    }

    static String resolveString(final Config config, final String key, final String envName,
                                final String defaultValue, final Map<String, String> environment,
                                final Map<String, String> localEnv) {
        final String explicitValue = lookupString(environment, localEnv, envName);
        if (explicitValue != null) {
            return normalize(explicitValue);
        }

        final String rawValue = config.get(key).asString().orElse(defaultValue);
        final String placeholderValue = resolvePlaceholder(rawValue, environment, localEnv);
        if (placeholderValue != null) {
            return normalize(placeholderValue);
        }
        return normalize(rawValue == null ? defaultValue : rawValue);
    }

    /**
     * Resolves a numeric value with the same precedence rules as {@link #resolveString(Config,
     * String, String, String)}.
     *
     * @param config        the config node to read from
     * @param key           the child key to resolve
     * @param envName       the environment variable name
     * @param defaultValue  the default value if nothing else is set
     * @return the resolved numeric value
     */
    public static long resolveLong(final Config config, final String key, final String envName,
                                   final long defaultValue) {
        return resolveLong(config, key, envName, defaultValue, System.getenv(), LOCAL_ENV);
    }

    static long resolveLong(final Config config, final String key, final String envName,
                            final long defaultValue, final Map<String, String> environment,
                            final Map<String, String> localEnv) {
        final String explicitValue = lookupNumeric(environment, localEnv, envName);
        if (explicitValue != null) {
            return Long.parseLong(explicitValue.trim());
        }

        final String rawValue = config.get(key).asString().orElse(Long.toString(defaultValue));
        final String placeholderValue = resolvePlaceholder(rawValue, environment, localEnv);
        final String resolvedValue = placeholderValue == null ? rawValue : placeholderValue;
        if (resolvedValue == null || resolvedValue.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(resolvedValue.trim());
    }

    static String resolvePlaceholder(final String rawValue, final Map<String, String> environment,
                                     final Map<String, String> localEnv) {
        if (rawValue == null) {
            return null;
        }

        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(rawValue.trim());
        if (!matcher.matches()) {
            return null;
        }

        final String envName = matcher.group(1);
        final String explicitValue = lookupString(environment, localEnv, envName);
        if (explicitValue != null) {
            return explicitValue;
        }

        final String defaultValue = matcher.group(2);
        return defaultValue == null ? "" : stripQuotes(defaultValue);
    }

    static String normalize(final String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\n", "\n");
    }

    private static String lookupString(final Map<String, String> environment,
                                       final Map<String, String> localEnv,
                                       final String envName) {
        if (environment.containsKey(envName)) {
            return environment.get(envName);
        }
        if (localEnv.containsKey(envName)) {
            return localEnv.get(envName);
        }
        return null;
    }

    private static String lookupNumeric(final Map<String, String> environment,
                                        final Map<String, String> localEnv,
                                        final String envName) {
        final String environmentValue = lookupString(environment, localEnv, envName);
        if (environmentValue == null || environmentValue.isBlank()) {
            return null;
        }
        return environmentValue;
    }

    private static String stripQuotes(final String value) {
        if (value.length() >= 2) {
            final char first = value.charAt(0);
            final char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
