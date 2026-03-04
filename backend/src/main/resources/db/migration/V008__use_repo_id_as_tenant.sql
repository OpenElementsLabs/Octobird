-- V008: Add repo_id (immutable GitHub numeric ID) as tenant discriminator.
-- repo_full_name is kept as denormalized display column but loses its UNIQUE role.

-- repo_config
ALTER TABLE repo_config ADD COLUMN repo_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE repo_config DROP CONSTRAINT IF EXISTS repo_config_repo_full_name_key;
ALTER TABLE repo_config ADD CONSTRAINT uq_repo_config_repo_id UNIQUE (repo_id);

-- spam_user
ALTER TABLE spam_user ADD COLUMN repo_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE spam_user DROP CONSTRAINT IF EXISTS spam_user_repo_full_name_username_key;
ALTER TABLE spam_user ADD CONSTRAINT uq_spam_user_repo_id_username UNIQUE (repo_id, username);

-- mentor
ALTER TABLE mentor ADD COLUMN repo_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE mentor DROP CONSTRAINT IF EXISTS mentor_repo_full_name_username_key;
ALTER TABLE mentor ADD CONSTRAINT uq_mentor_repo_id_username UNIQUE (repo_id, username);

-- reminder_state
ALTER TABLE reminder_state ADD COLUMN repo_id BIGINT NOT NULL DEFAULT 0;
DROP INDEX IF EXISTS idx_reminder_state_repo_issue_type;
CREATE INDEX idx_reminder_state_repo_id_issue_type ON reminder_state (repo_id, issue_number, reminder_type);

-- mentor_rotation
ALTER TABLE mentor_rotation ADD COLUMN repo_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE mentor_rotation DROP CONSTRAINT IF EXISTS mentor_rotation_repo_full_name_key;
ALTER TABLE mentor_rotation ADD CONSTRAINT uq_mentor_rotation_repo_id UNIQUE (repo_id);

-- audit_log
ALTER TABLE audit_log ADD COLUMN repo_id BIGINT NOT NULL DEFAULT 0;
DROP INDEX IF EXISTS idx_audit_log_repo_created;
CREATE INDEX idx_audit_log_repo_id_created ON audit_log (repo_id, created_at DESC);
