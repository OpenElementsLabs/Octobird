import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, cleanup, act, fireEvent } from "@testing-library/react";
import ActivityPage from "./page";
import type { AuditLogPage } from "@/lib/types";

// Mock next/navigation
vi.mock("next/navigation", () => ({
  useParams: () => ({ owner: "owner", repo: "repo" }),
}));

// Mock the api module
vi.mock("@/lib/api", () => ({
  fetchAuditLog: vi.fn(),
}));

import { fetchAuditLog } from "@/lib/api";
const mockFetchAuditLog = vi.mocked(fetchAuditLog);

const EMPTY_PAGE: AuditLogPage = { entries: [], total: 0, offset: 0, limit: 25 };

function makePage(count: number, total: number, offset = 0): AuditLogPage {
  return {
    entries: Array.from({ length: count }, (_, i) => ({
      id: `entry-${offset + i}`,
      handlerName: `Handler${offset + i}`,
      action: `action-${offset + i}`,
      target: `owner/repo#${offset + i}`,
      timestamp: `2026-03-20T14:${String(i).padStart(2, "0")}:00Z`,
      details: `Detail ${offset + i}`,
    })),
    total,
    offset,
    limit: 25,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(cleanup);

describe("ActivityPage", () => {
  describe("table display", () => {
    it("shows loading indicator while fetching", () => {
      mockFetchAuditLog.mockReturnValue(new Promise(() => {}));
      render(<ActivityPage />);

      expect(screen.getByText("Loading...")).toBeInTheDocument();
    });

    it("shows entries in a table with correct columns", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(2, 2));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("Handler0")).toBeInTheDocument();
      });

      // Check table headers (th elements)
      const headers = document.querySelectorAll("th");
      const headerTexts = Array.from(headers).map((h) => h.textContent);
      expect(headerTexts).toContain("Timestamp");
      expect(headerTexts).toContain("Handler");
      expect(headerTexts).toContain("Action");
    });

    it("shows empty state message", async () => {
      mockFetchAuditLog.mockResolvedValue(EMPTY_PAGE);
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("No activity log entries found")).toBeInTheDocument();
      });
    });

    it("formats timestamp as YYYY-MM-DD HH:mm", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(1, 1));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("2026-03-20 14:00")).toBeInTheDocument();
      });
    });
  });

  describe("expandable detail rows", () => {
    it("expands a row to show target and details", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(1, 1));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("Handler0")).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText("Handler0"));

      expect(screen.getByText("owner/repo#0")).toBeInTheDocument();
      expect(screen.getByText("Detail 0")).toBeInTheDocument();
    });

    it("collapses an expanded row on second click", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(1, 1));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("Handler0")).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText("Handler0"));
      expect(screen.getByText("Detail 0")).toBeInTheDocument();

      fireEvent.click(screen.getByText("Handler0"));
      expect(screen.queryByText("Detail 0")).not.toBeInTheDocument();
    });

    it("only one row expanded at a time", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(2, 2));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("Handler0")).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText("Handler0"));
      expect(screen.getByText("Detail 0")).toBeInTheDocument();

      fireEvent.click(screen.getByText("Handler1"));
      expect(screen.queryByText("Detail 0")).not.toBeInTheDocument();
      expect(screen.getByText("Detail 1")).toBeInTheDocument();
    });
  });

  describe("pagination", () => {
    it("shows pagination info text", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(25, 50));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText(/Showing 1-25 of 50/)).toBeInTheDocument();
      });
    });

    it("disables Previous button on first page", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(25, 50));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("Previous")).toBeDisabled();
      });
    });

    it("enables Next button when more pages exist", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(25, 50));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("Next")).toBeEnabled();
      });
    });

    it("disables Next button on last page", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(10, 10));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("Next")).toBeDisabled();
      });
    });

    it("navigates to next page on click", async () => {
      mockFetchAuditLog.mockResolvedValue(makePage(25, 50));
      render(<ActivityPage />);

      await waitFor(() => {
        expect(screen.getByText("Next")).toBeEnabled();
      });

      mockFetchAuditLog.mockClear();
      mockFetchAuditLog.mockResolvedValue(makePage(25, 50, 25));

      fireEvent.click(screen.getByText("Next"));

      await waitFor(() => {
        expect(mockFetchAuditLog).toHaveBeenCalledWith(
          "owner", "repo",
          expect.objectContaining({ offset: 25 })
        );
      });
    });
  });

  describe("text filters with debounce", () => {
    it("calls API with handler filter after debounce", async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      mockFetchAuditLog.mockResolvedValue(EMPTY_PAGE);
      render(<ActivityPage />);

      // Wait for initial load
      await act(async () => {
        vi.advanceTimersByTime(350);
      });
      mockFetchAuditLog.mockClear();

      // Type in handler filter
      const handlerInput = screen.getByPlaceholderText("Filter by handler...");
      fireEvent.change(handlerInput, { target: { value: "assign" } });

      // Advance past debounce
      await act(async () => {
        vi.advanceTimersByTime(350);
      });

      await waitFor(() => {
        expect(mockFetchAuditLog).toHaveBeenCalledWith(
          "owner", "repo",
          expect.objectContaining({ handler: "assign" })
        );
      });

      vi.useRealTimers();
    });

    it("calls API with action filter after debounce", async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      mockFetchAuditLog.mockResolvedValue(EMPTY_PAGE);
      render(<ActivityPage />);

      await act(async () => {
        vi.advanceTimersByTime(350);
      });
      mockFetchAuditLog.mockClear();

      const actionInput = screen.getByPlaceholderText("Filter by action...");
      fireEvent.change(actionInput, { target: { value: "comment" } });

      await act(async () => {
        vi.advanceTimersByTime(350);
      });

      await waitFor(() => {
        expect(mockFetchAuditLog).toHaveBeenCalledWith(
          "owner", "repo",
          expect.objectContaining({ action: "comment" })
        );
      });

      vi.useRealTimers();
    });

    it("resets pagination to offset=0 on filter change", async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true });
      mockFetchAuditLog.mockResolvedValue(makePage(25, 50));
      render(<ActivityPage />);

      await act(async () => {
        vi.advanceTimersByTime(350);
      });

      // Navigate to page 2
      mockFetchAuditLog.mockResolvedValue(makePage(25, 50, 25));
      fireEvent.click(screen.getByText("Next"));

      await act(async () => {
        vi.advanceTimersByTime(50);
      });

      mockFetchAuditLog.mockClear();
      mockFetchAuditLog.mockResolvedValue(EMPTY_PAGE);

      // Change filter - should reset to offset 0
      const handlerInput = screen.getByPlaceholderText("Filter by handler...");
      fireEvent.change(handlerInput, { target: { value: "a" } });

      await act(async () => {
        vi.advanceTimersByTime(350);
      });

      await waitFor(() => {
        expect(mockFetchAuditLog).toHaveBeenCalledWith(
          "owner", "repo",
          expect.objectContaining({ offset: 0 })
        );
      });

      vi.useRealTimers();
    });
  });

  describe("filter inputs exist", () => {
    it("has handler, action, from and to filter inputs", () => {
      mockFetchAuditLog.mockReturnValue(new Promise(() => {}));
      render(<ActivityPage />);

      expect(screen.getByPlaceholderText("Filter by handler...")).toBeInTheDocument();
      expect(screen.getByPlaceholderText("Filter by action...")).toBeInTheDocument();
      // Date inputs don't have placeholders but their labels are visible
      expect(screen.getByText("From")).toBeInTheDocument();
      expect(screen.getByText("To")).toBeInTheDocument();
      // Verify there are 2 date inputs
      const dateInputs = document.querySelectorAll('input[type="date"]');
      expect(dateInputs.length).toBe(2);
    });
  });
});
