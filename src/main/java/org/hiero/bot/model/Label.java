package org.hiero.bot.model;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a label attached to a GitHub issue or pull request.
 *
 * @param id          unique identifier of the label
 * @param name        display name of the label (e.g. {@code "bug"})
 * @param color       hex color code without leading {@code #} (e.g. {@code "ff0000"}), may be {@code null}
 * @param description optional description of the label, may be {@code null}
 * @see <a href="https://docs.github.com/en/rest/issues/labels#get-a-label">GitHub REST API &ndash; Labels</a>
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#label">GitHub Webhooks &ndash; label object</a>
 */
public record Label(long id, String name, @Nullable String color, @Nullable String description) {

    public Label {
        Objects.requireNonNull(name, "name must not be null");
    }
}
