"use client";

import { useEffect, useState } from "react";
import Header from "@/components/header";
import { fetchRepos } from "@/lib/api";

export default function Home() {
  const [repos, setRepos] = useState<string[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadRepos = () => {
    setError(null);
    setRepos(null);
    fetchRepos()
      .then((data) => setRepos([...data].sort()))
      .catch((err) => setError(err.message));
  };

  useEffect(() => {
    loadRepos();
  }, []);

  return (
    <>
      <Header />
      <main className="mx-auto max-w-4xl p-8">
        <h2 className="font-heading text-2xl font-bold text-oe-dark mb-6">
          Repositories
        </h2>

        {/* Loading state */}
        {repos === null && error === null && (
          <div className="space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="h-14 animate-pulse rounded-lg bg-oe-light-gray"
              />
            ))}
          </div>
        )}

        {/* Error state */}
        {error && (
          <div className="rounded-lg border border-oe-red/20 bg-oe-red/5 p-6 text-center">
            <p className="text-oe-red font-medium mb-3">
              Failed to load repositories. Please try again.
            </p>
            <button
              onClick={loadRepos}
              className="rounded-lg bg-oe-green px-4 py-2 text-sm text-white font-medium transition-colors hover:bg-oe-green-dark"
            >
              Retry
            </button>
          </div>
        )}

        {/* Empty state */}
        {repos !== null && repos.length === 0 && (
          <div className="rounded-lg border border-oe-mid-gray/30 bg-oe-light-gray/50 p-8 text-center">
            <p className="text-oe-mid-gray">
              No repositories found. Install Octobird on a GitHub repository to
              get started.
            </p>
          </div>
        )}

        {/* Success state */}
        {repos !== null && repos.length > 0 && (
          <div className="space-y-2">
            {repos.map((name) => (
              <a
                key={name}
                href={`/repos/${name}/config`}
                className="flex items-center justify-between rounded-lg border border-oe-light-gray bg-white p-4 transition-all hover:border-oe-green hover:shadow-md"
              >
                <span className="font-medium text-oe-dark">{name}</span>
                <svg
                  className="h-5 w-5 text-oe-mid-gray"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={2}
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M8.25 4.5l7.5 7.5-7.5 7.5"
                  />
                </svg>
              </a>
            ))}
          </div>
        )}
      </main>
    </>
  );
}
