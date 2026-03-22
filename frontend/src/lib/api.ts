/**
 * Centralized API client for Octobird backend.
 * Handles authentication redirects and error responses.
 */

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
