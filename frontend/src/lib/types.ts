/** Mirrors backend config records */

export interface FeaturesConfig {
  unassignCommand: boolean;
  assignCommand: boolean;
  missingLinkedIssue: boolean;
  verifiedCommits: boolean;
  mergeConflict: boolean;
  nextIssueRecommendation: boolean;
  workflowFailureNotification: boolean;
  gfiCandidateNotification: boolean;
  inactivityUnassign: boolean;
  issueReminderNoPr: boolean;
  prInactivityReminder: boolean;
  linkedIssueEnforcer: boolean;
  communityCallReminder: boolean;
  officeHoursReminder: boolean;
}

export interface LabelsConfig {
  levelLabels: Record<string, string>;
  gfiCandidate: string;
}

export interface AssignmentLimitsConfig {
  normalUserMax: number;
  spamUserMax: number;
}

export interface GuardsConfig {
  requiredCounts: Record<string, number>;
}

export interface CommandsConfig {
  assignPattern: string;
  unassignPattern: string;
  workingPattern: string;
}

export interface MarkersConfig {
  unassignPrefix: string;
  gfiReminder: string;
  beginnerReminder: string;
  beginnerGfiGuard: string;
  mentorAssignment: string;
  intermediateGuard: string;
  advancedGuard: string;
  missingLinkedIssue: string;
  verifiedCommits: string;
  mergeConflict: string;
  nextIssueRecommendation: string;
  workflowFailureNotification: string;
  gfiCandidateNotification: string;
  inactivityUnassign: string;
  issueReminderNoPr: string;
  prInactivityReminder: string;
  linkedIssueEnforcer: string;
  communityCallReminder: string;
  officeHoursReminder: string;
}

export interface TeamsConfig {
  gfiCandidateTeam: string;
}

export interface CommunityCallConfig {
  anchorDate: string;
  meetingLink: string;
  calendarLink: string;
  cancelledDates: string[];
  excludedAuthors: string[];
}

export interface OfficeHoursConfig {
  anchorDate: string;
  meetingLink: string;
  calendarLink: string;
  cancelledDates: string[];
  excludedAuthors: string[];
}

export interface ScheduledConfig {
  inactivityDays: number;
  issueReminderDays: number;
  prInactivityDays: number;
  linkedIssueEnforcerDays: number;
  requireAuthorAssigned: boolean;
  communityCall: CommunityCallConfig;
  officeHours: OfficeHoursConfig;
}

export interface RepoConfig {
  repoId: number;
  repoFullName: string;
  features: FeaturesConfig;
  labels: LabelsConfig;
  assignmentLimits: AssignmentLimitsConfig;
  guards: GuardsConfig;
  commands: CommandsConfig;
  markers: MarkersConfig;
  teams: TeamsConfig;
  scheduled: ScheduledConfig;
}

export interface GitHubAccount {
  githubId: number;
  username: string;
}

export interface AuditLogEntry {
  id: string;
  handlerName: string;
  action: string;
  target: string;
  timestamp: string;
  details: string;
}

export interface AuditLogPage {
  entries: AuditLogEntry[];
  total: number;
  offset: number;
  limit: number;
}

export interface AuditLogFilters {
  handler?: string;
  action?: string;
  dateFrom?: string;
  dateTo?: string;
  offset?: number;
  limit?: number;
}
