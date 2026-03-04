CREATE TABLE repo_config_cc_cancelled_dates (
    repo_config_id  BIGINT NOT NULL REFERENCES repo_config(id) ON DELETE CASCADE,
    cancelled_date  VARCHAR(10) NOT NULL
);

CREATE TABLE repo_config_cc_excluded_authors (
    repo_config_id  BIGINT NOT NULL REFERENCES repo_config(id) ON DELETE CASCADE,
    excluded_author VARCHAR(255) NOT NULL
);

CREATE TABLE repo_config_oh_cancelled_dates (
    repo_config_id  BIGINT NOT NULL REFERENCES repo_config(id) ON DELETE CASCADE,
    cancelled_date  VARCHAR(10) NOT NULL
);

CREATE TABLE repo_config_oh_excluded_authors (
    repo_config_id  BIGINT NOT NULL REFERENCES repo_config(id) ON DELETE CASCADE,
    excluded_author VARCHAR(255) NOT NULL
);
