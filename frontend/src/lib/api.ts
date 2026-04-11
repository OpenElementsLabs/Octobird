import type { GitHubAccountDto, RepoConfig } from "@/types/config";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function fetchJson<T>(path: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`);
  if (!res.ok) {
    throw new ApiError(res.status, `GET ${path} failed: ${res.status}`);
  }
  return res.json() as Promise<T>;
}

async function putJson(path: string, body: unknown): Promise<void> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    throw new ApiError(res.status, `PUT ${path} failed: ${res.status}`);
  }
}

export async function fetchRepos(): Promise<string[]> {
  return fetchJson<string[]>("/api/repos");
}

export async function fetchConfig(
  owner: string,
  repo: string,
): Promise<RepoConfig> {
  return fetchJson<RepoConfig>(`/api/repos/${owner}/${repo}/config`);
}

export async function saveConfig(
  owner: string,
  repo: string,
  config: RepoConfig,
): Promise<void> {
  return putJson(`/api/repos/${owner}/${repo}/config`, config);
}

export async function fetchSpamUsers(
  owner: string,
  repo: string,
): Promise<GitHubAccountDto[]> {
  return fetchJson<GitHubAccountDto[]>(
    `/api/repos/${owner}/${repo}/spam-users`,
  );
}

export async function saveSpamUsers(
  owner: string,
  repo: string,
  users: GitHubAccountDto[],
): Promise<void> {
  return putJson(`/api/repos/${owner}/${repo}/spam-users`, users);
}

export async function fetchMentors(
  owner: string,
  repo: string,
): Promise<GitHubAccountDto[]> {
  return fetchJson<GitHubAccountDto[]>(
    `/api/repos/${owner}/${repo}/mentors`,
  );
}

export async function saveMentors(
  owner: string,
  repo: string,
  mentors: GitHubAccountDto[],
): Promise<void> {
  return putJson(`/api/repos/${owner}/${repo}/mentors`, mentors);
}

export { ApiError };