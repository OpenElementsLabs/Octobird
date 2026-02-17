package org.hiero.bot.config;

import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHRepository;

import java.io.IOException;

public class PermissionChecker {

    public boolean isMaintainer(final GHRepository repo, final String username) throws IOException {
        final GHPermissionType permission = getPermission(repo, username);
        return permission == GHPermissionType.ADMIN || permission == GHPermissionType.WRITE;
    }

    public GHPermissionType getPermission(final GHRepository repo, final String username) throws IOException {
        return repo.getPermission(username);
    }
}
