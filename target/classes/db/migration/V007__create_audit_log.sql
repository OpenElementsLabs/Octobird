CREATE TABLE audit_log (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    repo_full_name  VARCHAR(255) NOT NULL,
    handler_name    VARCHAR(255) NOT NULL,
    action          VARCHAR(255) NOT NULL,
    target          VARCHAR(255),
    created_at      TIMESTAMP NOT NULL,
    details         VARCHAR(4000)
);

CREATE INDEX idx_audit_log_repo ON audit_log(repo_full_name, created_at DESC);
