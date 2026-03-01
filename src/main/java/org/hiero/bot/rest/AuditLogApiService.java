package org.hiero.bot.rest;

import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.hiero.bot.persistence.entity.AuditLogEntity;
import org.hiero.bot.scheduled.RepoRegistry;
import org.hiero.bot.service.AuditLogService;

import java.util.List;
import java.util.Objects;

/**
 * REST endpoint for viewing audit log entries.
 *
 * <ul>
 *   <li>{@code GET /api/repos/{owner}/{repo}/audit-log} - list recent audit entries</li>
 * </ul>
 */
public class AuditLogApiService implements HttpService {

    private static final int DEFAULT_LIMIT = 100;

    private final AuditLogService auditLogService;
    private final RepoRegistry repoRegistry;

    public AuditLogApiService(final AuditLogService auditLogService, final RepoRegistry repoRegistry) {
        this.auditLogService = Objects.requireNonNull(auditLogService, "auditLogService must not be null");
        this.repoRegistry = Objects.requireNonNull(repoRegistry, "repoRegistry must not be null");
    }

    @Override
    public void routing(final HttpRules rules) {
        rules.get("/{owner}/{repo}/audit-log", this::getAuditLog);
    }

    private void getAuditLog(final ServerRequest req, final ServerResponse res) {
        final String repoFullName = extractRepoFullName(req);
        final Long repoId = RepoRegistryLookup.resolveRepoId(repoRegistry, repoFullName);
        if (repoId == null) {
            JsonHelper.sendJson(res, List.of());
            return;
        }
        final int limit = req.query().first("limit").map(Integer::parseInt).orElse(DEFAULT_LIMIT);
        final List<AuditLogEntity> entries = auditLogService.findRecent(repoId, limit);
        JsonHelper.sendJson(res, entries);
    }

    private static String extractRepoFullName(final ServerRequest req) {
        return req.path().pathParameters().get("owner") + "/" + req.path().pathParameters().get("repo");
    }
}
