CREATE TABLE accounts (
    id uuid PRIMARY KEY,
    email varchar(254) NOT NULL UNIQUE,
    password_hash text DEFAULT NULL,
    session_version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_accounts_email CHECK (email=lower(btrim(email)) AND char_length(email)>0),
    CONSTRAINT ck_accounts_hash CHECK (password_hash IS NULL OR char_length(password_hash)>0),
    CONSTRAINT ck_accounts_version CHECK (session_version>=0),
    CONSTRAINT ck_accounts_time CHECK (isfinite(created_at) AND isfinite(updated_at))
);

CREATE TABLE session_security (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    session_version bigint NOT NULL,
    setup_generation bigint NOT NULL DEFAULT 0,
    guardian_unlocked_until timestamptz DEFAULT NULL,
    guardian_pin_version bigint DEFAULT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz,
    revoked_at timestamptz DEFAULT NULL,
    CONSTRAINT uq_security_account UNIQUE (id,account_id),
    CONSTRAINT ck_security_versions CHECK (session_version>=0 AND setup_generation>=0),
    CONSTRAINT ck_security_guardian CHECK (
        (guardian_unlocked_until IS NULL AND guardian_pin_version IS NULL) OR
        (guardian_unlocked_until IS NOT NULL AND guardian_pin_version IS NOT NULL
         AND guardian_pin_version>=1)),
    CONSTRAINT ck_security_expiry CHECK (expires_at IS NULL OR expires_at>created_at),
    CONSTRAINT ck_security_revoke CHECK (revoked_at IS NULL OR revoked_at>=created_at),
    CONSTRAINT ck_security_guardian_bound CHECK (
        guardian_unlocked_until IS NULL OR expires_at IS NULL OR guardian_unlocked_until<=expires_at),
    CONSTRAINT ck_security_time CHECK (
        isfinite(created_at) AND (expires_at IS NULL OR isfinite(expires_at)) AND
        (revoked_at IS NULL OR isfinite(revoked_at)) AND
        (guardian_unlocked_until IS NULL OR isfinite(guardian_unlocked_until)))
);
CREATE INDEX ix_security_account ON session_security(account_id);
CREATE INDEX ix_security_expiry ON session_security(expires_at);
