package org.hiero.bot.config;

import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PermissionChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PermissionChecker.class);

    public boolean isMaintainer(final GHRepository repo, final String username) throws IOException {
        final GHPermissionType permission = getPermission(repo, username);
        return permission == GHPermissionType.ADMIN || permission == GHPermissionType.WRITE;
    }

    public boolean isExemptFromGuard(final GHRepository repo, final String username) throws IOException {
        final GHPermissionType permission = getPermission(repo, username);
        return permission == GHPermissionType.ADMIN || permission == GHPermissionType.WRITE;
    }

    public boolean isCollaborator(final GHRepository repo, final String username) {
        try {
            final GHPermissionType permission = getPermission(repo, username);
            return permission != GHPermissionType.NONE;
        } catch (final IOException e) {
            LOG.debug("Failed to check collaborator status for {} in {}", username, repo.getFullName(), e);
            return false;
        }
    }

    public GHPermissionType getPermission(final GHRepository repo, final String username) throws IOException {
        return repo.getPermission(username);
    }
}
