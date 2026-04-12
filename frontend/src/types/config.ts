export enum IssueLevel {
  GOOD_FIRST_ISSUE = "GOOD_FIRST_ISSUE",
  BEGINNER = "BEGINNER",
  INTERMEDIATE = "INTERMEDIATE",
  ADVANCED = "ADVANCED",
}

export interface GitHubAccountDto {
  readonly githubId: number;
  readonly username: string;
}

export interface FeaturesConfig {
  readonly unassignCommand: boolean;
  readonly assignCommand: boolean;
  readonly missingLinkedIssue: boolean;
  readonly verifiedCommits: boolean;
  readonly mergeConflict: boolean;
  readonly nextIssueRecommendation: boolean;
  readonly workflowFailureNotification: boolean;
  readonly gfiCandidateNotification: boolean;
  readonly inactivityUnassign: boolean;
  readonly issueReminderNoPr: boolean;
  readonly prInactivityReminder: boolean;
  readonly linkedIssueEnforcer: boolean;
  readonly communityCallReminder: boolean;
  readonly officeHoursReminder: boolean;
}

export interface LabelsConfig {
  readonly levelLabels: Record<IssueLevel, string>;
  readonly gfiCandidate: string;
}

export interface AssignmentLimitsConfig {
  readonly normalUserMax: number;
  readonly spamUserMax: number;
}

export interface GuardsConfig {
  readonly requiredCounts: Record<IssueLevel, number>;
}

export interface CommandsConfig {
  readonly assignPattern: string;
  readonly unassignPattern: string;
  readonly workingPattern: string;
}

export interface MarkersConfig {
  readonly unassignPrefix: string;
  readonly gfiReminder: string;
  readonly beginnerReminder: string;
  readonly beginnerGfiGuard: string;
  readonly mentorAssignment: string;
  readonly intermediateGuard: string;
  readonly advancedGuard: string;
  readonly missingLinkedIssue: string;
  readonly verifiedCommits: string;
  readonly mergeConflict: string;
  readonly nextIssueRecommendation: string;
  readonly workflowFailureNotification: string;
  readonly gfiCandidateNotification: string;
  readonly inactivityUnassign: string;
  readonly issueReminderNoPr: string;
  readonly prInactivityReminder: string;
  readonly linkedIssueEnforcer: string;
  readonly communityCallReminder: string;
  readonly officeHoursReminder: string;
}

export interface TeamsConfig {
  readonly gfiCandidateTeam: string;
}

export interface CommunityCallConfig {
  readonly anchorDate: string;
  readonly meetingLink: string;
  readonly calendarLink: string;
  readonly cancelledDates: readonly string[];
  readonly excludedAuthors: readonly string[];
}

export interface OfficeHoursConfig {
  readonly anchorDate: string;
  readonly meetingLink: string;
  readonly calendarLink: string;
  readonly cancelledDates: readonly string[];
  readonly excludedAuthors: readonly string[];
}

export interface ScheduledConfig {
  readonly inactivityDays: number;
  readonly issueReminderDays: number;
  readonly prInactivityDays: number;
  readonly linkedIssueEnforcerDays: number;
  readonly requireAuthorAssigned: boolean;
  readonly communityCall: CommunityCallConfig;
  readonly officeHours: OfficeHoursConfig;
}

export interface RepoConfig {
  readonly repoId: number;
  readonly repoFullName: string;
  readonly labels: LabelsConfig;
  readonly assignmentLimits: AssignmentLimitsConfig;
  readonly guards: GuardsConfig;
  readonly features: FeaturesConfig;
  readonly markers: MarkersConfig;
  readonly commands: CommandsConfig;
  readonly teams: TeamsConfig;
  readonly scheduled: ScheduledConfig;
}
