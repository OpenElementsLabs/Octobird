"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams } from "next/navigation";
import { fetchAuditLog } from "@/lib/api";
import type { AuditLogEntry, AuditLogFilters, AuditLogPage } from "@/lib/types";

export default function ActivityPage() {
  const params = useParams<{ owner: string; repo: string }>();
  const [page, setPage] = useState<AuditLogPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [expandedRow, setExpandedRow] = useState<string | null>(null);

  const [handlerFilter, setHandlerFilter] = useState("");
  const [actionFilter, setActionFilter] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [offset, setOffset] = useState(0);
  const limit = 25;

  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadData = useCallback(
    async (filters: AuditLogFilters) => {
      setLoading(true);
      try {
        const result = await fetchAuditLog(params.owner, params.repo, filters);
        setPage(result);
      } catch {
        setPage({ entries: [], total: 0, offset: 0, limit });
      } finally {
        setLoading(false);
      }
    },
    [params.owner, params.repo]
  );

  // Debounced text filter change
  const scheduleLoad = useCallback(
    (newOffset: number) => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
      debounceRef.current = setTimeout(() => {
        const filters: AuditLogFilters = {
          offset: newOffset,
          limit,
          handler: handlerFilter || undefined,
          action: actionFilter || undefined,
          dateFrom: dateFrom || undefined,
          dateTo: dateTo || undefined,
        };
        loadData(filters);
      }, 300);
    },
    [handlerFilter, actionFilter, dateFrom, dateTo, loadData]
  );

  // On filter text change → debounce + reset to page 1
  useEffect(() => {
    setOffset(0);
    scheduleLoad(0);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [handlerFilter, actionFilter, scheduleLoad]);

  // On date change → immediate load + reset to page 1
  useEffect(() => {
    setOffset(0);
    loadData({
      offset: 0,
      limit,
      handler: handlerFilter || undefined,
      action: actionFilter || undefined,
      dateFrom: dateFrom || undefined,
      dateTo: dateTo || undefined,
    });
  }, [dateFrom, dateTo, loadData]);

  // On offset change (pagination)
  useEffect(() => {
    loadData({
      offset,
      limit,
      handler: handlerFilter || undefined,
      action: actionFilter || undefined,
      dateFrom: dateFrom || undefined,
      dateTo: dateTo || undefined,
    });
  }, [offset]);

  const totalPages = page ? Math.ceil(page.total / limit) : 0;
  const currentPage = Math.floor(offset / limit) + 1;
  const showFrom = page && page.total > 0 ? offset + 1 : 0;
  const showTo = page ? Math.min(offset + limit, page.total) : 0;

  return (
    <div>
      <h3 className="font-heading text-lg font-bold text-oe-dark mb-4">
        Activity Log
      </h3>

      {/* Filters */}
      <div className="mb-4 grid gap-3 sm:grid-cols-4">
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            Handler
          </label>
          <input
            type="text"
            value={handlerFilter}
            onChange={(e) => setHandlerFilter(e.target.value)}
            placeholder="Filter by handler..."
            className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            Action
          </label>
          <input
            type="text"
            value={actionFilter}
            onChange={(e) => setActionFilter(e.target.value)}
            placeholder="Filter by action..."
            className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            From
          </label>
          <input
            type="date"
            value={dateFrom}
            onChange={(e) => setDateFrom(e.target.value)}
            className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            To
          </label>
          <input
            type="date"
            value={dateTo}
            onChange={(e) => setDateTo(e.target.value)}
            className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
      </div>

      {/* Loading */}
      {loading && (
        <div className="flex items-center gap-2 text-sm text-oe-mid-gray py-4">
          <div className="h-4 w-4 animate-spin rounded-full border-2 border-oe-green border-t-transparent" />
          Loading...
        </div>
      )}

      {/* Table */}
      {!loading && page && (
        <>
          {page.entries.length === 0 ? (
            <p className="text-oe-mid-gray py-8 text-center">
              No activity log entries found
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-oe-dark text-white text-left text-xs font-medium">
                    <th className="px-4 py-2.5 rounded-tl-lg">Timestamp</th>
                    <th className="px-4 py-2.5">Handler</th>
                    <th className="px-4 py-2.5 rounded-tr-lg">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {page.entries.map((entry, i) => (
                    <EntryRow
                      key={entry.id || i}
                      entry={entry}
                      isExpanded={expandedRow === entry.id}
                      onToggle={() =>
                        setExpandedRow(
                          expandedRow === entry.id ? null : entry.id
                        )
                      }
                      isEven={i % 2 === 0}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          {page.total > 0 && (
            <div className="mt-4 flex items-center justify-between text-sm">
              <span className="text-oe-mid-gray">
                Showing {showFrom}-{showTo} of {page.total}
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setOffset(Math.max(0, offset - limit))}
                  disabled={offset === 0}
                  className="rounded border border-oe-light-gray px-3 py-1 text-xs font-medium text-oe-dark transition-colors hover:bg-oe-light-gray disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <button
                  onClick={() => setOffset(offset + limit)}
                  disabled={currentPage >= totalPages}
                  className="rounded border border-oe-light-gray px-3 py-1 text-xs font-medium text-oe-dark transition-colors hover:bg-oe-light-gray disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function EntryRow({
  entry,
  isExpanded,
  onToggle,
  isEven,
}: {
  entry: AuditLogEntry;
  isExpanded: boolean;
  onToggle: () => void;
  isEven: boolean;
}) {
  const timestamp = entry.timestamp
    ? entry.timestamp.replace("T", " ").slice(0, 16)
    : "";

  return (
    <>
      <tr
        onClick={onToggle}
        className={`cursor-pointer border-b border-oe-light-gray/50 transition-colors hover:bg-oe-green/5 ${
          isEven ? "bg-white" : "bg-oe-light-gray/20"
        }`}
      >
        <td className="px-4 py-2.5 text-oe-mid-gray">{timestamp}</td>
        <td className="px-4 py-2.5 text-oe-dark">{entry.handlerName}</td>
        <td className="px-4 py-2.5 text-oe-dark">{entry.action}</td>
      </tr>
      {isExpanded && (
        <tr className="bg-oe-light-gray/30">
          <td colSpan={3} className="px-4 py-3 text-xs">
            <div className="grid gap-2 sm:grid-cols-2">
              <div>
                <span className="font-medium text-oe-mid-gray">Target:</span>{" "}
                <span className="text-oe-dark">{entry.target || "—"}</span>
              </div>
              <div>
                <span className="font-medium text-oe-mid-gray">Details:</span>{" "}
                <span className="text-oe-dark">{entry.details || "—"}</span>
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}
