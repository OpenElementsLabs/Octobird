CREATE TABLE spam_user (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repo_full_name  VARCHAR(255) NOT NULL,
    username        VARCHAR(255) NOT NULL,
    UNIQUE (repo_full_name, username)
);
