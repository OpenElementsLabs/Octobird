package com.openelements.octobird.persistence.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing the full per-repository bot configuration stored in the database.
 * Flat columns map to the various config record fields; collection tables hold list-valued settings.
 */
@Entity
@Table(name = "repo_config", uniqueConstraints = @UniqueConstraint(columnNames = "repo_id"))
public class RepoConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_id", nullable = false)
    private long repoId;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    // --- labels ---
    @Column(name = "label_gfi")
    private String labelGfi;
    @Column(name = "label_beginner")
    private String labelBeginner;
    @Column(name = "label_intermediate")
    private String labelIntermediate;
    @Column(name = "label_advanced")
    private String labelAdvanced;
    @Column(name = "label_gfi_candidate")
    private String labelGfiCandidate;

    // --- assignment limits ---
    @Column(name = "normal_user_max")
    private Integer normalUserMax;
    @Column(name = "spam_user_max")
    private Integer spamUserMax;

    // --- guards ---
    @Column(name = "guard_gfi")
    private Integer guardGfi;
    @Column(name = "guard_beginner")
    private Integer guardBeginner;
    @Column(name = "guard_intermediate")
    private Integer guardIntermediate;
    @Column(name = "guard_advanced")
    private Integer guardAdvanced;

    // --- features ---
    @Column(name = "feat_unassign_command")
    private Boolean featUnassignCommand;
    @Column(name = "feat_assign_command")
    private Boolean featAssignCommand;
    @Column(name = "feat_missing_linked_issue")
    private Boolean featMissingLinkedIssue;
    @Column(name = "feat_verified_commits")
    private Boolean featVerifiedCommits;
    @Column(name = "feat_merge_conflict")
    private Boolean featMergeConflict;
    @Column(name = "feat_next_issue_recommendation")
    private Boolean featNextIssueRecommendation;
    @Column(name = "feat_workflow_failure_notification")
    private Boolean featWorkflowFailureNotification;
    @Column(name = "feat_gfi_candidate_notification")
    private Boolean featGfiCandidateNotification;
    @Column(name = "feat_inactivity_unassign")
    private Boolean featInactivityUnassign;
    @Column(name = "feat_issue_reminder_no_pr")
    private Boolean featIssueReminderNoPr;
    @Column(name = "feat_pr_inactivity_reminder")
    private Boolean featPrInactivityReminder;
    @Column(name = "feat_linked_issue_enforcer")
    private Boolean featLinkedIssueEnforcer;
    @Column(name = "feat_community_call_reminder")
    private Boolean featCommunityCallReminder;
    @Column(name = "feat_office_hours_reminder")
    private Boolean featOfficeHoursReminder;

    // --- markers ---
    @Column(name = "marker_unassign_prefix")
    private String markerUnassignPrefix;
    @Column(name = "marker_gfi_reminder")
    private String markerGfiReminder;
    @Column(name = "marker_beginner_reminder")
    private String markerBeginnerReminder;
    @Column(name = "marker_beginner_gfi_guard")
    private String markerBeginnerGfiGuard;
    @Column(name = "marker_mentor_assignment")
    private String markerMentorAssignment;
    @Column(name = "marker_intermediate_guard")
    private String markerIntermediateGuard;
    @Column(name = "marker_advanced_guard")
    private String markerAdvancedGuard;
    @Column(name = "marker_missing_linked_issue")
    private String markerMissingLinkedIssue;
    @Column(name = "marker_verified_commits")
    private String markerVerifiedCommits;
    @Column(name = "marker_merge_conflict")
    private String markerMergeConflict;
    @Column(name = "marker_next_issue_recommendation")
    private String markerNextIssueRecommendation;
    @Column(name = "marker_workflow_failure_notification")
    private String markerWorkflowFailureNotification;
    @Column(name = "marker_gfi_candidate_notification")
    private String markerGfiCandidateNotification;
    @Column(name = "marker_inactivity_unassign")
    private String markerInactivityUnassign;
    @Column(name = "marker_issue_reminder_no_pr")
    private String markerIssueReminderNoPr;
    @Column(name = "marker_pr_inactivity_reminder")
    private String markerPrInactivityReminder;
    @Column(name = "marker_linked_issue_enforcer")
    private String markerLinkedIssueEnforcer;
    @Column(name = "marker_community_call_reminder")
    private String markerCommunityCallReminder;
    @Column(name = "marker_office_hours_reminder")
    private String markerOfficeHoursReminder;

    // --- commands ---
    @Column(name = "cmd_assign_pattern")
    private String cmdAssignPattern;
    @Column(name = "cmd_unassign_pattern")
    private String cmdUnassignPattern;
    @Column(name = "cmd_working_pattern")
    private String cmdWorkingPattern;

    // --- teams ---
    @Column(name = "team_gfi_candidate")
    private String teamGfiCandidate;

    // --- scheduled ---
    @Column(name = "sched_inactivity_days")
    private Integer schedInactivityDays;
    @Column(name = "sched_issue_reminder_days")
    private Integer schedIssueReminderDays;
    @Column(name = "sched_pr_inactivity_days")
    private Integer schedPrInactivityDays;
    @Column(name = "sched_linked_issue_enforcer_days")
    private Integer schedLinkedIssueEnforcerDays;
    @Column(name = "sched_require_author_assigned")
    private Boolean schedRequireAuthorAssigned;

    // --- community call ---
    @Column(name = "sched_cc_anchor_date")
    private String schedCcAnchorDate;
    @Column(name = "sched_cc_meeting_link")
    private String schedCcMeetingLink;
    @Column(name = "sched_cc_calendar_link")
    private String schedCcCalendarLink;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "repo_config_cc_cancelled_dates",
            joinColumns = @JoinColumn(name = "repo_config_id"))
    @Column(name = "cancelled_date")
    private List<String> schedCcCancelledDates = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "repo_config_cc_excluded_authors",
            joinColumns = @JoinColumn(name = "repo_config_id"))
    @Column(name = "excluded_author")
    private List<String> schedCcExcludedAuthors = new ArrayList<>();

    // --- office hours ---
    @Column(name = "sched_oh_anchor_date")
    private String schedOhAnchorDate;
    @Column(name = "sched_oh_meeting_link")
    private String schedOhMeetingLink;
    @Column(name = "sched_oh_calendar_link")
    private String schedOhCalendarLink;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "repo_config_oh_cancelled_dates",
            joinColumns = @JoinColumn(name = "repo_config_id"))
    @Column(name = "cancelled_date")
    private List<String> schedOhCancelledDates = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "repo_config_oh_excluded_authors",
            joinColumns = @JoinColumn(name = "repo_config_id"))
    @Column(name = "excluded_author")
    private List<String> schedOhExcludedAuthors = new ArrayList<>();

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(final Long id) { this.id = id; }

    public long getRepoId() { return repoId; }
    public void setRepoId(final long repoId) { this.repoId = repoId; }

    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(final String repoFullName) { this.repoFullName = repoFullName; }

    public String getLabelGfi() { return labelGfi; }
    public void setLabelGfi(final String labelGfi) { this.labelGfi = labelGfi; }
    public String getLabelBeginner() { return labelBeginner; }
    public void setLabelBeginner(final String labelBeginner) { this.labelBeginner = labelBeginner; }
    public String getLabelIntermediate() { return labelIntermediate; }
    public void setLabelIntermediate(final String labelIntermediate) { this.labelIntermediate = labelIntermediate; }
    public String getLabelAdvanced() { return labelAdvanced; }
    public void setLabelAdvanced(final String labelAdvanced) { this.labelAdvanced = labelAdvanced; }
    public String getLabelGfiCandidate() { return labelGfiCandidate; }
    public void setLabelGfiCandidate(final String labelGfiCandidate) { this.labelGfiCandidate = labelGfiCandidate; }

    public Integer getNormalUserMax() { return normalUserMax; }
    public void setNormalUserMax(final Integer normalUserMax) { this.normalUserMax = normalUserMax; }
    public Integer getSpamUserMax() { return spamUserMax; }
    public void setSpamUserMax(final Integer spamUserMax) { this.spamUserMax = spamUserMax; }

    public Integer getGuardGfi() { return guardGfi; }
    public void setGuardGfi(final Integer guardGfi) { this.guardGfi = guardGfi; }
    public Integer getGuardBeginner() { return guardBeginner; }
    public void setGuardBeginner(final Integer guardBeginner) { this.guardBeginner = guardBeginner; }
    public Integer getGuardIntermediate() { return guardIntermediate; }
    public void setGuardIntermediate(final Integer guardIntermediate) { this.guardIntermediate = guardIntermediate; }
    public Integer getGuardAdvanced() { return guardAdvanced; }
    public void setGuardAdvanced(final Integer guardAdvanced) { this.guardAdvanced = guardAdvanced; }

    public Boolean getFeatUnassignCommand() { return featUnassignCommand; }
    public void setFeatUnassignCommand(final Boolean v) { this.featUnassignCommand = v; }
    public Boolean getFeatAssignCommand() { return featAssignCommand; }
    public void setFeatAssignCommand(final Boolean v) { this.featAssignCommand = v; }
    public Boolean getFeatMissingLinkedIssue() { return featMissingLinkedIssue; }
    public void setFeatMissingLinkedIssue(final Boolean v) { this.featMissingLinkedIssue = v; }
    public Boolean getFeatVerifiedCommits() { return featVerifiedCommits; }
    public void setFeatVerifiedCommits(final Boolean v) { this.featVerifiedCommits = v; }
    public Boolean getFeatMergeConflict() { return featMergeConflict; }
    public void setFeatMergeConflict(final Boolean v) { this.featMergeConflict = v; }
    public Boolean getFeatNextIssueRecommendation() { return featNextIssueRecommendation; }
    public void setFeatNextIssueRecommendation(final Boolean v) { this.featNextIssueRecommendation = v; }
    public Boolean getFeatWorkflowFailureNotification() { return featWorkflowFailureNotification; }
    public void setFeatWorkflowFailureNotification(final Boolean v) { this.featWorkflowFailureNotification = v; }
    public Boolean getFeatGfiCandidateNotification() { return featGfiCandidateNotification; }
    public void setFeatGfiCandidateNotification(final Boolean v) { this.featGfiCandidateNotification = v; }
    public Boolean getFeatInactivityUnassign() { return featInactivityUnassign; }
    public void setFeatInactivityUnassign(final Boolean v) { this.featInactivityUnassign = v; }
    public Boolean getFeatIssueReminderNoPr() { return featIssueReminderNoPr; }
    public void setFeatIssueReminderNoPr(final Boolean v) { this.featIssueReminderNoPr = v; }
    public Boolean getFeatPrInactivityReminder() { return featPrInactivityReminder; }
    public void setFeatPrInactivityReminder(final Boolean v) { this.featPrInactivityReminder = v; }
    public Boolean getFeatLinkedIssueEnforcer() { return featLinkedIssueEnforcer; }
    public void setFeatLinkedIssueEnforcer(final Boolean v) { this.featLinkedIssueEnforcer = v; }
    public Boolean getFeatCommunityCallReminder() { return featCommunityCallReminder; }
    public void setFeatCommunityCallReminder(final Boolean v) { this.featCommunityCallReminder = v; }
    public Boolean getFeatOfficeHoursReminder() { return featOfficeHoursReminder; }
    public void setFeatOfficeHoursReminder(final Boolean v) { this.featOfficeHoursReminder = v; }

    public String getMarkerUnassignPrefix() { return markerUnassignPrefix; }
    public void setMarkerUnassignPrefix(final String v) { this.markerUnassignPrefix = v; }
    public String getMarkerGfiReminder() { return markerGfiReminder; }
    public void setMarkerGfiReminder(final String v) { this.markerGfiReminder = v; }
    public String getMarkerBeginnerReminder() { return markerBeginnerReminder; }
    public void setMarkerBeginnerReminder(final String v) { this.markerBeginnerReminder = v; }
    public String getMarkerBeginnerGfiGuard() { return markerBeginnerGfiGuard; }
    public void setMarkerBeginnerGfiGuard(final String v) { this.markerBeginnerGfiGuard = v; }
    public String getMarkerMentorAssignment() { return markerMentorAssignment; }
    public void setMarkerMentorAssignment(final String v) { this.markerMentorAssignment = v; }
    public String getMarkerIntermediateGuard() { return markerIntermediateGuard; }
    public void setMarkerIntermediateGuard(final String v) { this.markerIntermediateGuard = v; }
    public String getMarkerAdvancedGuard() { return markerAdvancedGuard; }
    public void setMarkerAdvancedGuard(final String v) { this.markerAdvancedGuard = v; }
    public String getMarkerMissingLinkedIssue() { return markerMissingLinkedIssue; }
    public void setMarkerMissingLinkedIssue(final String v) { this.markerMissingLinkedIssue = v; }
    public String getMarkerVerifiedCommits() { return markerVerifiedCommits; }
    public void setMarkerVerifiedCommits(final String v) { this.markerVerifiedCommits = v; }
    public String getMarkerMergeConflict() { return markerMergeConflict; }
    public void setMarkerMergeConflict(final String v) { this.markerMergeConflict = v; }
    public String getMarkerNextIssueRecommendation() { return markerNextIssueRecommendation; }
    public void setMarkerNextIssueRecommendation(final String v) { this.markerNextIssueRecommendation = v; }
    public String getMarkerWorkflowFailureNotification() { return markerWorkflowFailureNotification; }
    public void setMarkerWorkflowFailureNotification(final String v) { this.markerWorkflowFailureNotification = v; }
    public String getMarkerGfiCandidateNotification() { return markerGfiCandidateNotification; }
    public void setMarkerGfiCandidateNotification(final String v) { this.markerGfiCandidateNotification = v; }
    public String getMarkerInactivityUnassign() { return markerInactivityUnassign; }
    public void setMarkerInactivityUnassign(final String v) { this.markerInactivityUnassign = v; }
    public String getMarkerIssueReminderNoPr() { return markerIssueReminderNoPr; }
    public void setMarkerIssueReminderNoPr(final String v) { this.markerIssueReminderNoPr = v; }
    public String getMarkerPrInactivityReminder() { return markerPrInactivityReminder; }
    public void setMarkerPrInactivityReminder(final String v) { this.markerPrInactivityReminder = v; }
    public String getMarkerLinkedIssueEnforcer() { return markerLinkedIssueEnforcer; }
    public void setMarkerLinkedIssueEnforcer(final String v) { this.markerLinkedIssueEnforcer = v; }
    public String getMarkerCommunityCallReminder() { return markerCommunityCallReminder; }
    public void setMarkerCommunityCallReminder(final String v) { this.markerCommunityCallReminder = v; }
    public String getMarkerOfficeHoursReminder() { return markerOfficeHoursReminder; }
    public void setMarkerOfficeHoursReminder(final String v) { this.markerOfficeHoursReminder = v; }

    public String getCmdAssignPattern() { return cmdAssignPattern; }
    public void setCmdAssignPattern(final String v) { this.cmdAssignPattern = v; }
    public String getCmdUnassignPattern() { return cmdUnassignPattern; }
    public void setCmdUnassignPattern(final String v) { this.cmdUnassignPattern = v; }
    public String getCmdWorkingPattern() { return cmdWorkingPattern; }
    public void setCmdWorkingPattern(final String v) { this.cmdWorkingPattern = v; }

    public String getTeamGfiCandidate() { return teamGfiCandidate; }
    public void setTeamGfiCandidate(final String v) { this.teamGfiCandidate = v; }

    public Integer getSchedInactivityDays() { return schedInactivityDays; }
    public void setSchedInactivityDays(final Integer v) { this.schedInactivityDays = v; }
    public Integer getSchedIssueReminderDays() { return schedIssueReminderDays; }
    public void setSchedIssueReminderDays(final Integer v) { this.schedIssueReminderDays = v; }
    public Integer getSchedPrInactivityDays() { return schedPrInactivityDays; }
    public void setSchedPrInactivityDays(final Integer v) { this.schedPrInactivityDays = v; }
    public Integer getSchedLinkedIssueEnforcerDays() { return schedLinkedIssueEnforcerDays; }
    public void setSchedLinkedIssueEnforcerDays(final Integer v) { this.schedLinkedIssueEnforcerDays = v; }
    public Boolean getSchedRequireAuthorAssigned() { return schedRequireAuthorAssigned; }
    public void setSchedRequireAuthorAssigned(final Boolean v) { this.schedRequireAuthorAssigned = v; }

    public String getSchedCcAnchorDate() { return schedCcAnchorDate; }
    public void setSchedCcAnchorDate(final String v) { this.schedCcAnchorDate = v; }
    public String getSchedCcMeetingLink() { return schedCcMeetingLink; }
    public void setSchedCcMeetingLink(final String v) { this.schedCcMeetingLink = v; }
    public String getSchedCcCalendarLink() { return schedCcCalendarLink; }
    public void setSchedCcCalendarLink(final String v) { this.schedCcCalendarLink = v; }
    public List<String> getSchedCcCancelledDates() { return schedCcCancelledDates; }
    public void setSchedCcCancelledDates(final List<String> v) { this.schedCcCancelledDates = v; }
    public List<String> getSchedCcExcludedAuthors() { return schedCcExcludedAuthors; }
    public void setSchedCcExcludedAuthors(final List<String> v) { this.schedCcExcludedAuthors = v; }

    public String getSchedOhAnchorDate() { return schedOhAnchorDate; }
    public void setSchedOhAnchorDate(final String v) { this.schedOhAnchorDate = v; }
    public String getSchedOhMeetingLink() { return schedOhMeetingLink; }
    public void setSchedOhMeetingLink(final String v) { this.schedOhMeetingLink = v; }
    public String getSchedOhCalendarLink() { return schedOhCalendarLink; }
    public void setSchedOhCalendarLink(final String v) { this.schedOhCalendarLink = v; }
    public List<String> getSchedOhCancelledDates() { return schedOhCancelledDates; }
    public void setSchedOhCancelledDates(final List<String> v) { this.schedOhCancelledDates = v; }
    public List<String> getSchedOhExcludedAuthors() { return schedOhExcludedAuthors; }
    public void setSchedOhExcludedAuthors(final List<String> v) { this.schedOhExcludedAuthors = v; }
}
