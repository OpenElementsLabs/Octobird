CREATE TABLE mentor (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repo_full_name  VARCHAR(255) NOT NULL,
    username        VARCHAR(255) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    UNIQUE (repo_full_name, username)
);
