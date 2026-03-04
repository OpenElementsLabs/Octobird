package com.openelements.octobird.model;

/**
 * Represents a GitHub App installation. Included in webhook payloads to identify which
 * installation of the App triggered the event.
 *
 * @param id    unique identifier of the installation
 * @param appId identifier of the GitHub App that was installed
 * @see <a href="https://docs.github.com/en/rest/apps/installations#get-an-installation-for-the-authenticated-app">GitHub REST API &ndash; App Installations</a>
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#installation">GitHub Webhooks &ndash; installation object</a>
 */
public record Installation(long id, long appId) {
}
