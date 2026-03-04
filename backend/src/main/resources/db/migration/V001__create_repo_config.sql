CREATE TABLE repo_config (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repo_full_name  VARCHAR(255) NOT NULL UNIQUE,

    -- labels
    label_gfi           VARCHAR(255),
    label_beginner      VARCHAR(255),
    label_intermediate  VARCHAR(255),
    label_advanced      VARCHAR(255),
    label_gfi_candidate VARCHAR(255),

    -- assignment limits
    normal_user_max     INT,
    spam_user_max       INT,

    -- guards (required counts)
    guard_gfi           INT,
    guard_beginner      INT,
    guard_intermediate  INT,
    guard_advanced      INT,

    -- features
    feat_unassign_command               BOOLEAN,
    feat_assign_command                 BOOLEAN,
    feat_missing_linked_issue           BOOLEAN,
    feat_verified_commits               BOOLEAN,
    feat_merge_conflict                 BOOLEAN,
    feat_next_issue_recommendation      BOOLEAN,
    feat_workflow_failure_notification   BOOLEAN,
    feat_gfi_candidate_notification     BOOLEAN,
    feat_inactivity_unassign            BOOLEAN,
    feat_issue_reminder_no_pr           BOOLEAN,
    feat_pr_inactivity_reminder         BOOLEAN,
    feat_linked_issue_enforcer          BOOLEAN,
    feat_community_call_reminder        BOOLEAN,
    feat_office_hours_reminder          BOOLEAN,

    -- markers
    marker_unassign_prefix              VARCHAR(512),
    marker_gfi_reminder                 VARCHAR(512),
    marker_beginner_reminder            VARCHAR(512),
    marker_beginner_gfi_guard           VARCHAR(512),
    marker_mentor_assignment            VARCHAR(512),
    marker_intermediate_guard           VARCHAR(512),
    marker_advanced_guard               VARCHAR(512),
    marker_missing_linked_issue         VARCHAR(512),
    marker_verified_commits             VARCHAR(512),
    marker_merge_conflict               VARCHAR(512),
    marker_next_issue_recommendation    VARCHAR(512),
    marker_workflow_failure_notification VARCHAR(512),
    marker_gfi_candidate_notification   VARCHAR(512),
    marker_inactivity_unassign          VARCHAR(512),
    marker_issue_reminder_no_pr         VARCHAR(512),
    marker_pr_inactivity_reminder       VARCHAR(512),
    marker_linked_issue_enforcer        VARCHAR(512),
    marker_community_call_reminder      VARCHAR(512),
    marker_office_hours_reminder        VARCHAR(512),

    -- commands
    cmd_assign_pattern      VARCHAR(255),
    cmd_unassign_pattern    VARCHAR(255),
    cmd_working_pattern     VARCHAR(255),

    -- paths
    path_spam_list      VARCHAR(255),
    path_mentor_roster  VARCHAR(255),

    -- teams
    team_gfi_candidate  VARCHAR(255),

    -- scheduled
    sched_inactivity_days           INT,
    sched_issue_reminder_days       INT,
    sched_pr_inactivity_days        INT,
    sched_linked_issue_enforcer_days INT,
    sched_require_author_assigned   BOOLEAN,

    -- community call
    sched_cc_anchor_date    VARCHAR(10),
    sched_cc_meeting_link   VARCHAR(1024),
    sched_cc_calendar_link  VARCHAR(1024),

    -- office hours
    sched_oh_anchor_date    VARCHAR(10),
    sched_oh_meeting_link   VARCHAR(1024),
    sched_oh_calendar_link  VARCHAR(1024)
);
