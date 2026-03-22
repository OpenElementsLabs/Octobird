"use client";

import { usePathname } from "next/navigation";
import Header from "@/components/header";

interface RepoLayoutProps {
  children: React.ReactNode;
  params: Promise<{ owner: string; repo: string }>;
}

const NAV_ITEMS = [
  { label: "Configuration", path: "config" },
  { label: "Spam Users", path: "spam-users" },
  { label: "Mentors", path: "mentors" },
  { label: "Activity Log", path: "activity" },
];

export default function RepoLayout({ children, params }: RepoLayoutProps) {
  const pathname = usePathname();

  // Extract owner/repo from pathname since params is async in Next.js 15
  const segments = pathname.split("/");
  const owner = segments[2] || "";
  const repo = segments[3] || "";
  const repoFullName = `${owner}/${repo}`;
  const basePath = `/repos/${owner}/${repo}`;

  return (
    <>
      <Header />
      <div className="mx-auto max-w-6xl p-6">
        {/* Page header */}
        <div className="mb-6 flex items-center gap-3">
          <a
            href="/"
            className="text-oe-mid-gray transition-colors hover:text-oe-dark"
            aria-label="Back to repositories"
          >
            <svg
              className="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={2}
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15.75 19.5L8.25 12l7.5-7.5"
              />
            </svg>
          </a>
          <h2 className="font-heading text-xl font-bold text-oe-dark">
            {repoFullName}
          </h2>
        </div>

        <div className="flex gap-8">
          {/* Sidebar */}
          <nav className="w-52 shrink-0">
            <ul className="space-y-1">
              {NAV_ITEMS.map((item) => {
                const href = `${basePath}/${item.path}`;
                const isActive = pathname.startsWith(href);
                return (
                  <li key={item.path}>
                    <a
                      href={href}
                      className={`block rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${
                        isActive
                          ? "bg-oe-green text-white"
                          : "text-oe-dark hover:bg-oe-light-gray"
                      }`}
                    >
                      {item.label}
                    </a>
                  </li>
                );
              })}
            </ul>
          </nav>

          {/* Content */}
          <main className="min-w-0 flex-1">{children}</main>
        </div>
      </div>
    </>
  );
}
