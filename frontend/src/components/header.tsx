"use client";

import { useEffect, useState } from "react";

interface UserInfo {
  login: string;
  avatarUrl: string;
}

export default function Header() {
  const [user, setUser] = useState<UserInfo | null>(null);

  useEffect(() => {
    fetch("/auth/me")
      .then((res) => {
        if (res.ok) return res.json();
        return null;
      })
      .then((data) => {
        if (data) setUser(data);
      })
      .catch(() => {});
  }, []);

  return (
    <header className="flex items-center justify-between bg-oe-dark px-6 py-3 text-white">
      <div className="flex items-center gap-3">
        <h1 className="font-heading text-xl font-bold tracking-tight">
          Octobird
        </h1>
      </div>
      {user && (
        <div className="flex items-center gap-3">
          <img
            src={user.avatarUrl}
            alt={user.login}
            className="h-8 w-8 rounded-full"
          />
          <span className="text-sm font-medium">{user.login}</span>
          <a
            href="/auth/logout"
            className="ml-3 rounded border border-white/30 px-3 py-1 text-xs transition-colors hover:bg-white/10"
          >
            Logout
          </a>
        </div>
      )}
    </header>
  );
}
