package com.openelements.octobird.rest;

import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import com.openelements.octobird.persistence.entity.AuditLogEntity;
import com.openelements.octobird.persistence.repository.AuditLogRepository;
import com.openelements.octobird.scheduled.RepoRegistry;
import com.openelements.octobird.service.AuditLogService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * REST endpoint for viewing audit log entries with optional filtering and pagination.
 *
 * <ul>
 *   <li>{@code GET /api/repos/{owner}/{repo}/audit-log} - list audit entries (filterable, paginated)</li>
 * </ul>
 */
public class AuditLogApiService implements HttpService {

    private static final int DEFAULT_LIMIT = 25;

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
            JsonHelper.sendJson(res, new AuditLogPageDto(List.of(), 0, 0, DEFAULT_LIMIT));
            return;
        }

        final String handler = req.query().first("handler").orElse(null);
        final String action = req.query().first("action").orElse(null);
        final int offset = req.query().first("offset").map(Integer::parseInt).orElse(0);
        final int limit = req.query().first("limit").map(Integer::parseInt).orElse(DEFAULT_LIMIT);

        LocalDate dateFrom = null;
        LocalDate dateTo = null;
        try {
            final String dateFromStr = req.query().first("dateFrom").orElse(null);
            if (dateFromStr != null) {
                dateFrom = LocalDate.parse(dateFromStr);
            }
            final String dateToStr = req.query().first("dateTo").orElse(null);
            if (dateToStr != null) {
                dateTo = LocalDate.parse(dateToStr);
            }
        } catch (final DateTimeParseException e) {
            res.status(Status.BAD_REQUEST_400).send("Invalid date format. Use ISO-8601 (YYYY-MM-DD).");
            return;
        }

        final AuditLogRepository.FilteredResult result = auditLogService.findFiltered(
                repoId, handler, action, dateFrom, dateTo, offset, limit);

        final List<AuditLogPageDto.AuditLogEntryDto> entries = result.entries().stream()
                .map(e -> new AuditLogPageDto.AuditLogEntryDto(
                        e.getId() != null ? e.getId().toString() : "",
                        e.getHandlerName(),
                        e.getAction(),
                        e.getTarget(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toString() : "",
                        e.getDetails()
                ))
                .toList();

        JsonHelper.sendJson(res, new AuditLogPageDto(entries, result.total(), offset, limit));
    }

    private static String extractRepoFullName(final ServerRequest req) {
        return req.path().pathParameters().get("owner") + "/" + req.path().pathParameters().get("repo");
    }
}
