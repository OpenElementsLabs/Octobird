CREATE TABLE reminder_state (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repo_full_name  VARCHAR(255) NOT NULL,
    issue_number    INT NOT NULL,
    reminder_type   VARCHAR(100) NOT NULL,
    posted_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_reminder_state_repo_issue ON reminder_state(repo_full_name, issue_number, reminder_type);
