import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import Home from "./page";

// Mock the Header component to avoid its fetch call
vi.mock("@/components/header", () => ({
  default: () => <div data-testid="header">Header</div>,
}));

// Mock the api module
vi.mock("@/lib/api", () => ({
  fetchRepos: vi.fn(),
}));

import { fetchRepos } from "@/lib/api";
const mockFetchRepos = vi.mocked(fetchRepos);

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(cleanup);

describe("Home page", () => {
  it("shows loading skeleton while fetching", () => {
    mockFetchRepos.mockReturnValue(new Promise(() => {}));
    render(<Home />);

    const skeletons = document.querySelectorAll(".animate-pulse");
    expect(skeletons.length).toBe(4);
  });

  it("displays repositories after loading", async () => {
    mockFetchRepos.mockResolvedValue(["owner/repo-a", "owner/repo-b"]);
    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText("owner/repo-a")).toBeInTheDocument();
      expect(screen.getByText("owner/repo-b")).toBeInTheDocument();
    });
  });

  it("repositories are sorted alphabetically", async () => {
    mockFetchRepos.mockResolvedValue(["org/zebra", "org/alpha", "org/middle"]);
    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText("org/alpha")).toBeInTheDocument();
    });

    // Check order by comparing DOM positions
    const links = screen.getAllByRole("link").filter(
      (el) => el.getAttribute("href")?.startsWith("/repos/")
    );
    const names = links.map((l) => l.querySelector("span")?.textContent);
    expect(names).toEqual(["org/alpha", "org/middle", "org/zebra"]);
  });

  it("repository entries link to config page", async () => {
    mockFetchRepos.mockResolvedValue(["owner/repo-a"]);
    render(<Home />);

    await waitFor(() => {
      const link = screen.getByText("owner/repo-a").closest("a");
      expect(link).toHaveAttribute("href", "/repos/owner/repo-a/config");
    });
  });

  it("shows empty state when no repos", async () => {
    mockFetchRepos.mockResolvedValue([]);
    render(<Home />);

    await waitFor(() => {
      expect(
        screen.getByText(
          /No repositories found/
        )
      ).toBeInTheDocument();
    });
  });

  it("shows error state on API failure", async () => {
    mockFetchRepos.mockRejectedValue(new Error("Server error"));
    render(<Home />);

    await waitFor(() => {
      expect(
        screen.getByText("Failed to load repositories. Please try again.")
      ).toBeInTheDocument();
    });
  });

  it("shows retry button on error", async () => {
    mockFetchRepos.mockRejectedValue(new Error("Server error"));
    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText("Retry")).toBeInTheDocument();
    });
  });

  it("retry button triggers reload", async () => {
    const user = userEvent.setup();
    mockFetchRepos.mockRejectedValueOnce(new Error("Server error"));
    render(<Home />);

    await waitFor(() => {
      expect(screen.getByText("Retry")).toBeInTheDocument();
    });

    mockFetchRepos.mockResolvedValueOnce(["owner/repo-a"]);
    await user.click(screen.getByText("Retry"));

    await waitFor(() => {
      expect(screen.getByText("owner/repo-a")).toBeInTheDocument();
    });
    expect(mockFetchRepos).toHaveBeenCalledTimes(2);
  });

  it("displays the page title", () => {
    mockFetchRepos.mockReturnValue(new Promise(() => {}));
    render(<Home />);

    expect(screen.getByRole("heading", { name: "Repositories" })).toBeInTheDocument();
  });
});
