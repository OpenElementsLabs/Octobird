"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { fetchSpamUsers, saveSpamUsers } from "@/lib/api";
import type { GitHubAccount } from "@/lib/types";
import { ToastContainer, useToast } from "@/components/toast";

export default function SpamUsersPage() {
  const params = useParams<{ owner: string; repo: string }>();
  const [users, setUsers] = useState<GitHubAccount[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [newUsername, setNewUsername] = useState("");
  const [newGithubId, setNewGithubId] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);
  const { toasts, showToast, removeToast } = useToast();

  useEffect(() => {
    fetchSpamUsers(params.owner, params.repo)
      .then(setUsers)
      .catch((err) => setError(err.message));
  }, [params.owner, params.repo]);

  const addUser = () => {
    setValidationError(null);
    if (!newUsername.trim()) {
      setValidationError("Username is required");
      return;
    }
    const githubId = parseInt(newGithubId);
    if (!newGithubId.trim() || isNaN(githubId) || githubId <= 0) {
      setValidationError("Valid GitHub ID is required");
      return;
    }
    setUsers((prev) => [
      ...(prev || []),
      { username: newUsername.trim(), githubId },
    ]);
    setNewUsername("");
    setNewGithubId("");
  };

  const removeUser = (index: number) => {
    setUsers((prev) => prev?.filter((_, i) => i !== index) ?? []);
  };

  const handleSave = async () => {
    if (!users) return;
    setSaving(true);
    try {
      await saveSpamUsers(params.owner, params.repo, users);
      showToast("Spam user list saved successfully", "success");
    } catch (err) {
      showToast(
        `Failed to save: ${err instanceof Error ? err.message : "Unknown error"}`,
        "error"
      );
    } finally {
      setSaving(false);
    }
  };

  if (error) {
    return (
      <div className="rounded-lg border border-oe-red/20 bg-oe-red/5 p-6 text-center">
        <p className="text-oe-red font-medium">{error}</p>
      </div>
    );
  }

  if (users === null) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-12 animate-pulse rounded-lg bg-oe-light-gray" />
        ))}
      </div>
    );
  }

  return (
    <div>
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      <h3 className="font-heading text-lg font-bold text-oe-dark mb-4">
        Spam Users
      </h3>

      {users.length === 0 ? (
        <p className="text-oe-mid-gray mb-4">No spam users configured</p>
      ) : (
        <table className="mb-4 w-full text-sm">
          <thead>
            <tr className="border-b border-oe-light-gray text-left text-xs font-medium text-oe-mid-gray">
              <th className="pb-2">Username</th>
              <th className="pb-2">GitHub ID</th>
              <th className="pb-2 w-20"></th>
            </tr>
          </thead>
          <tbody>
            {users.map((user, i) => (
              <tr key={i} className="border-b border-oe-light-gray/50">
                <td className="py-2 text-oe-dark">{user.username}</td>
                <td className="py-2 text-oe-mid-gray">{user.githubId}</td>
                <td className="py-2 text-right">
                  <button
                    onClick={() => removeUser(i)}
                    className="text-xs text-oe-red font-medium hover:underline"
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <div className="flex items-end gap-3 mb-6">
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            Username
          </label>
          <input
            type="text"
            value={newUsername}
            onChange={(e) => setNewUsername(e.target.value)}
            className="rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            GitHub ID
          </label>
          <input
            type="number"
            min={1}
            value={newGithubId}
            onChange={(e) => setNewGithubId(e.target.value)}
            className="rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
        <button
          onClick={addUser}
          className="rounded bg-oe-green px-4 py-1.5 text-sm font-medium text-white hover:bg-oe-green-dark"
        >
          Add
        </button>
      </div>
      {validationError && (
        <p className="text-oe-red text-xs mb-4">{validationError}</p>
      )}

      <button
        onClick={handleSave}
        disabled={saving}
        className="rounded-lg bg-oe-green px-6 py-2.5 font-medium text-white transition-colors hover:bg-oe-green-dark disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {saving ? "Saving..." : "Save"}
      </button>
    </div>
  );
}
