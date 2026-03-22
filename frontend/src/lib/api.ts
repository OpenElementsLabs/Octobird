/**
 * Centralized API client for Octobird backend.
 * Handles authentication redirects and error responses.
 */

import type {
  AuditLogFilters,
  AuditLogPage,
  GitHubAccount,
  RepoConfig,
} from "./types";

async function apiFetch(path: string, options?: RequestInit): Promise<Response> {
  const res = await fetch(path, {
    ...options,
    credentials: "same-origin",
  });

  if (res.status === 401) {
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new Error("Authentication required");
  }

  if (res.status === 403) {
    throw new Error("Insufficient permissions");
  }

  if (!res.ok) {
    throw new Error(`API error: ${res.status} ${res.statusText}`);
  }

  return res;
}

export async function fetchRepos(): Promise<string[]> {
  const res = await apiFetch("/api/repos");
  return res.json();
}

export async function fetchRepoConfig(
  owner: string,
  repo: string
): Promise<RepoConfig> {
  const res = await apiFetch(`/api/repos/${owner}/${repo}/config`);
  return res.json();
}

export async function saveRepoConfig(
  owner: string,
  repo: string,
  config: RepoConfig
): Promise<void> {
  await apiFetch(`/api/repos/${owner}/${repo}/config`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(config),
  });
}

export async function fetchSpamUsers(
  owner: string,
  repo: string
): Promise<GitHubAccount[]> {
  const res = await apiFetch(`/api/repos/${owner}/${repo}/spam-users`);
  return res.json();
}

export async function saveSpamUsers(
  owner: string,
  repo: string,
  users: GitHubAccount[]
): Promise<void> {
  await apiFetch(`/api/repos/${owner}/${repo}/spam-users`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(users),
  });
}

export async function fetchMentors(
  owner: string,
  repo: string
): Promise<GitHubAccount[]> {
  const res = await apiFetch(`/api/repos/${owner}/${repo}/mentors`);
  return res.json();
}

export async function saveMentors(
  owner: string,
  repo: string,
  mentors: GitHubAccount[]
): Promise<void> {
  await apiFetch(`/api/repos/${owner}/${repo}/mentors`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(mentors),
  });
}

export async function fetchAuditLog(
  owner: string,
  repo: string,
  filters?: AuditLogFilters
): Promise<AuditLogPage> {
  const params = new URLSearchParams();
  if (filters?.handler) params.set("handler", filters.handler);
  if (filters?.action) params.set("action", filters.action);
  if (filters?.dateFrom) params.set("dateFrom", filters.dateFrom);
  if (filters?.dateTo) params.set("dateTo", filters.dateTo);
  if (filters?.offset !== undefined)
    params.set("offset", String(filters.offset));
  if (filters?.limit !== undefined) params.set("limit", String(filters.limit));

  const query = params.toString();
  const path = `/api/repos/${owner}/${repo}/audit-log${query ? `?${query}` : ""}`;
  const res = await apiFetch(path);
  return res.json();
}
