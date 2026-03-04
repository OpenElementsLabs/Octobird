package com.openelements.octobird.util;

import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utility for checking a GitHub user's permission level on a repository. Used by handlers to
 * decide whether to apply assignment limits or prerequisite guards.
 */
public final class PermissionChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PermissionChecker.class);

    private PermissionChecker() {
    }

    /**
     * Returns {@code true} if the user has {@code ADMIN} or {@code WRITE} permission on the
     * repository, qualifying them as a maintainer.
     *
     * @param repo     the repository to check
     * @param username the GitHub login to check
     * @return {@code true} if the user is a maintainer
     * @throws IOException if the permission cannot be fetched
     */
    public static boolean isMaintainer(final GHRepository repo, final String username) throws IOException {
        final GHPermissionType permission = getPermission(repo, username);
        return permission == GHPermissionType.ADMIN || permission == GHPermissionType.WRITE;
    }

    /**
     * Returns {@code true} if the user should be exempt from assignment prerequisite guards
     * (i.e. has {@code ADMIN} or {@code WRITE} permission).
     *
     * @param repo     the repository to check
     * @param username the GitHub login to check
     * @return {@code true} if the user is exempt from guards
     * @throws IOException if the permission cannot be fetched
     */
    public static boolean isCommitterOfRepo(final GHRepository repo, final String username) throws IOException {
        final GHPermissionType permission = getPermission(repo, username);
        return permission == GHPermissionType.ADMIN || permission == GHPermissionType.WRITE;
    }

    /**
     * Returns {@code true} if the user has any explicit permission level on the repository
     * (i.e. not {@code NONE}). Returns {@code false} on error.
     *
     * @param repo     the repository to check
     * @param username the GitHub login to check
     * @return {@code true} if the user is a collaborator
     */
    public static boolean isCollaborator(final GHRepository repo, final String username) {
        try {
            final GHPermissionType permission = getPermission(repo, username);
            return permission != GHPermissionType.NONE;
        } catch (final IOException e) {
            LOG.debug("Failed to check collaborator status for {} in {}", username, repo.getFullName(), e);
            return false;
        }
    }

    /**
     * Returns the raw {@link GHPermissionType} for the user on the given repository.
     *
     * @param repo     the repository to query
     * @param username the GitHub login to query
     * @return the user's permission type
     * @throws IOException if the permission cannot be fetched
     */
    public static GHPermissionType getPermission(final GHRepository repo, final String username) throws IOException {
        return repo.getPermission(username);
    }
}
