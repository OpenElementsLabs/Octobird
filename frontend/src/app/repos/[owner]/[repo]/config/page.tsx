"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { fetchRepoConfig, saveRepoConfig } from "@/lib/api";
import type { RepoConfig } from "@/lib/types";
import CollapsibleSection from "@/components/collapsible-section";
import ListEditor from "@/components/list-editor";
import { ToastContainer, useToast } from "@/components/toast";

export default function ConfigPage() {
  const params = useParams<{ owner: string; repo: string }>();
  const [config, setConfig] = useState<RepoConfig | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const { toasts, showToast, removeToast } = useToast();

  useEffect(() => {
    fetchRepoConfig(params.owner, params.repo)
      .then(setConfig)
      .catch((err) => setError(err.message));
  }, [params.owner, params.repo]);

  const handleSave = async () => {
    if (!config) return;
    setSaving(true);
    try {
      await saveRepoConfig(params.owner, params.repo, config);
      showToast("Configuration saved successfully", "success");
    } catch (err) {
      showToast(
        `Failed to save configuration: ${err instanceof Error ? err.message : "Unknown error"}`,
        "error"
      );
    } finally {
      setSaving(false);
    }
  };

  const updateFeature = (key: string, value: boolean) => {
    if (!config) return;
    setConfig({
      ...config,
      features: { ...config.features, [key]: value },
    });
  };

  const updateLabels = (key: string, value: string) => {
    if (!config) return;
    if (key === "gfiCandidate") {
      setConfig({ ...config, labels: { ...config.labels, gfiCandidate: value } });
    } else {
      setConfig({
        ...config,
        labels: {
          ...config.labels,
          levelLabels: { ...config.labels.levelLabels, [key]: value },
        },
      });
    }
  };

  if (error) {
    return (
      <div className="rounded-lg border border-oe-red/20 bg-oe-red/5 p-6 text-center">
        <p className="text-oe-red font-medium">{error}</p>
      </div>
    );
  }

  if (!config) {
    return (
      <div className="space-y-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-16 animate-pulse rounded-lg bg-oe-light-gray" />
        ))}
      </div>
    );
  }

  const featureFlags = Object.entries(config.features) as [string, boolean][];

  return (
    <div className="space-y-4">
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      <CollapsibleSection title="Features">
        <div className="grid gap-3 sm:grid-cols-2">
          {featureFlags.map(([key, enabled]) => (
            <label key={key} className="flex items-center gap-3 text-sm">
              <input
                type="checkbox"
                checked={enabled}
                onChange={(e) => updateFeature(key, e.target.checked)}
                className="h-4 w-4 rounded border-oe-mid-gray text-oe-green focus:ring-oe-green"
              />
              <span className="text-oe-dark">{formatKey(key)}</span>
            </label>
          ))}
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Labels">
        <div className="grid gap-3 sm:grid-cols-2">
          {["GOOD_FIRST_ISSUE", "BEGINNER", "INTERMEDIATE", "ADVANCED"].map(
            (level) => (
              <div key={level}>
                <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
                  {level}
                </label>
                <input
                  type="text"
                  value={config.labels.levelLabels[level] ?? ""}
                  onChange={(e) => updateLabels(level, e.target.value)}
                  className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
                />
              </div>
            )
          )}
          <div>
            <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
              GFI Candidate
            </label>
            <input
              type="text"
              value={config.labels.gfiCandidate}
              onChange={(e) => updateLabels("gfiCandidate", e.target.value)}
              className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
            />
          </div>
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Assignment Limits">
        <div className="grid gap-3 sm:grid-cols-2">
          <NumberField
            label="Normal User Max"
            value={config.assignmentLimits.normalUserMax}
            onChange={(v) =>
              setConfig({
                ...config,
                assignmentLimits: { ...config.assignmentLimits, normalUserMax: v },
              })
            }
          />
          <NumberField
            label="Spam User Max"
            value={config.assignmentLimits.spamUserMax}
            onChange={(v) =>
              setConfig({
                ...config,
                assignmentLimits: { ...config.assignmentLimits, spamUserMax: v },
              })
            }
          />
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Guards">
        <div className="grid gap-3 sm:grid-cols-2">
          {["GOOD_FIRST_ISSUE", "BEGINNER", "INTERMEDIATE", "ADVANCED"].map(
            (level) => (
              <NumberField
                key={level}
                label={level}
                value={config.guards.requiredCounts[level] ?? 0}
                onChange={(v) =>
                  setConfig({
                    ...config,
                    guards: {
                      ...config.guards,
                      requiredCounts: {
                        ...config.guards.requiredCounts,
                        [level]: v,
                      },
                    },
                  })
                }
              />
            )
          )}
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Commands">
        <div className="grid gap-3">
          {(["assignPattern", "unassignPattern", "workingPattern"] as const).map(
            (key) => (
              <div key={key}>
                <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
                  {formatKey(key)}
                </label>
                <input
                  type="text"
                  value={config.commands[key]}
                  onChange={(e) =>
                    setConfig({
                      ...config,
                      commands: { ...config.commands, [key]: e.target.value },
                    })
                  }
                  className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm font-mono focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
                />
              </div>
            )
          )}
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Teams">
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            GFI Candidate Team
          </label>
          <input
            type="text"
            value={config.teams.gfiCandidateTeam}
            onChange={(e) =>
              setConfig({
                ...config,
                teams: { ...config.teams, gfiCandidateTeam: e.target.value },
              })
            }
            className="w-full max-w-md rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Scheduled Tasks">
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <NumberField
              label="Inactivity Days"
              value={config.scheduled.inactivityDays}
              onChange={(v) =>
                setConfig({
                  ...config,
                  scheduled: { ...config.scheduled, inactivityDays: v },
                })
              }
            />
            <NumberField
              label="Issue Reminder Days"
              value={config.scheduled.issueReminderDays}
              onChange={(v) =>
                setConfig({
                  ...config,
                  scheduled: { ...config.scheduled, issueReminderDays: v },
                })
              }
            />
            <NumberField
              label="PR Inactivity Days"
              value={config.scheduled.prInactivityDays}
              onChange={(v) =>
                setConfig({
                  ...config,
                  scheduled: { ...config.scheduled, prInactivityDays: v },
                })
              }
            />
            <NumberField
              label="Linked Issue Enforcer Days"
              value={config.scheduled.linkedIssueEnforcerDays}
              onChange={(v) =>
                setConfig({
                  ...config,
                  scheduled: {
                    ...config.scheduled,
                    linkedIssueEnforcerDays: v,
                  },
                })
              }
            />
          </div>
          <label className="flex items-center gap-3 text-sm">
            <input
              type="checkbox"
              checked={config.scheduled.requireAuthorAssigned}
              onChange={(e) =>
                setConfig({
                  ...config,
                  scheduled: {
                    ...config.scheduled,
                    requireAuthorAssigned: e.target.checked,
                  },
                })
              }
              className="h-4 w-4 rounded border-oe-mid-gray text-oe-green focus:ring-oe-green"
            />
            Require Author Assigned
          </label>

          <h4 className="font-heading text-sm font-semibold text-oe-dark mt-4">
            Community Call
          </h4>
          <ScheduleSubSection
            schedule={config.scheduled.communityCall}
            onChange={(cc) =>
              setConfig({
                ...config,
                scheduled: { ...config.scheduled, communityCall: cc },
              })
            }
          />

          <h4 className="font-heading text-sm font-semibold text-oe-dark mt-4">
            Office Hours
          </h4>
          <ScheduleSubSection
            schedule={config.scheduled.officeHours}
            onChange={(oh) =>
              setConfig({
                ...config,
                scheduled: { ...config.scheduled, officeHours: oh },
              })
            }
          />
        </div>
      </CollapsibleSection>

      <div className="pt-4">
        <button
          onClick={handleSave}
          disabled={saving}
          className="rounded-lg bg-oe-green px-6 py-2.5 font-medium text-white transition-colors hover:bg-oe-green-dark disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {saving ? "Saving..." : "Save"}
        </button>
      </div>
    </div>
  );
}

function NumberField({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number;
  onChange: (v: number) => void;
}) {
  return (
    <div>
      <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
        {label}
      </label>
      <input
        type="number"
        min={0}
        value={value}
        onChange={(e) => onChange(Math.max(0, parseInt(e.target.value) || 0))}
        className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
      />
    </div>
  );
}

function ScheduleSubSection({
  schedule,
  onChange,
}: {
  schedule: { anchorDate: string; meetingLink: string; calendarLink: string; cancelledDates: string[]; excludedAuthors: string[] };
  onChange: (s: typeof schedule) => void;
}) {
  return (
    <div className="space-y-3 pl-4 border-l-2 border-oe-light-gray">
      <div className="grid gap-3 sm:grid-cols-3">
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            Anchor Date
          </label>
          <input
            type="text"
            value={schedule.anchorDate}
            onChange={(e) => onChange({ ...schedule, anchorDate: e.target.value })}
            className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            Meeting Link
          </label>
          <input
            type="text"
            value={schedule.meetingLink}
            onChange={(e) =>
              onChange({ ...schedule, meetingLink: e.target.value })
            }
            className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
        <div>
          <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
            Calendar Link
          </label>
          <input
            type="text"
            value={schedule.calendarLink}
            onChange={(e) =>
              onChange({ ...schedule, calendarLink: e.target.value })
            }
            className="w-full rounded border border-oe-light-gray px-3 py-1.5 text-sm focus:border-oe-green focus:outline-none focus:ring-1 focus:ring-oe-green"
          />
        </div>
      </div>
      <div>
        <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
          Cancelled Dates
        </label>
        <ListEditor
          items={schedule.cancelledDates}
          onChange={(items) => onChange({ ...schedule, cancelledDates: items })}
          placeholder="Add date (YYYY-MM-DD)..."
        />
      </div>
      <div>
        <label className="mb-1 block text-xs font-medium text-oe-mid-gray">
          Excluded Authors
        </label>
        <ListEditor
          items={schedule.excludedAuthors}
          onChange={(items) =>
            onChange({ ...schedule, excludedAuthors: items })
          }
          placeholder="Add username..."
        />
      </div>
    </div>
  );
}

function formatKey(key: string): string {
  return key
    .replace(/([A-Z])/g, " $1")
    .replace(/^./, (s) => s.toUpperCase())
    .trim();
}
