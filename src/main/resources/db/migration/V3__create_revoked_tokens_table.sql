CREATE TABLE revoked_tokens (
    jti UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_revoked_tokens PRIMARY KEY (jti)
);

CREATE INDEX idx_revoked_tokens_expires_at
    ON revoked_tokens (expires_at);