import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  fetchRepos,
  fetchRepoConfig,
  saveRepoConfig,
  fetchSpamUsers,
  saveSpamUsers,
  fetchMentors,
  saveMentors,
  fetchAuditLog,
} from "./api";
import type { RepoConfig, GitHubAccount, AuditLogPage } from "./types";

function mockFetch(body: unknown, status = 200) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    statusText: status === 200 ? "OK" : "Error",
    json: () => Promise.resolve(body),
  });
}

beforeEach(() => {
  vi.stubGlobal("fetch", mockFetch([]));
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("fetchRepos", () => {
  it("calls GET /api/repos", async () => {
    const data = ["owner/repo-a", "owner/repo-b"];
    vi.stubGlobal("fetch", mockFetch(data));

    const result = await fetchRepos();

    expect(fetch).toHaveBeenCalledWith("/api/repos", expect.objectContaining({ credentials: "same-origin" }));
    expect(result).toEqual(data);
  });
});

describe("fetchRepoConfig", () => {
  it("calls GET /api/repos/{owner}/{repo}/config", async () => {
    const config = { repoId: 1, repoFullName: "owner/repo" } as RepoConfig;
    vi.stubGlobal("fetch", mockFetch(config));

    const result = await fetchRepoConfig("owner", "repo");

    expect(fetch).toHaveBeenCalledWith("/api/repos/owner/repo/config", expect.any(Object));
    expect(result).toEqual(config);
  });
});

describe("saveRepoConfig", () => {
  it("calls PUT /api/repos/{owner}/{repo}/config with JSON body", async () => {
    vi.stubGlobal("fetch", mockFetch(null, 204));
    const config = { repoId: 1 } as RepoConfig;

    await saveRepoConfig("owner", "repo", config);

    expect(fetch).toHaveBeenCalledWith(
      "/api/repos/owner/repo/config",
      expect.objectContaining({
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(config),
      })
    );
  });
});

describe("fetchSpamUsers", () => {
  it("calls GET /api/repos/{owner}/{repo}/spam-users", async () => {
    const users: GitHubAccount[] = [{ username: "spammer", githubId: 123 }];
    vi.stubGlobal("fetch", mockFetch(users));

    const result = await fetchSpamUsers("owner", "repo");

    expect(fetch).toHaveBeenCalledWith("/api/repos/owner/repo/spam-users", expect.any(Object));
    expect(result).toEqual(users);
  });
});

describe("saveSpamUsers", () => {
  it("calls PUT /api/repos/{owner}/{repo}/spam-users", async () => {
    vi.stubGlobal("fetch", mockFetch(null, 204));
    const users: GitHubAccount[] = [{ username: "spammer", githubId: 123 }];

    await saveSpamUsers("owner", "repo", users);

    expect(fetch).toHaveBeenCalledWith(
      "/api/repos/owner/repo/spam-users",
      expect.objectContaining({ method: "PUT", body: JSON.stringify(users) })
    );
  });
});

describe("fetchMentors", () => {
  it("calls GET /api/repos/{owner}/{repo}/mentors", async () => {
    const mentors: GitHubAccount[] = [{ username: "alice", githubId: 456 }];
    vi.stubGlobal("fetch", mockFetch(mentors));

    const result = await fetchMentors("owner", "repo");

    expect(fetch).toHaveBeenCalledWith("/api/repos/owner/repo/mentors", expect.any(Object));
    expect(result).toEqual(mentors);
  });
});

describe("saveMentors", () => {
  it("calls PUT /api/repos/{owner}/{repo}/mentors", async () => {
    vi.stubGlobal("fetch", mockFetch(null, 204));
    const mentors: GitHubAccount[] = [{ username: "alice", githubId: 456 }];

    await saveMentors("owner", "repo", mentors);

    expect(fetch).toHaveBeenCalledWith(
      "/api/repos/owner/repo/mentors",
      expect.objectContaining({ method: "PUT", body: JSON.stringify(mentors) })
    );
  });
});

describe("fetchAuditLog", () => {
  const auditPage: AuditLogPage = {
    entries: [{ id: "1", handlerName: "H", action: "a", target: "t", timestamp: "2026-03-20T14:30:00Z", details: "d" }],
    total: 1,
    offset: 0,
    limit: 25,
  };

  it("calls GET /api/repos/{owner}/{repo}/audit-log without filters", async () => {
    vi.stubGlobal("fetch", mockFetch(auditPage));

    await fetchAuditLog("owner", "repo");

    expect(fetch).toHaveBeenCalledWith("/api/repos/owner/repo/audit-log", expect.any(Object));
  });

  it("appends filter parameters to URL", async () => {
    vi.stubGlobal("fetch", mockFetch(auditPage));

    await fetchAuditLog("owner", "repo", {
      handler: "assign",
      action: "comment",
      dateFrom: "2026-03-01",
      dateTo: "2026-03-15",
      offset: 25,
      limit: 25,
    });

    const calledUrl = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][0] as string;
    expect(calledUrl).toContain("handler=assign");
    expect(calledUrl).toContain("action=comment");
    expect(calledUrl).toContain("dateFrom=2026-03-01");
    expect(calledUrl).toContain("dateTo=2026-03-15");
    expect(calledUrl).toContain("offset=25");
    expect(calledUrl).toContain("limit=25");
  });

  it("omits undefined filter parameters", async () => {
    vi.stubGlobal("fetch", mockFetch(auditPage));

    await fetchAuditLog("owner", "repo", { handler: "assign" });

    const calledUrl = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][0] as string;
    expect(calledUrl).toContain("handler=assign");
    expect(calledUrl).not.toContain("action=");
    expect(calledUrl).not.toContain("dateFrom=");
    expect(calledUrl).not.toContain("dateTo=");
  });
});

describe("error handling", () => {
  it("throws on 401 and redirects to /login", async () => {
    // Mock window.location
    const locationMock = { href: "" };
    vi.stubGlobal("window", { location: locationMock });
    vi.stubGlobal("fetch", mockFetch(null, 401));

    await expect(fetchRepos()).rejects.toThrow("Authentication required");
    expect(locationMock.href).toBe("/login");
  });

  it("throws on 403 with permissions message", async () => {
    vi.stubGlobal("fetch", mockFetch(null, 403));

    await expect(fetchRepos()).rejects.toThrow("Insufficient permissions");
  });

  it("throws on 500 with API error message", async () => {
    vi.stubGlobal("fetch", mockFetch(null, 500));

    await expect(fetchRepos()).rejects.toThrow("API error: 500");
  });
});
