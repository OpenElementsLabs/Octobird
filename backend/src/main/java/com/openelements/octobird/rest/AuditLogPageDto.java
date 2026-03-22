package com.openelements.octobird.rest;

import java.util.List;
import java.util.Objects;

/**
 * Paginated response envelope for audit log queries.
 *
 * @param entries the audit log entries for the current page
 * @param total   total number of matching entries across all pages
 * @param offset  the offset of the first entry in this page
 * @param limit   the maximum number of entries per page
 */
public record AuditLogPageDto(List<AuditLogEntryDto> entries, long total, int offset, int limit) {

    public AuditLogPageDto {
        Objects.requireNonNull(entries, "entries must not be null");
    }

    /**
     * A single audit log entry.
     *
     * @param id          the entry ID
     * @param handlerName the handler that performed the action
     * @param action      the action performed
     * @param target      the target (e.g. issue number, username)
     * @param timestamp   the timestamp as ISO-8601 string
     * @param details     additional details
     */
    public record AuditLogEntryDto(String id, String handlerName, String action,
                                    String target, String timestamp, String details) {
    }
}
