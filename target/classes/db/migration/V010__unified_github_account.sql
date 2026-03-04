-- Unified github_account table with SINGLE_TABLE inheritance for spam users and mentors.
-- Replaces: spam_user, mentor, mentor_rotation

CREATE TABLE github_account (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dtype           VARCHAR(31) NOT NULL,
    repo_id         BIGINT NOT NULL,
    github_id       BIGINT NOT NULL,
    username        VARCHAR(255) NOT NULL,
    used_as_mentor  INT,
    CONSTRAINT fk_github_account_repo FOREIGN KEY (repo_id) REFERENCES repo_config(repo_id),
    CONSTRAINT uq_github_account UNIQUE (repo_id, github_id, dtype)
);

DROP TABLE IF EXISTS spam_user;
DROP TABLE IF EXISTS mentor;
DROP TABLE IF EXISTS mentor_rotation;
