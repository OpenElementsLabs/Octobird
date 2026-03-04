CREATE TABLE mentor_rotation (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repo_full_name  VARCHAR(255) NOT NULL UNIQUE,
    next_index      INT NOT NULL DEFAULT 0
);
