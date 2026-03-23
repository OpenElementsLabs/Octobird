import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, cleanup } from "@testing-library/react";
import Header from "./header";

beforeEach(() => {
  vi.restoreAllMocks();
});

afterEach(cleanup);

describe("Header", () => {
  it("displays Octobird branding", () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false }));
    render(<Header />);

    expect(screen.getByRole("heading", { name: "Octobird" })).toBeInTheDocument();
  });

  it("shows user avatar and login name when authenticated", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () =>
          Promise.resolve({
            login: "octocat",
            avatarUrl: "https://avatars.githubusercontent.com/u/583231",
          }),
      })
    );
    render(<Header />);

    await waitFor(() => {
      expect(screen.getByText("octocat")).toBeInTheDocument();
    });

    const avatar = screen.getByAltText("octocat") as HTMLImageElement;
    expect(avatar.src).toBe("https://avatars.githubusercontent.com/u/583231");
  });

  it("shows a Logout link pointing to /auth/logout", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () =>
          Promise.resolve({ login: "octocat", avatarUrl: "https://avatar" }),
      })
    );
    render(<Header />);

    await waitFor(() => {
      expect(screen.getByText("Logout")).toBeInTheDocument();
    });

    const logoutLink = screen.getByText("Logout").closest("a");
    expect(logoutLink).toHaveAttribute("href", "/auth/logout");
  });

  it("does not show user info when not authenticated", async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: false, status: 401 });
    vi.stubGlobal("fetch", fetchMock);
    render(<Header />);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalled();
    });

    // Give React time to process the response
    await new Promise((r) => setTimeout(r, 50));

    expect(screen.queryByText("Logout")).not.toBeInTheDocument();
  });
});
